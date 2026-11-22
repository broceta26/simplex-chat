package chat.simplex.common.model

import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.Log
import chat.simplex.common.platform.TAG
import chat.simplex.common.platform.chatModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.URL
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*

// Šapat directory: each profile has a code (permanent, can be replaced) and a name (a mask over the code).
// Both resolve to the profile's contact address; a contact request still needs the owner's approval.
// The API certificate is pinned, so only our directory gets the token.
object SapatDirectory {
  private const val API = "https://148.251.84.251:5224/api/v1/"
  private const val CERT_SHA256 = "A1163DA99BC420B89569B67E9030C825AEBF45EB6B1321E7549C9465943DF386"
  private const val HEARTBEAT_EVERY_MS = 24 * 3600 * 1000L
  const val TLD = "simplex"

  @Serializable
  data class Entry(
    val token: String,
    val code: String,
    val name: String? = null,
    val reserved: Boolean = false,
    val reservableAt: Long = 0,
    val canReserve: Boolean = false,
    val address: String? = null,
    val checkedAt: Long = 0,
  )

  class DirectoryError(val error: String) : Exception(error)

  private val json = Json { ignoreUnknownKeys = true }

  private fun entries(): MutableMap<String, Entry> =
    runCatching { json.decodeFromString<Map<String, Entry>>(appPrefs.sapatDirectory.get() ?: "{}").toMutableMap() }
      .getOrElse { mutableMapOf() }

  fun entry(userId: Long): Entry? = entries()[userId.toString()]

  private fun save(userId: Long, e: Entry) {
    val all = entries()
    all[userId.toString()] = e
    appPrefs.sapatDirectory.set(json.encodeToString(all))
  }

  fun forget(userId: Long) {
    val all = entries()
    all.remove(userId.toString())
    appPrefs.sapatDirectory.set(json.encodeToString(all))
  }

  /** Registers the current profile once, keeps its address up to date and sends the daily heartbeat. */
  suspend fun ensureRegistered(rh: Long?): Entry? {
    val userId = chatModel.currentUser.value?.userId ?: return null
    val address = currentAddress(rh) ?: return null
    val existing = entry(userId)
    return try {
      when {
        existing == null -> {
          val r = post("register", buildJsonObject { put("address", address) }, null)
          fromStatus(r, r["token"]!!.jsonPrimitive.content, address).also { save(userId, it) }
        }
        existing.address != address -> {
          val r = post("address", buildJsonObject { put("address", address) }, existing.token)
          fromStatus(r, existing.token, address).also { save(userId, it) }
        }
        System.currentTimeMillis() - existing.checkedAt > HEARTBEAT_EVERY_MS -> {
          val r = post("heartbeat", buildJsonObject { }, existing.token)
          fromStatus(r, existing.token, address).also { save(userId, it) }
        }
        else -> existing
      }
    } catch (e: Exception) {
      Log.e(TAG, "SapatDirectory.ensureRegistered: ${e.message}")
      existing
    }
  }

  suspend fun setName(userId: Long, name: String): Entry = update(userId, "name", buildJsonObject { put("name", name.trim().lowercase()) })
  suspend fun reserveName(userId: Long): Entry = update(userId, "reserve", buildJsonObject { })
  suspend fun newCode(userId: Long): Entry = update(userId, "rotate", buildJsonObject { })

  private suspend fun update(userId: Long, op: String, body: JsonObject): Entry {
    val e = entry(userId) ?: throw DirectoryError("noProfile")
    val r = post(op, body, e.token)
    return fromStatus(r, e.token, e.address).also { save(userId, it) }
  }

  /** What the core resolves: a name or a code, as a contact name in the directory's top-level part. */
  fun connectTarget(input: String): String? {
    val s = input.trim().removePrefix("@").lowercase()
    val ok = Regex("^[a-z0-9][a-z0-9-]{2,31}$").matches(s)
    return if (ok) "@$s.$TLD" else null
  }

  private fun fromStatus(r: JsonObject, token: String, address: String?): Entry =
    Entry(
      token = token,
      code = r["code"]!!.jsonPrimitive.content,
      name = r["name"]?.jsonPrimitive?.contentOrNull,
      reserved = r["reserved"]?.jsonPrimitive?.booleanOrNull ?: false,
      reservableAt = r["reservableAt"]?.jsonPrimitive?.longOrNull ?: 0,
      canReserve = r["canReserve"]?.jsonPrimitive?.booleanOrNull ?: false,
      address = address,
      checkedAt = System.currentTimeMillis(),
    )

  private suspend fun currentAddress(rh: Long?): String? {
    val link = chatModel.userAddress.value?.connLinkContact
      ?: chatModel.controller.apiGetUserAddressRec(rh)?.connLinkContact
      ?: createAddressSilently(rh)
    return link?.connShortLink ?: link?.connFullLink
  }

  // No alerts: this runs in the background on start; a failure is retried on the next start.
  private suspend fun createAddressSilently(rh: Long?): CreatedConnLink? {
    val userId = chatModel.currentUser.value?.userId ?: return null
    val r = chatModel.controller.sendCmd(rh, CC.ApiCreateMyAddress(userId))
    return if (r is API.Result && r.res is CR.UserContactLinkCreated) r.res.connLinkContact else null
  }

  private suspend fun post(op: String, body: JsonObject, token: String?): JsonObject = withContext(Dispatchers.IO) {
    val conn = URL(API + op).openConnection() as HttpsURLConnection
    conn.sslSocketFactory = pinnedContext.socketFactory
    conn.hostnameVerifier = HostnameVerifier { _, _ -> true } // the pinned certificate is the check
    conn.connectTimeout = 10_000
    conn.readTimeout = 10_000
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json")
    if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
    conn.outputStream.use { it.write(body.toString().toByteArray()) }
    val code = conn.responseCode
    val text = (if (code < 400) conn.inputStream else conn.errorStream)?.use { it.readBytes().decodeToString() } ?: "{}"
    val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrElse { JsonObject(emptyMap()) }
    if (code >= 400) throw DirectoryError(obj["error"]?.jsonPrimitive?.contentOrNull ?: "http$code")
    obj
  }

  private val pinnedContext: SSLContext by lazy {
    val tm = object : X509TrustManager {
      override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = throw java.security.cert.CertificateException()
      override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw java.security.cert.CertificateException("no certificate")
        val fp = MessageDigest.getInstance("SHA-256").digest(leaf.encoded).joinToString("") { "%02X".format(it) }
        if (fp != CERT_SHA256) throw java.security.cert.CertificateException("directory certificate mismatch")
      }
      override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    SSLContext.getInstance("TLS").apply { init(null, arrayOf(tm), null) }
  }
}
