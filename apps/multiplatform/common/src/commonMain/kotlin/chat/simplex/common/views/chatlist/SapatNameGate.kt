package chat.simplex.common.views.chatlist

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.directoryErrorText
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.withTimeoutOrNull

// Shapat: each profile has to choose its name in the directory, and the name is also the profile's display name.
// A new profile's name is claimed without asking. When the directory can't be reached the user gets in, and is asked
// again the next time the app starts.
object SapatName {
  sealed class Claim {
    object Done: Claim()
    class Rejected(val error: String): Claim()
    object Later: Claim()
  }

  sealed class Check {
    object Nothing: Check()
    object Ask: Check()
    class ClaimName(val name: String): Check()
  }

  val askUserId = mutableStateOf<Long?>(null)
  val rejected = mutableStateOf<String?>(null)
  private val askedLater = mutableSetOf<Long>()

  fun check(android: Boolean, remote: Boolean, hidden: Boolean, later: Boolean, entry: SapatDirectory.Entry?, wantsClaim: Boolean, displayName: String): Check = when {
    !android || remote || hidden || later -> Check.Nothing
    SapatDirectory.nameChosen(entry) -> Check.Nothing
    wantsClaim && SapatDirectory.validName(displayName) -> Check.ClaimName(displayName)
    else -> Check.Ask
  }

  fun later(userId: Long) {
    askedLater.add(userId)
    askUserId.value = null
  }

  suspend fun checkUser(user: User, rh: Long?) {
    val userId = user.userId
    val check = check(appPlatform.isAndroid, rh != null, user.hidden, userId in askedLater, SapatDirectory.entry(userId), SapatDirectory.wantsClaim(userId), user.profile.displayName)
    when (check) {
      Check.Nothing -> {}
      Check.Ask -> askUserId.value = userId
      is Check.ClaimName -> when (val r = claim(userId, check.name, rh)) {
        Claim.Done -> {}
        is Claim.Rejected -> {
          rejected.value = directoryErrorText(r.error)
          askUserId.value = userId
        }
        Claim.Later -> askedLater.add(userId)
      }
    }
  }

  suspend fun claim(userId: Long, name: String, rh: Long?): Claim =
    try {
      val registered = withTimeoutOrNull(30_000) { SapatDirectory.ensureRegistered(rh) }
      if (registered == null || chatModel.currentUser.value?.userId != userId) {
        Claim.Later
      } else {
        SapatDirectory.setName(userId, name)
        SapatDirectory.clearClaim(userId)
        Claim.Done
      }
    } catch (e: SapatDirectory.DirectoryError) {
      when (e.error) {
        "nameTaken", "badName", "nameReserved" -> {
          SapatDirectory.clearClaim(userId)
          Claim.Rejected(e.error)
        }
        else -> Claim.Later
      }
    } catch (e: Exception) {
      Log.e(TAG, "SapatName.claim: ${e.message}")
      Claim.Later
    }
}

@Composable
fun SapatNameGate() {
  val user = chatModel.currentUser.value
  val running = chatModel.chatRunning.value
  LaunchedEffect(user?.userId, running) {
    if (user != null && running == true) SapatName.checkUser(user, chatModel.remoteHostId())
  }
  if (user != null && SapatName.askUserId.value == user.userId) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background.copy(1f), contentColor = LocalContentColor.current) {
      SapatChooseNameView(user)
    }
  }
}

@Composable
private fun SapatChooseNameView(user: User) {
  val displayName = user.profile.displayName.trim().lowercase()
  var name by remember { mutableStateOf(if (SapatName.rejected.value == null && SapatDirectory.validName(displayName)) displayName else "") }
  var error by remember { mutableStateOf(SapatName.rejected.value) }
  var unreachable by remember { mutableStateOf(false) }
  var busy by remember { mutableStateOf(false) }
  BackHandler { }

  fun save() {
    busy = true
    error = null
    withBGApi {
      try {
        val rh = chatModel.remoteHostId()
        when (val r = SapatName.claim(user.userId, name, rh)) {
          SapatName.Claim.Done -> {
            if (user.profile.displayName != name) {
              val updated = chatModel.controller.apiUpdateProfile(rh, user.profile.toProfile().copy(displayName = name))
              if (updated != null) chatModel.updateCurrentUser(rh, updated.first)
            }
            SapatName.rejected.value = null
            SapatName.askUserId.value = null
          }
          is SapatName.Claim.Rejected -> error = directoryErrorText(r.error)
          SapatName.Claim.Later -> {
            error = generalGetString(MR.strings.sapat_choose_name_unreachable)
            unreachable = true
          }
        }
      } finally {
        busy = false
      }
    }
  }

  ColumnWithScrollBar(Modifier.fillMaxWidth().systemBarsPadding()) {
    AppBarTitle(stringResource(MR.strings.sapat_choose_name_title))
    Column(Modifier.padding(horizontal = DEFAULT_PADDING), verticalArrangement = Arrangement.spacedBy(DEFAULT_PADDING)) {
      Text(stringResource(MR.strings.sapat_choose_name_text), color = MaterialTheme.colors.secondary, fontSize = 15.sp)
      SapatLabel(stringResource(MR.strings.sapat_your_name))
      OutlinedTextField(
        value = name,
        onValueChange = { name = it.trim().lowercase(); error = null },
        singleLine = true,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        shape = sapatFieldShape(),
        colors = sapatFieldColors(),
      )
      Text(error ?: stringResource(MR.strings.sapat_err_bad_name), color = if (error != null) MaterialTheme.colors.error else MaterialTheme.colors.secondary, fontSize = 13.sp)
      SapatPrimaryButton(stringResource(MR.strings.sapat_save_name), { save() }, enabled = !busy && SapatDirectory.validName(name))
      if (unreachable) {
        SapatSecondaryButton(stringResource(MR.strings.sapat_choose_name_later), { SapatName.later(user.userId) }, Modifier.fillMaxWidth(), enabled = !busy)
        Text(stringResource(MR.strings.sapat_choose_name_later_text), color = MaterialTheme.colors.secondary, fontSize = 13.sp)
      }
      Spacer(Modifier.height(DEFAULT_PADDING))
    }
  }
}
