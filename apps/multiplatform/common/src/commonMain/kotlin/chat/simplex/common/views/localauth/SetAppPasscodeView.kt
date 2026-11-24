package chat.simplex.common.views.localauth

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.views.helpers.DatabaseUtils
import chat.simplex.common.views.helpers.DatabaseUtils.ksAppPassword
import chat.simplex.common.views.helpers.DatabaseUtils.ksSelfDestructPassword
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.*

@Composable
fun SetAppPasscodeView(
  passcodeKeychain: DatabaseUtils.KeyStoreItem = ksAppPassword,
  prohibitedPasscodeKeychain: DatabaseUtils.KeyStoreItem = ksSelfDestructPassword,
  title: String = generalGetString(MR.strings.new_passcode),
  reason: String? = null,
  mandatory: Boolean = false,
  submit: () -> Unit,
  cancel: () -> Unit,
  close: () -> Unit
) {
  val passcode = rememberSaveable { mutableStateOf("") }
  var enteredPassword by rememberSaveable { mutableStateOf("") }
  var confirming by rememberSaveable { mutableStateOf(false) }

  @Composable
  fun SetPasswordView(title: String, submitLabel: String, submitEnabled: (((String) -> Boolean))? = null, submit: () -> Unit) {
    // Shapat: a mandatory PIN can't be skipped; going back from the confirmation returns to the first entry
    val back = {
      if (mandatory) {
        enteredPassword = ""
        passcode.value = ""
        confirming = false
      } else {
        close()
        cancel()
      }
    }
    BackHandler(onBack = back)
    PasscodeView(passcode, title = title, reason = reason, submitLabel = submitLabel, submitEnabled = submitEnabled, showCancel = !mandatory || confirming, checkOnly = mandatory, submit = submit, cancel = back)
  }

  if (confirming) {
    SetPasswordView(
      generalGetString(MR.strings.confirm_passcode),
      generalGetString(MR.strings.confirm_verb),
      submitEnabled = { pwd -> pwd == enteredPassword }
    ) {
      if (passcode.value == enteredPassword) {
        passcodeKeychain.set(passcode.value)
        enteredPassword = ""
        passcode.value = ""
        close()
        submit()
      }
    }
  } else {
    SetPasswordView(title, generalGetString(MR.strings.save_verb),
      // Do not allow to set app passcode == selfDestruct passcode
      submitEnabled = { pwd -> pwd != prohibitedPasscodeKeychain.get() && !(passcodeKeychain === ksAppPassword && SapatWipe.isWipePin(pwd)) }) {
      enteredPassword = passcode.value
      passcode.value = ""
      confirming = true
    }
  }
}
