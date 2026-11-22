package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import SectionTextFooter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import chat.simplex.common.platform.ColumnWithScrollBar
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.localauth.SapatWipe
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.stringResource

// Shapat: PINs that wipe the app at once when entered on the lock screen.
@Composable
fun SapatWipePinsView() {
  var count by remember { mutableStateOf(SapatWipe.count()) }
  var pin by remember { mutableStateOf("") }
  var pin2 by remember { mutableStateOf("") }
  val appPin = remember { DatabaseUtils.ksAppPassword.get() }
  val valid = pin.length >= 4 && pin.all { it.isDigit() } && pin == pin2 && pin != appPin

  ColumnWithScrollBar {
    AppBarTitle(stringResource(MR.strings.sapat_wipe_pins))
    Column(Modifier.padding(horizontal = DEFAULT_PADDING), verticalArrangement = Arrangement.spacedBy(DEFAULT_PADDING)) {
      Text(String.format(stringResource(MR.strings.sapat_wipe_pins_count), count))
      OutlinedTextField(
        value = pin, onValueChange = { pin = it.filter { c -> c.isDigit() }.take(16) },
        label = { Text(stringResource(MR.strings.sapat_wipe_pin_new)) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = pin2, onValueChange = { pin2 = it.filter { c -> c.isDigit() }.take(16) },
        label = { Text(stringResource(MR.strings.sapat_wipe_pin_repeat)) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
        isError = pin2.isNotEmpty() && pin2 != pin,
      )
      if (pin.isNotEmpty() && pin == appPin) {
        Text(stringResource(MR.strings.sapat_wipe_pin_same_as_app), color = MaterialTheme.colors.error)
      }
      Button(onClick = {
        SapatWipe.addWipePin(pin)
        count = SapatWipe.count()
        pin = ""; pin2 = ""
      }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text(stringResource(MR.strings.sapat_wipe_pin_add)) }
      if (count > 0) {
        OutlinedButton(onClick = {
          AlertManager.shared.showAlertDialog(
            title = generalGetString(MR.strings.sapat_wipe_pins_clear_question),
            confirmText = generalGetString(MR.strings.delete_verb),
            destructive = true,
            onConfirm = { SapatWipe.clearWipePins(); count = 0 },
          )
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(MR.strings.sapat_wipe_pins_clear)) }
      }
    }
    SectionTextFooter(stringResource(MR.strings.sapat_wipe_footer))
    SectionBottomSpacer()
  }
}
