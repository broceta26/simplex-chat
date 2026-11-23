package chat.simplex.common.views.localauth

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import chat.simplex.common.model.ChatController.appPrefs
import kotlinx.coroutines.*
import java.security.MessageDigest

// Shapat: the app is wiped at once on any of the user's wipe PINs. Wrong PINs make the user wait longer each time;
// only when the user turned it on in settings, the third wrong PIN wipes the app instead (with a warning before it).
// Wipe PINs are kept only as SHA-256 hashes.
object SapatWipe {
  const val MAX_WRONG = 3

  sealed class WrongPin {
    object Wipe: WrongPin()
    object LastAttempt: WrongPin()
    class Pause(val seconds: Int): WrongPin()
  }

  /** The wait after this many wrong PINs in a row: the first two are free, then it grows to 15 minutes. */
  fun pauseSeconds(wrong: Int): Int = when {
    wrong < MAX_WRONG -> 0
    wrong == 3 -> 30
    wrong == 4 -> 60
    wrong == 5 -> 300
    else -> 900
  }

  fun afterWrongPin(wrong: Int, wipeOnWrongPin: Boolean): WrongPin = when {
    wipeOnWrongPin && wrong >= MAX_WRONG -> WrongPin.Wipe
    wipeOnWrongPin && wrong == MAX_WRONG - 1 -> WrongPin.LastAttempt
    else -> WrongPin.Pause(pauseSeconds(wrong))
  }

  // The seconds left are saved and counted down only while the app runs: changing the clock or restarting the phone doesn't shorten the wait.
  val pauseLeft: MutableState<Int> by lazy { mutableStateOf(appPrefs.sapatPinPauseLeft.get()) }
  private var pauseJob: Job? = null

  fun startPause(seconds: Int) {
    if (seconds <= 0) return
    appPrefs.sapatPinPauseLeft.set(seconds)
    pauseLeft.value = seconds
    resumePause()
  }

  fun resumePause() {
    if (pauseLeft.value <= 0 || pauseJob?.isActive == true) return
    pauseJob = CoroutineScope(Dispatchers.Default).launch {
      while (pauseLeft.value > 0) {
        delay(1000)
        val left = pauseLeft.value - 1
        appPrefs.sapatPinPauseLeft.set(left)
        pauseLeft.value = left
      }
    }
  }

  fun pauseText(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

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

  fun resetWrong() {
    appPrefs.sapatWrongPins.set(0)
    appPrefs.sapatPinPauseLeft.set(0)
    pauseLeft.value = 0
  }
}
