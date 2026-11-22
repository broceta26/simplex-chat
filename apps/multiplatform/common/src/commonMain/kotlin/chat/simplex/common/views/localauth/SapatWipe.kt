package chat.simplex.common.views.localauth

import chat.simplex.common.model.ChatController.appPrefs
import java.security.MessageDigest

// Šapat: the app is wiped after 3 wrong PINs (with a warning before the third),
// or at once on any of the user's wipe PINs. Wipe PINs are kept only as SHA-256 hashes.
object SapatWipe {
  const val MAX_WRONG = 3

  private fun hash(pin: String): String =
    MessageDigest.getInstance("SHA-256").digest(("sapat-wipe|$pin").toByteArray()).joinToString("") { "%02x".format(it) }

  private fun hashes(): Set<String> =
    (appPrefs.sapatWipePins.get() ?: "").split(",").filter { it.isNotEmpty() }.toSet()

  fun count(): Int = hashes().size

  fun isWipePin(pin: String): Boolean = pin.isNotEmpty() && hash(pin) in hashes()

  fun addWipePin(pin: String) {
    appPrefs.sapatWipePins.set((hashes() + hash(pin)).joinToString(","))
  }

  fun clearWipePins() = appPrefs.sapatWipePins.set(null)

  /** Records a wrong PIN; returns how many wrong PINs in a row there are now. */
  fun wrongPin(): Int {
    val n = appPrefs.sapatWrongPins.get() + 1
    appPrefs.sapatWrongPins.set(n)
    return n
  }

  fun resetWrong() = appPrefs.sapatWrongPins.set(0)
}
