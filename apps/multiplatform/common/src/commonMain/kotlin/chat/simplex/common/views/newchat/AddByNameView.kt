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
import chat.simplex.common.model.SapatDirectory
import chat.simplex.common.views.helpers.*
import chat.simplex.common.platform.ColumnWithScrollBar
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.stringResource

// Šapat: add a contact by name or code. The other side gets a contact request and approves it.
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
        planAndConnect(rhId, t, close = close, cleanup = { inProgress = false })
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
        onValueChange = { text = it.trim() },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(MR.strings.sapat_name_or_code)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { connect() }),
        isError = text.isNotEmpty() && target == null,
      )
      Spacer(Modifier.height(DEFAULT_PADDING))
      Button(onClick = { connect() }, enabled = target != null && !inProgress, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(MR.strings.sapat_send_request))
      }
    }
  }
}
