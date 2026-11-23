package chat.simplex.common.views.localauth

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatModel.controller
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.helpers.DatabaseUtils.ksSelfDestructPassword
import chat.simplex.common.views.helpers.DatabaseUtils.ksAppPassword
import chat.simplex.common.views.onboarding.OnboardingStage
import chat.simplex.common.platform.*
import chat.simplex.common.views.database.*
import chat.simplex.res.*
import kotlinx.coroutines.delay

@Composable
fun LocalAuthView(m: ChatModel, authRequest: LocalAuthRequest) {
  val passcode = rememberSaveable { mutableStateOf("") }
  val allowToReact = rememberSaveable { mutableStateOf(true) }
  if (!allowToReact.value) {
    BackHandler {
      // do nothing until submit action finishes to prevent concurrent removing of storage
    }
  }
  val pauseLeft = remember { SapatWipe.pauseLeft }
  LaunchedEffect(Unit) { SapatWipe.resumePause() }
  val reason = if (pauseLeft.value > 0) String.format(generalGetString(MR.strings.sapat_pin_pause), SapatWipe.pauseText(pauseLeft.value)) else authRequest.reason
  PasscodeView(passcode, authRequest.title ?: stringResource(MR.strings.la_enter_app_passcode), reason, stringResource(MR.strings.submit_passcode), buttonsEnabled = allowToReact,
    submit = {
      val sdPassword = ksSelfDestructPassword.get()
      if (SapatWipe.isWipePin(passcode.value)) {
        allowToReact.value = false
        deleteStorageAndRestart(m, null) { r -> authRequest.completed(r) }
      } else if (pauseLeft.value > 0) {
        // a wipe PIN works during the wait; any other PIN is neither checked nor counted
        passcode.value = ""
      } else if (passcode.value != authRequest.password && !(sdPassword == passcode.value && authRequest.selfDestruct)) {
        when (val wrong = SapatWipe.afterWrongPin(SapatWipe.wrongPin(), m.controller.appPrefs.sapatWipeOnWrongPin.get())) {
          SapatWipe.WrongPin.Wipe -> {
            allowToReact.value = false
            deleteStorageAndRestart(m, null) { r -> authRequest.completed(r) }
          }
          SapatWipe.WrongPin.LastAttempt -> authRequest.completed(LAResult.Error(generalGetString(MR.strings.sapat_wipe_last_attempt)))
          is SapatWipe.WrongPin.Pause -> {
            SapatWipe.startPause(wrong.seconds)
            val text = if (wrong.seconds > 0) String.format(generalGetString(MR.strings.sapat_pin_wrong_wait), SapatWipe.pauseText(wrong.seconds)) else generalGetString(MR.strings.incorrect_passcode)
            authRequest.completed(LAResult.Error(text))
          }
        }
      } else if (sdPassword == passcode.value && authRequest.selfDestruct) {
        allowToReact.value = false
        deleteStorageAndRestart(m, sdPassword) { r ->
          authRequest.completed(r)
        }
      } else {
        val r: LAResult = if (passcode.value == authRequest.password) {
          SapatWipe.resetWrong()
          if (authRequest.selfDestruct && sdPassword != null && controller.getChatCtrl() == -1L) {
            initChatControllerOnStart()
          }
          LAResult.Success
        } else {
          LAResult.Error(generalGetString(MR.strings.incorrect_passcode))
        }
        authRequest.completed(r)
      }
    },
    cancel = {
      authRequest.completed(LAResult.Error(generalGetString(MR.strings.authentication_cancelled)))
    })
}

// password == null (Shapat wipe): no app passcode afterwards, lock off, directory entries forgotten
private fun deleteStorageAndRestart(m: ChatModel, password: String?, completed: (LAResult) -> Unit) {
  withLongRunningApi {
    try {
      /** Waiting until [initChatController] finishes */
      while (m.ctrlInitInProgress.value) {
        delay(50)
      }
      if (m.chatRunning.value == true) {
        stopChatAsync(m)
      }
      val ctrl = m.controller.getChatCtrl()
      if (ctrl != null && ctrl != -1L) {
        /**
         * The following sequence can bring a user here:
         * the user opened the app, entered app passcode, went to background, returned back, entered self-destruct code.
         * In this case database should be closed to prevent possible situation when OS can deny database removal command
         * */
        chatCloseStore(ctrl)
      }
      deleteChatDatabaseFilesAndState()
      if (password != null) {
        ksAppPassword.set(password)
      } else {
        ksAppPassword.remove()
        m.controller.appPrefs.performLA.set(false)
        m.controller.appPrefs.sapatDirectory.set(null)
        m.controller.appPrefs.sapatClaimName.set(null)
        SapatWipe.resetWrong()
      }
      ksSelfDestructPassword.remove()
      ntfManager.cancelAllNotifications()
      val selfDestructPref = m.controller.appPrefs.selfDestruct
      val displayNamePref = m.controller.appPrefs.selfDestructDisplayName
      val displayName = displayNamePref.get()
      selfDestructPref.set(false)
      displayNamePref.set(null)
      reinitChatController()
      if (m.currentUser.value != null) {
        return@withLongRunningApi
      }
      var profile: Profile? = null
      if (!displayName.isNullOrEmpty()) {
        profile = Profile(displayName = displayName, fullName = "", shortDescr = null)
      }
      val createdUser = m.controller.apiCreateActiveUser(null, profile, pastTimestamp = true)
      m.currentUser.value = createdUser
      m.controller.appPrefs.onboardingStage.set(OnboardingStage.OnboardingComplete)
      if (createdUser != null) {
        m.controller.startChat(createdUser)
      }
      ModalManager.closeAllModalsEverywhere()
      AlertManager.shared.hideAllAlerts()
      AlertManager.privacySensitive.hideAllAlerts()
      completed(LAResult.Success)
    } catch (e: Exception) {
      Log.e(TAG, "Unable to delete storage: ${e.stackTraceToString()}")
      completed(LAResult.Error(generalGetString(MR.strings.incorrect_passcode)))
    }
  }
}

suspend fun reinitChatController() {
  chatModel.chatDbChanged.value = true
  chatModel.chatDbStatus.value = null
  try {
    initChatController()
  } catch (e: Exception) {
    Log.d(TAG, "initializeChat ${e.stackTraceToString()}")
  }
  chatModel.chatDbChanged.value = false
}
