package chat.simplex.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import chat.simplex.common.AppLock
import chat.simplex.common.simplexWindowState
import chat.simplex.common.model.SapatDirectory
import chat.simplex.common.ui.theme.SimpleXTheme
import chat.simplex.common.views.chatlist.SapatName
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.common.views.localauth.SapatWipe
import chat.simplex.common.views.localauth.SetAppPasscodeView
import chat.simplex.res.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Shapat: the app opens only with a PIN, wrong PINs make the user wait unless they chose the wipe, and each profile chooses its name.
@OptIn(ExperimentalTestApi::class)
class SapatGatesTest {
  private fun entry(name: String?, chosen: Boolean = false) = SapatDirectory.Entry(token = "t", code = "123456789", name = name, chosen = chosen)

  @BeforeTest
  fun setUp() {
    // the PIN pad reads the window's orientation
    simplexWindowState.windowState = WindowState(size = DpSize(390.dp, 844.dp))
  }

  @Test
  fun pinIsRequiredOnAndroidUntilItIsSet() {
    assertTrue(AppLock.pinRequired(performLA = false, android = true))
    assertFalse(AppLock.pinRequired(performLA = true, android = true))
    assertFalse(AppLock.pinRequired(performLA = false, android = false))
  }

  @Test
  fun mandatoryPinCanNotBeCancelled() = runComposeUiTest {
    var cancelled = false
    setContent { SimpleXTheme { SetAppPasscodeView(mandatory = true, submit = {}, cancel = { cancelled = true }, close = { cancelled = true }) } }
    onAllNodesWithText(generalGetString(MR.strings.cancel_verb)).assertCountEquals(0)
    for (d in "1379") onNodeWithText(d.toString()).performClick()
    onNodeWithText(generalGetString(MR.strings.save_verb)).performClick()
    // the confirmation can be cancelled, back to the first entry
    onNodeWithText(generalGetString(MR.strings.confirm_passcode)).assertExists()
    onNodeWithText(generalGetString(MR.strings.cancel_verb)).performClick()
    onNodeWithText(generalGetString(MR.strings.new_passcode)).assertExists()
    onAllNodesWithText(generalGetString(MR.strings.cancel_verb)).assertCountEquals(0)
    assertFalse(cancelled)
  }

  @Test
  fun optionalPasscodeCanBeCancelled() = runComposeUiTest {
    var cancelled = false
    setContent { SimpleXTheme { SetAppPasscodeView(submit = {}, cancel = { cancelled = true }, close = {}) } }
    onNodeWithText(generalGetString(MR.strings.cancel_verb)).performClick()
    assertTrue(cancelled)
  }

  @Test
  fun wrongPinsPauseLongerEachTime() {
    assertEquals(listOf(0, 0, 30, 60, 300, 900, 900), (1..7).map { SapatWipe.pauseSeconds(it) })
    assertEquals("0:30", SapatWipe.pauseText(30))
    assertEquals("15:00", SapatWipe.pauseText(900))
  }

  @Test
  fun wrongPinsWipeOnlyWhenTheUserChoseIt() {
    for (wrong in 1..20) assertIs<SapatWipe.WrongPin.Pause>(SapatWipe.afterWrongPin(wrong, wipeOnWrongPin = false))
    assertEquals(30, (SapatWipe.afterWrongPin(3, wipeOnWrongPin = false) as SapatWipe.WrongPin.Pause).seconds)
    assertIs<SapatWipe.WrongPin.Pause>(SapatWipe.afterWrongPin(1, wipeOnWrongPin = true))
    assertIs<SapatWipe.WrongPin.LastAttempt>(SapatWipe.afterWrongPin(2, wipeOnWrongPin = true))
    assertIs<SapatWipe.WrongPin.Wipe>(SapatWipe.afterWrongPin(3, wipeOnWrongPin = true))
  }

  @Test
  fun directoryNames() {
    for (name in listOf("nikola", "ana-2", "abc", "a".repeat(32))) assertTrue(SapatDirectory.validName(name), name)
    for (name in listOf("", "ab", "Nikola", "1nikola", "-ana", "ana maria", "šapat", "a".repeat(33))) assertFalse(SapatDirectory.validName(name), name)
  }

  @Test
  fun temporaryNameIsNotChosen() {
    assertFalse(SapatDirectory.nameChosen(null))
    assertFalse(SapatDirectory.nameChosen(entry(null)))
    assertFalse(SapatDirectory.nameChosen(entry("sova-48213")))
    assertTrue(SapatDirectory.nameChosen(entry("sova-48213", chosen = true)))
    assertTrue(SapatDirectory.nameChosen(entry("nikola")))
  }

  @Test
  fun nameGate() {
    fun check(android: Boolean = true, remote: Boolean = false, hidden: Boolean = false, later: Boolean = false, e: SapatDirectory.Entry? = null, wantsClaim: Boolean = false, displayName: String = "nikola") =
      SapatName.check(android, remote, hidden, later, e, wantsClaim, displayName)
    // a new profile's name is claimed without asking
    assertEquals("nikola", (check(wantsClaim = true) as SapatName.Check.ClaimName).name)
    assertIs<SapatName.Check.ClaimName>(check(wantsClaim = true, e = entry("sova-48213")))
    // an earlier profile, or a display name that can't be a directory name, is asked
    assertIs<SapatName.Check.Ask>(check())
    assertIs<SapatName.Check.Ask>(check(e = entry("sova-48213")))
    assertIs<SapatName.Check.Ask>(check(wantsClaim = true, displayName = "Nikola B"))
    // nothing once the name is chosen, after "continue without a name" in this run, or where the gate doesn't apply
    assertIs<SapatName.Check.Nothing>(check(e = entry("nikola")))
    assertIs<SapatName.Check.Nothing>(check(later = true))
    assertIs<SapatName.Check.Nothing>(check(android = false))
    assertIs<SapatName.Check.Nothing>(check(remote = true))
    assertIs<SapatName.Check.Nothing>(check(hidden = true))
  }
}
