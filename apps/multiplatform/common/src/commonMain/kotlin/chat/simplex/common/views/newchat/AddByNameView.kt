package chat.simplex.common.views.newchat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import chat.simplex.common.views.helpers.*
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.stringResource

// Shapat: add a contact by name or code, in one step. The other side is connected at once, unless they chose to approve each request.
@Composable
fun AddByNameView(rhId: Long?, close: () -> Unit) {
  var text by remember { mutableStateOf("") }
  var inProgress by remember { mutableStateOf(false) }
  val target = SapatDirectory.connectTarget(text)

  fun connect() {
    val t = target ?: return
    inProgress = true
    withBGApi {
      try {
        sapatConnect(rhId, t, close)
      } finally {
        inProgress = false
      }
    }
  }

  ColumnWithScrollBar(Modifier.fillMaxWidth()) {
    AppBarTitle(stringResource(MR.strings.sapat_add_contact))
    Column(Modifier.padding(horizontal = DEFAULT_PADDING)) {
      Text(stringResource(MR.strings.sapat_add_contact_desc), color = MaterialTheme.colors.secondary)
      Spacer(Modifier.height(DEFAULT_PADDING))
      OutlinedTextField(
        value = text,
        onValueChange = { text = it.trimStart() },
        modifier = Modifier.fillMaxWidth(),
        shape = sapatFieldShape(),
        colors = sapatFieldColors(),
        singleLine = true,
        label = { Text(stringResource(MR.strings.sapat_name_or_code)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { connect() }),
        isError = text.isNotEmpty() && target == null,
      )
      Spacer(Modifier.height(DEFAULT_PADDING))
      SapatPrimaryButton(stringResource(MR.strings.sapat_send_request), { connect() }, enabled = target != null && !inProgress)
      Spacer(Modifier.height(DEFAULT_PADDING * 2))
      SapatMyName()
    }
  }
}

// The request is sent as soon as the name or code resolves: the chat opens connected, with nothing more to tap.
// Anything else the name resolves to (a known contact, the user's own name, a group) is shown as for any link.
private suspend fun sapatConnect(rhId: Long?, target: String, close: () -> Unit) {
  val inProgress = mutableStateOf(true)
  val r = chatModel.controller.apiConnectPlan(rhId, target, inProgress = inProgress) ?: return
  val plan = (r.connectionPlan as? ConnectionPlan.ContactAddress)?.contactAddressPlan
  val lapsed = (r.connectionPlan.nameChange as? NameChange.Lapsed)?.nameWarning != null
  val data = (plan as? ContactAddressPlan.Ok)?.contactSLinkData_
  val prepared = (plan as? ContactAddressPlan.ContactViaAddress)?.contact?.takeIf { it.nextConnectPrepared }
  when {
    lapsed -> planAndConnect(rhId, target, close = close)
    data != null -> {
      val chat = chatModel.controller.apiPrepareContact(rhId, r.connLink, data, r.planSimplexName?.nameDomain) ?: return
      withContext(Dispatchers.Main) { chatModel.chatsContext.addChat(chat) }
      sapatConnectPrepared(rhId, chat.chatInfo.apiId)
      withContext(Dispatchers.Main) { openChat_(chatModel, rhId, close, chat) }
    }
    prepared != null -> {
      sapatConnectPrepared(rhId, prepared.contactId)
      withContext(Dispatchers.Main) { openKnownContact(chatModel, rhId, close, prepared) }
    }
    else -> planAndConnect(rhId, target, close = close)
  }
}

private suspend fun sapatConnectPrepared(rhId: Long?, contactId: Long) {
  val contact = chatModel.controller.apiConnectPreparedContact(rhId, contactId, incognito = false, msg = null) ?: return
  withContext(Dispatchers.Main) { chatModel.chatsContext.updateContact(rhId, contact) }
}

// The user's own name and code, to send to the people who want to add them.
@Composable
fun SapatMyName() {
  val userId = chatModel.currentUser.value?.userId ?: return
  val running = chatModel.chatRunning.value
  LaunchedEffect(userId, running) {
    if (running == true) withBGApi { SapatDirectory.ensureRegistered(chatModel.remoteHostId()) }
  }
  // read again when the entry is saved: the name is claimed after the home screen shows
  val e = remember(userId, SapatDirectory.saved.value) { SapatDirectory.entry(userId) } ?: return
  val name = e.name ?: e.code
  val clipboard = LocalClipboardManager.current
  val invite = String.format(generalGetString(MR.strings.sapat_share_my_name), name, e.code)
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SapatLabel(stringResource(MR.strings.sapat_your_name))
    Text(name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colors.onBackground)
    Text(String.format(generalGetString(MR.strings.sapat_my_name_text), e.code.chunked(3).joinToString("-")), color = MaterialTheme.colors.secondary, fontSize = 14.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      SapatSecondaryButton(stringResource(MR.strings.copy_verb), { clipboard.setText(AnnotatedString(name)) }, Modifier.weight(1f))
      SapatSecondaryButton(stringResource(MR.strings.share_verb), { clipboard.shareText(invite) }, Modifier.weight(1f))
    }
  }
}
