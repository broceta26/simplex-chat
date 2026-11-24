package chat.simplex.common.views.usersettings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.androidAppContext
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.views.helpers.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PhoneContact(val name: String, val number: String)

private suspend fun readContacts(): List<PhoneContact> = withContext(Dispatchers.IO) {
  val res = mutableListOf<PhoneContact>()
  androidAppContext.contentResolver.query(
    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
    arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
    null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
  )?.use { c ->
    while (c.moveToNext()) {
      val number = c.getString(1)?.trim().orEmpty()
      if (number.isNotEmpty()) res.add(PhoneContact(c.getString(0)?.trim().orEmpty().ifEmpty { number }, number))
    }
  }
  res.distinctBy { it.number.filter { ch -> ch.isDigit() } }
}

private fun openSms(number: String, text: String) {
  val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))).apply {
    putExtra("sms_body", text)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
  try { androidAppContext.startActivity(intent) } catch (_: Exception) {}
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun SapatInviteContactsView(inviteText: String, labels: SapatInviteLabels) {
  val permission = rememberPermissionState(Manifest.permission.READ_CONTACTS)
  var contacts by remember { mutableStateOf<List<PhoneContact>?>(null) }
  val selected = remember { mutableStateListOf<String>() }
  val sent = remember { mutableStateListOf<String>() }
  var sending by remember { mutableStateOf(false) }
  LaunchedEffect(permission.status.isGranted) {
    if (permission.status.isGranted) contacts = readContacts()
  }
  Column(Modifier.fillMaxSize()) {
    AppBarTitle(labels.title)
    when {
      !permission.status.isGranted -> Column(Modifier.padding(DEFAULT_PADDING), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(labels.permission, color = MaterialTheme.colors.secondary)
        Button({ permission.launchPermissionRequest() }, Modifier.fillMaxWidth()) { Text(labels.grant) }
      }
      contacts == null -> {}
      contacts!!.isEmpty() -> Text(labels.none, Modifier.padding(DEFAULT_PADDING), color = MaterialTheme.colors.secondary)
      else -> {
        val list = if (sending) contacts!!.filter { it.number in selected } else contacts!!
        LazyColumn(Modifier.weight(1f)) {
          items(list, key = { it.number }) { c ->
            val on = c.number in selected
            Row(
              Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable {
                if (sending) { openSms(c.number, inviteText); if (c.number !in sent) sent.add(c.number) }
                else if (on) selected.remove(c.number) else selected.add(c.number)
              }.padding(horizontal = DEFAULT_PADDING),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (!sending) Checkbox(on, null)
              Column(Modifier.weight(1f).padding(start = if (sending) 0.dp else 12.dp)) {
                Text(c.name, fontSize = 16.sp, color = MaterialTheme.colors.onBackground)
                Text(c.number, fontSize = 13.sp, color = MaterialTheme.colors.secondary)
              }
              if (sending) Text(if (c.number in sent) labels.sent else labels.send, fontSize = 13.sp, color = MaterialTheme.colors.primary)
            }
          }
        }
        if (!sending) Button(
          { sending = true }, Modifier.fillMaxWidth().padding(DEFAULT_PADDING).navigationBarsPadding(), enabled = selected.isNotEmpty()
        ) { Text(labels.next(selected.size)) }
      }
    }
  }
}
