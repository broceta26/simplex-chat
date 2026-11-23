package chat.simplex.common.views.usersettings

import androidx.compose.ui.unit.dp
import chat.simplex.common.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.SapatDirectory
import chat.simplex.common.platform.ColumnWithScrollBar
import chat.simplex.common.platform.chatModel
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.views.helpers.*
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.stringResource
import java.text.DateFormat
import java.util.Date

// Shapat: the profile's name (a mask) and code (its permanent number in the directory).
@Composable
fun SapatIdentityView() {
  val userId = chatModel.currentUser.value?.userId ?: return
  var entry by remember { mutableStateOf(SapatDirectory.entry(userId)) }
  var nameText by remember { mutableStateOf(entry?.name ?: "") }
  var busy by remember { mutableStateOf(false) }
  val clipboard = LocalClipboardManager.current

  LaunchedEffect(Unit) {
    if (entry == null) {
      busy = true
      entry = SapatDirectory.ensureRegistered(chatModel.remoteHostId())
      nameText = entry?.name ?: ""
      busy = false
    }
  }

  fun run(action: suspend () -> SapatDirectory.Entry) {
    busy = true
    withBGApi {
      try {
        entry = action()
        nameText = entry?.name ?: ""
      } catch (e: SapatDirectory.DirectoryError) {
        AlertManager.shared.showAlertMsg(generalGetString(MR.strings.sapat_error), directoryErrorText(e.error))
      } catch (e: Exception) {
        AlertManager.shared.showAlertMsg(generalGetString(MR.strings.sapat_error), e.message)
      } finally {
        busy = false
      }
    }
  }

  ColumnWithScrollBar(Modifier.fillMaxWidth()) {
    AppBarTitle(stringResource(MR.strings.sapat_name_and_code))
    val e = entry
    Column(Modifier.padding(horizontal = DEFAULT_PADDING), verticalArrangement = Arrangement.spacedBy(DEFAULT_PADDING)) {
      if (e == null) {
        Text(stringResource(if (busy) MR.strings.sapat_registering else MR.strings.sapat_not_registered), color = MaterialTheme.colors.secondary)
        if (!busy) Button(onClick = { run { SapatDirectory.ensureRegistered(chatModel.remoteHostId()) ?: throw SapatDirectory.DirectoryError("offline") } }) {
          Text(stringResource(MR.strings.retry_verb))
        }
        return@Column
      }
      SapatCard {
        SapatLabel(stringResource(MR.strings.sapat_your_code))
        Spacer(Modifier.height(10.dp))
        SapatCodeText(e.code.chunked(3).joinToString(if (sapatStyleB()) "-" else " "))
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          SapatSecondaryButton(stringResource(MR.strings.sapat_new_code), {
            AlertManager.shared.showAlertDialog(
              title = generalGetString(MR.strings.sapat_new_code_question),
              text = generalGetString(MR.strings.sapat_new_code_desc),
              confirmText = generalGetString(MR.strings.sapat_new_code),
              destructive = true,
              onConfirm = { run { SapatDirectory.newCode(userId) } },
            )
          }, Modifier.weight(1f), enabled = !busy)
          SapatSecondaryButton(stringResource(MR.strings.copy_verb), { clipboard.setText(AnnotatedString(e.code)) }, Modifier.weight(1f))
        }
      }
      SapatLabel(stringResource(MR.strings.sapat_your_name))
      OutlinedTextField(
        value = nameText,
        onValueChange = { nameText = it.trim().lowercase() },
        singleLine = true,
        enabled = !e.reserved && !busy,
        modifier = Modifier.fillMaxWidth(),
        shape = sapatFieldShape(),
        colors = sapatFieldColors(),
      )
      if (!e.reserved) {
        SapatPrimaryButton(stringResource(MR.strings.sapat_save_name), { run { SapatDirectory.setName(userId, nameText) } }, enabled = !busy && nameText != (e.name ?: "") && SapatDirectory.connectTarget(nameText) != null)
      }
      Text(
        when {
          e.reserved -> generalGetString(MR.strings.sapat_name_reserved)
          e.canReserve -> generalGetString(MR.strings.sapat_name_can_reserve)
          else -> String.format(generalGetString(MR.strings.sapat_name_reserve_from), DateFormat.getDateInstance().format(Date(e.reservableAt * 1000)))
        },
        color = MaterialTheme.colors.secondary, fontSize = 14.sp,
      )
      if (e.canReserve && !e.reserved) {
        SapatSecondaryButton(stringResource(MR.strings.sapat_reserve_name), { run { SapatDirectory.reserveName(userId) } }, Modifier.fillMaxWidth(), enabled = !busy)
      }
      Text(stringResource(MR.strings.sapat_identity_footer), color = MaterialTheme.colors.secondary, fontSize = 13.sp)
    }
  }
}

fun directoryErrorText(error: String): String = when (error) {
  "nameTaken" -> generalGetString(MR.strings.sapat_err_name_taken)
  "badName" -> generalGetString(MR.strings.sapat_err_bad_name)
  "nameReserved" -> generalGetString(MR.strings.sapat_name_reserved)
  "cannotReserveYet" -> generalGetString(MR.strings.sapat_err_cannot_reserve)
  "rateLimited" -> generalGetString(MR.strings.please_try_later)
  else -> error
}
