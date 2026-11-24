package chat.simplex.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.simplexWindowState
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chat.*
import chat.simplex.common.views.chat.group.PreviewGroupChatInfoLayout
import chat.simplex.common.views.chatlist.SapatHomeView
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.SapatThemeView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import java.io.File
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test

// Renders the Shapat screens in style A (light and dark) and style B, for review. Runs only when SAPAT_SHOTS is set.
class SapatShotsTest {
  private val w = 390
  private val h = 844

  private fun contact(id: Long, name: String) =
    ChatInfo.Direct(Contact.sampleData.copy(contactId = id, localDisplayName = name, profile = LocalProfile.sampleData.copy(displayName = name, fullName = "")))

  private fun item(id: Long, text: String, sent: Boolean, ts: String, member: GroupMember? = null) =
    ChatItem.getSampleData(id = id, dir = if (sent) CIDirection.DirectSnd() else if (member != null) CIDirection.GroupRcv(member) else CIDirection.DirectRcv(), ts = Instant.parse(ts), text = text)

  private fun sampleChats(): List<Chat> {
    val group = ChatInfo.Group(GroupInfo.sampleData.copy(groupId = 9, localDisplayName = "Kancelarija", groupProfile = GroupProfile.sampleData.copy(displayName = "Kancelarija", fullName = "")), groupChatScope = null)
    val bora = GroupMember.sampleData.copy(localDisplayName = "Branislav", memberProfile = GroupMember.sampleData.memberProfile.copy(displayName = "Branislav"))
    return listOf(
      Chat(null, contact(2, "Marko"), listOf(item(1, "Vidimo se sutra u 10", false, "2026-11-22T03:52:00Z")), Chat.ChatStats(unreadCount = 2)),
      Chat(null, contact(3, "Anna"), listOf(item(2, "Poslao sam ti fajl", true, "2026-11-21T18:10:00Z"))),
      Chat(null, group, listOf(item(3, "Može", false, "2026-11-21T12:00:00Z", bora)), Chat.ChatStats(unreadCount = 8)),
      Chat(null, contact(4, "Stefan Nikolić"), listOf(item(4, "Javiću ti kad stignem", false, "2026-11-20T09:00:00Z"))),
      Chat(null, contact(5, "titan"), listOf(item(5, "Radi sve, hvala!", true, "2026-11-19T20:00:00Z"))),
    )
  }

  private fun shot(file: File, content: @Composable () -> Unit) {
    var height = 0
    val frame = renderScene(width = w, height = h, frames = 4) {
      AppBarHandler.appBarMaxHeightPx = with(LocalDensity.current) { AppBarHeight.roundToPx() }
      Box(Modifier.size(w.dp, h.dp).background(CurrentColors.collectAsState().value.colors.background).onSizeChanged { height = it.height }) {
        CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) { SimpleXTheme { content() } }
      }
    }
    val img = Surface.makeRasterN32Premul(w, h).run { canvas.drawImage(frame, 0f, 0f); makeImageSnapshot() }
    file.writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
  }

  @Test
  fun renderSapatScreens() {
    val dir = System.getenv("SAPAT_SHOTS")?.let { File(it).apply { mkdirs() } } ?: return
    val theme = appPrefs.currentTheme.get()!!
    val style = appPrefs.sapatStyle.get()
    val overrides = appPrefs.themeOverrides.get()
    Locale.setDefault(Locale.forLanguageTag("sr-Latn-RS"))
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Belgrade"))
    simplexWindowState.windowState = WindowState(size = DpSize(w.dp, h.dp))
    appPrefs.densityScale.set(1f)
    appPrefs.themeOverrides.set(listOf())
    val chats = sampleChats()
    try {
      for ((st, base) in listOf("A" to DefaultTheme.LIGHT, "A" to DefaultTheme.DARK, "B" to DefaultTheme.DARK, "C" to DefaultTheme.DARK, "T" to DefaultTheme.DARK)) {
        appPrefs.sapatStyle.set(st)
        ThemeManager.applyTheme(base.themeName)
        val tag = if (st == "A") "A-${base.name.lowercase()}" else st
        ChatModel.chatRunning.value = true
        ChatModel.chatsContext.chats.value = androidx.compose.runtime.snapshots.SnapshotStateList<Chat>().apply { addAll(chats) }
        shot(File(dir, "$tag-1-pocetna.png")) { SapatHomeView(ChatModel, MutableStateFlow(AnimatedViewState.GONE), stopped = false) }
        shot(File(dir, "$tag-2-razgovor.png")) {
          if (st == "A" || st == "T") {
            SimpleXThemeOverride(retroTheme(CurrentColors.collectAsState().value), RetroTypography) { PreviewGroupChatLayoutTerminal() }
          } else PreviewChatLayout()
        }
        shot(File(dir, "$tag-3-grupa.png")) { PreviewGroupChatInfoLayout() }
        shot(File(dir, "$tag-4-tema.png")) { SapatThemeView() }
      }
    } finally {
      appPrefs.sapatStyle.set(style)
      appPrefs.themeOverrides.set(overrides)
      ThemeManager.applyTheme(theme)
    }
  }
}
