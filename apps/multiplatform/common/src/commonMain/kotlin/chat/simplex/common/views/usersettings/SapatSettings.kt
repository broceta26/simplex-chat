package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import SectionDividerSpaced
import SectionItemViewSpaceBetween
import SectionTextFooter
import SectionView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.ColumnWithScrollBar
import chat.simplex.common.platform.appPlatform
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.database.DatabaseView
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.usersettings.networkAndServers.toggleSocksProxyAlert
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

// Šapat: two themes — light (paper terminal) and dark (Commodore 64) — or follow the phone.
@Composable
fun SapatThemeView() {
  val current = remember { ChatModel.controller.appPrefs.currentTheme.state }
  ColumnWithScrollBar {
    AppBarTitle(stringResource(MR.strings.sapat_theme))
    val style = remember { ChatModel.controller.appPrefs.sapatStyle.state }
    SectionView(stringResource(MR.strings.sapat_style)) {
      listOf("B" to MR.strings.sapat_style_b, "A" to MR.strings.sapat_style_a).forEach { (st, label) ->
        SectionItemViewSpaceBetween({
          ChatModel.controller.appPrefs.sapatStyle.set(st)
          ThemeManager.applyTheme(current.value ?: DefaultTheme.SYSTEM_THEME_NAME)
        }) {
          Text(stringResource(label))
          if ((style.value ?: "B") == st) {
            Icon(painterResource(MR.images.ic_check), null, tint = MaterialTheme.colors.primary)
          }
        }
      }
    }
    SectionTextFooter(stringResource(MR.strings.sapat_style_footer))
    if (style.value == "A") SectionView {
      listOf(
        DefaultTheme.SYSTEM_THEME_NAME to MR.strings.theme_system,
        DefaultTheme.LIGHT.themeName to MR.strings.theme_light,
        DefaultTheme.DARK.themeName to MR.strings.theme_dark,
      ).forEach { (name, label) ->
        SectionItemViewSpaceBetween({ ThemeManager.applyTheme(name) }) {
          Text(stringResource(label))
          if ((current.value ?: DefaultTheme.SYSTEM_THEME_NAME) == name) {
            Icon(painterResource(MR.images.ic_check), null, tint = MaterialTheme.colors.primary)
          }
        }
      }
    }
    SectionTextFooter(stringResource(MR.strings.sapat_theme_footer))
    SectionBottomSpacer()
  }
}

// Šapat: what most people never need — incognito, Tor, the database.
@Composable
fun SapatAdvancedView(chatModel: ChatModel, showSettingsModal: (@Composable (ChatModel) -> Unit) -> (() -> Unit), stopped: Boolean) {
  val incognito = remember { chatModel.controller.appPrefs.incognito.state }
  val useTor = remember { mutableStateOf(chatModel.controller.appPrefs.networkUseSocksProxy.get()) }
  ColumnWithScrollBar {
    AppBarTitle(stringResource(MR.strings.sapat_advanced))
    SectionView {
      PreferenceToggle(stringResource(MR.strings.incognito), disabled = stopped, checked = incognito.value) {
        chatModel.controller.appPrefs.incognito.set(it)
      }
    }
    SectionTextFooter(stringResource(MR.strings.sapat_incognito_footer))
    SectionDividerSpaced()
    SectionView {
      PreferenceToggle(stringResource(MR.strings.sapat_use_tor), disabled = stopped, checked = useTor.value) { enable ->
        toggleSocksProxyAlert(enable, chatModel.controller.appPrefs.networkProxy.get().port) { useTor.value = it }
      }
    }
    SectionTextFooter(stringResource(MR.strings.sapat_use_tor_footer))
    SectionDividerSpaced()
    SectionView {
      SettingsActionItem(painterResource(MR.images.ic_database), stringResource(MR.strings.your_chat_database), showSettingsModal { DatabaseView() }, disabled = stopped)
    }
    SectionBottomSpacer()
  }
}
