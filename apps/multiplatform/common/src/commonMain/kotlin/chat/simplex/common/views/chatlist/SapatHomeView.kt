package chat.simplex.common.views.chatlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.AppLock
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatModel.controller
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chat.item.ItemAction
import chat.simplex.common.views.onboarding.SetNotificationsModeAdditions
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.newchat.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.*
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.MutableStateFlow

// Shapat home: a top bar with the profile, the app name, search and a menu; the chats; one button for new things.
@Composable
fun SapatHomeView(chatModel: ChatModel, userPickerState: MutableStateFlow<AnimatedViewState>, stopped: Boolean) {
  val userId = chatModel.currentUser.value?.userId
  LaunchedEffect(userId, chatModel.chatRunning.value) {
    if (userId != null && chatModel.chatRunning.value == true && !stopped) {
      withBGApi { SapatDirectory.ensureRegistered(chatModel.remoteHostId()) }
    }
  }
  val searching = remember { mutableStateOf(false) }
  val search = remember { mutableStateOf("") }
  val chats = chatModel.chats.value
  val shown = remember(chats.toList(), search.value) {
    val q = search.value.trim().lowercase()
    chats.filter { it.chatInfo.chatType != ChatType.Local || q.isNotEmpty() }
      .filter { q.isEmpty() || it.chatInfo.chatViewName.lowercase().contains(q) }
  }
  Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
    val styleB = remember { chatModel.controller.appPrefs.sapatStyle.state }.value != "A"
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
      if (styleB) SapatTopBarB(chatModel, userPickerState, search, stopped) else SapatTopBar(chatModel, userPickerState, searching, search)
      when {
        chatModel.chatRunning.value == null ->
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(MR.strings.loading_chats), color = MaterialTheme.colors.secondary) }
        shown.isEmpty() && search.value.isNotEmpty() ->
          SapatSearchEmpty(search.value)
        shown.isEmpty() ->
          SapatEmptyHome(stopped)
        else ->
          LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            items(shown, key = { it.remoteHostId to it.id }) { chat ->
              ChatListNavLinkView(chat, remember { mutableStateOf(false) })
            }
          }
      }
    }
    if (!styleB && !searching.value && chatModel.chatRunning.value == true) {
      FloatingActionButton(
        onClick = { if (!stopped) showSapatNewSheet() },
        Modifier.navigationBarsPadding().padding(end = 20.dp, bottom = 20.dp).align(Alignment.BottomEnd).size(60.dp),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (stopped) MaterialTheme.colors.secondary else MaterialTheme.colors.primary,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp)
      ) {
        Icon(painterResource(MR.images.ic_edit_filled), stringResource(MR.strings.sapat_new), Modifier.size(24.dp))
      }
    }
  }
  if (appPlatform.isAndroid) {
    val canEnableNotifications = remember { derivedStateOf { chatModel.chatRunning.value == true } }
    if (canEnableNotifications.value) SetNotificationsModeAdditions()
    tryOrShowError("UserPicker", error = {}) {
      UserPicker(chatModel = chatModel, userPickerState = userPickerState, setPerformLA = AppLock::setPerformLA)
    }
  }
}

@Composable
private fun SapatTopBar(chatModel: ChatModel, userPickerState: MutableStateFlow<AnimatedViewState>, searching: MutableState<Boolean>, search: MutableState<String>) {
  Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    if (searching.value) {
      IconButton({ searching.value = false; search.value = "" }) {
        Icon(painterResource(MR.images.ic_arrow_back_ios_new), stringResource(MR.strings.back), tint = MaterialTheme.colors.onBackground)
      }
      val focus = remember { FocusRequester() }
      Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
        if (search.value.isEmpty()) Text(stringResource(MR.strings.sapat_search_hint), color = MaterialTheme.colors.secondary, fontSize = 17.sp)
        BasicTextField(
          search.value, { search.value = it },
          Modifier.fillMaxWidth().focusRequester(focus),
          singleLine = true,
          textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground, fontSize = 17.sp),
          cursorBrush = SolidColor(MaterialTheme.colors.primary)
        )
      }
      LaunchedEffect(Unit) { focus.requestFocus() }
    } else {
      Box(Modifier.padding(start = 6.dp).size(40.dp).clip(CircleShape).clickable { userPickerState.value = AnimatedViewState.VISIBLE }, contentAlignment = Alignment.Center) {
        val u = chatModel.currentUser.value
        if (u?.profile?.image != null) ProfileImage(40.dp, u.profile.image)
        else SapatInitials(u?.profile?.displayName ?: "", 40.dp)
      }
      Text(
        stringResource(MR.strings.app_name),
        Modifier.weight(1f).padding(start = 16.dp),
        style = MaterialTheme.typography.h2.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colors.onBackground
      )
      IconButton({ searching.value = true }) {
        Icon(painterResource(MR.images.ic_search), stringResource(MR.strings.search_verb), tint = MaterialTheme.colors.onBackground)
      }
      val menu = remember { mutableStateOf(false) }
      Box {
        IconButton({ menu.value = true }) {
          Icon(painterResource(MR.images.ic_more_vert), null, tint = MaterialTheme.colors.onBackground)
        }
        DefaultDropdownMenu(menu) {
          ItemAction(stringResource(MR.strings.sapat_new_group), painterResource(MR.images.ic_group), onClick = { menu.value = false; openNewGroup() })
          ItemAction(stringResource(MR.strings.sapat_name_and_code), painterResource(MR.images.ic_person_add), onClick = {
            menu.value = false
            ModalManager.start.showModalCloseable { _ -> SapatIdentityView() }
          })
          ItemAction(stringResource(MR.strings.settings_section_title_settings), painterResource(MR.images.ic_settings), onClick = {
            menu.value = false
            ModalManager.start.showModalCloseable(cardScreen = true) { close -> SettingsView(chatModel, AppLock::setPerformLA, close) }
          })
        }
      }
    }
  }
}

@Composable
private fun SapatEmptyHome(stopped: Boolean) {
  Column(
    Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = 80.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colors.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
      Icon(painterResource(MR.images.ic_chat_bubble), null, Modifier.size(44.dp), tint = MaterialTheme.colors.primary)
    }
    Spacer(Modifier.height(24.dp))
    Text(stringResource(MR.strings.sapat_empty_title), style = MaterialTheme.typography.h3.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colors.onBackground)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(MR.strings.sapat_empty_text), color = MaterialTheme.colors.secondary, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    Spacer(Modifier.height(28.dp))
    Button({ if (!stopped) openAddContact() }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp), elevation = null) {
      Text(stringResource(MR.strings.sapat_add_contact), color = Color.White, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton({ if (!stopped) openInviteLink() }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp)) {
      Text(stringResource(MR.strings.sapat_send_invite_link), color = MaterialTheme.colors.primary, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(4.dp))
    TextButton({ if (!stopped) openNewGroup() }) {
      Text(stringResource(MR.strings.sapat_new_group), color = MaterialTheme.colors.primary)
    }
  }
}

@Composable
private fun SapatSearchEmpty(q: String) {
  Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Spacer(Modifier.height(48.dp))
    Text(stringResource(MR.strings.sapat_search_none), color = MaterialTheme.colors.secondary)
    Spacer(Modifier.height(16.dp))
    TextButton({ openAddContact() }) { Text(String.format(stringResource(MR.strings.sapat_search_add_by_name), q.trim())) }
  }
}

private fun showSapatNewSheet() {
  ModalManager.start.closeModals()
  ModalManager.start.showModalCloseable { close ->
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
      AppBarTitle(stringResource(MR.strings.sapat_new))
      SapatNewItem(painterResource(MR.images.ic_person_add), stringResource(MR.strings.sapat_add_contact), stringResource(MR.strings.sapat_add_contact_descr)) { close(); openAddContact() }
      SapatNewItem(painterResource(MR.images.ic_link), stringResource(MR.strings.sapat_send_invite_link), stringResource(MR.strings.sapat_send_invite_link_descr)) { close(); openInviteLink() }
      SapatNewItem(painterResource(MR.images.ic_group), stringResource(MR.strings.sapat_new_group), stringResource(MR.strings.sapat_new_group_descr)) { close(); openNewGroup() }
    }
  }
}

@Composable
private fun SapatNewItem(icon: androidx.compose.ui.graphics.painter.Painter, title: String, descr: String, onClick: () -> Unit) {
  Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colors.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
      Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colors.primary)
    }
    Column(Modifier.padding(start = 16.dp)) {
      Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colors.onBackground)
      Text(descr, fontSize = 14.sp, color = MaterialTheme.colors.secondary)
    }
  }
}

private fun openAddContact() {
  val closeAll = { ModalManager.start.closeModals() }
  ModalManager.start.showModalCloseable { _ -> AddByNameView(chatModel.currentRemoteHost.value?.remoteHostId, close = closeAll) }
}

private fun openInviteLink() {
  val closeAll = { ModalManager.start.closeModals() }
  ModalManager.start.showModalCloseable { _ -> NewChatView(chatModel.currentRemoteHost.value, NewChatOption.INVITE, close = closeAll) }
}

private fun openNewGroup() {
  val closeAll = { ModalManager.start.closeModals() }
  ModalManager.start.showCustomModal { close -> AddGroupView(chatModel, chatModel.currentRemoteHost.value, close, closeAll) }
}

// One chat row: round photo, name and time, last message and the unread count. No dividers.
@Composable
fun SapatChatRow(chat: Chat, disabled: Boolean) {
  val cInfo = chat.chatInfo
  val last = chat.chatItems.lastOrNull()?.takeIf { it.content !is CIContent.ChatBanner }
  val unread = chat.chatStats.unreadCount
  val muted = !cInfo.ntfsEnabled(false) && (cInfo is ChatInfo.Direct || cInfo is ChatInfo.Group)
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    SapatAvatar(cInfo, 54.dp)
    Column(Modifier.weight(1f).padding(start = 14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          cInfo.chatViewName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
          fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
          color = if (disabled) MaterialTheme.colors.secondary else MaterialTheme.colors.onBackground
        )
        Text(
          getTimestampText(last?.meta?.itemTs ?: cInfo.chatTs),
          fontSize = 12.sp,
          color = if (unread > 0) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
        )
      }
      Spacer(Modifier.height(3.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        val preview = when {
          last == null -> ""
          last.meta.itemDeleted != null -> stringResource(MR.strings.sapat_deleted_message)
          last.chatDir.sent -> stringResource(MR.strings.sapat_you_prefix) + " " + last.text
          else -> last.text
        }
        Text(preview, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, color = MaterialTheme.colors.secondary)
        if (muted) {
          Icon(painterResource(MR.images.ic_notifications_off), null, Modifier.padding(start = 6.dp).size(16.dp), tint = MaterialTheme.colors.secondary)
        }
        if (unread > 0 || chat.chatStats.unreadChat) {
          Box(
            Modifier.padding(start = 8.dp).defaultMinSize(minWidth = 22.dp).height(22.dp).clip(RoundedCornerShape(11.dp))
              .background(if (muted) MaterialTheme.colors.secondary else MaterialTheme.colors.primary).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(if (unread > 0) unreadCountStr(unread) else "", color = if (sapatStyleB()) MaterialTheme.colors.onPrimary else Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

private val avatarLight = listOf(0xFFF5D9C8 to 0xFF8A4420, 0xFFD5EBDD to 0xFF2D6A45, 0xFFE6DDF3 to 0xFF5B3D8F, 0xFFEDE7D6 to 0xFF6B5A2E, 0xFFDCE3F5 to 0xFF2F4F9F, 0xFFF3D9E2 to 0xFF8F2D52)
private val avatarDark = listOf(0xFF4A3A2E to 0xFFF5D9C8, 0xFF24423A to 0xFFD5EBDD, 0xFF3A2E5C to 0xFFE6DDF3, 0xFF45402C to 0xFFEDE7D6, 0xFF2C3A66 to 0xFFDCE3F5, 0xFF4F2A3A to 0xFFF3D9E2)

private fun initials(name: String): String {
  val parts = name.trim().split(Regex("[\\s._-]+")).filter { it.isNotEmpty() }
  return when {
    parts.isEmpty() -> "?"
    parts.size == 1 -> parts[0].take(2)
    else -> "${parts[0].first()}${parts[1].first()}"
  }.uppercase()
}

@Composable
private fun avatarColors(name: String): Pair<Color, Color> {
  val list = if (isInDarkTheme()) avatarDark else avatarLight
  val (bg, fg) = list[Math.floorMod(name.hashCode(), list.size)]
  return Color(bg) to Color(fg)
}

@Composable
fun SapatInitials(name: String, size: androidx.compose.ui.unit.Dp) {
  val b = sapatStyleB()
  val (bg, fg) = if (b) Color(0xFF1F2124) to Color(0xFFE8E8E8) else avatarColors(name)
  Box(Modifier.size(size).clip(if (b) RoundedCornerShape(size * 0.3f) else CircleShape).background(bg), contentAlignment = Alignment.Center) {
    Text(initials(name), color = fg, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun SapatAvatar(cInfo: ChatInfo, size: androidx.compose.ui.unit.Dp) {
  when {
    cInfo.image != null -> ChatInfoImage(cInfo, size = size)
    cInfo is ChatInfo.Group && sapatStyleB() -> SapatInitials(cInfo.chatViewName, size)
    cInfo is ChatInfo.Group -> {
      val (bg, fg) = avatarColors(cInfo.chatViewName)
      Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(painterResource(MR.images.ic_group), null, Modifier.size(size * 0.46f), tint = fg)
      }
    }
    cInfo is ChatInfo.Direct -> SapatInitials(cInfo.chatViewName, size)
    else -> ChatInfoImage(cInfo, size = size)
  }
}

@Composable
private fun SapatTopBarB(chatModel: ChatModel, userPickerState: MutableStateFlow<AnimatedViewState>, search: MutableState<String>, stopped: Boolean) {
  Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(
        stringResource(MR.strings.sapat_messages), Modifier.weight(1f),
        fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colors.onBackground
      )
      val menu = remember { mutableStateOf(false) }
      Box {
        IconButton({ menu.value = true }) {
          Icon(painterResource(MR.images.ic_more_vert), null, tint = MaterialTheme.colors.onBackground)
        }
        DefaultDropdownMenu(menu) {
          ItemAction(stringResource(MR.strings.sapat_profiles), painterResource(MR.images.ic_person_add), onClick = { menu.value = false; userPickerState.value = AnimatedViewState.VISIBLE })
          ItemAction(stringResource(MR.strings.sapat_name_and_code), painterResource(MR.images.ic_person_add), onClick = {
            menu.value = false
            ModalManager.start.showModalCloseable { _ -> SapatIdentityView() }
          })
          ItemAction(stringResource(MR.strings.settings_section_title_settings), painterResource(MR.images.ic_settings), onClick = {
            menu.value = false
            ModalManager.start.showModalCloseable(cardScreen = true) { close -> SettingsView(chatModel, AppLock::setPerformLA, close) }
          })
        }
      }
      Box(
        Modifier.padding(start = 4.dp).size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colors.primary)
          .clickable { if (!stopped) showSapatNewSheet() },
        contentAlignment = Alignment.Center
      ) {
        Icon(painterResource(MR.images.ic_add), stringResource(MR.strings.sapat_new), Modifier.size(26.dp), tint = MaterialTheme.colors.onPrimary)
      }
    }
    Row(
      Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp))
        .border(1.dp, MaterialTheme.colors.secondaryVariant, RoundedCornerShape(12.dp))
        .background(MaterialTheme.colors.surface).padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(painterResource(MR.images.ic_search), null, Modifier.size(20.dp), tint = MaterialTheme.colors.secondary)
      Box(Modifier.weight(1f).padding(start = 10.dp)) {
        if (search.value.isEmpty()) Text(stringResource(MR.strings.sapat_search_hint), color = MaterialTheme.colors.secondary, fontSize = 15.sp)
        BasicTextField(
          search.value, { search.value = it }, Modifier.fillMaxWidth(), singleLine = true,
          textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onBackground, fontSize = 15.sp),
          cursorBrush = SolidColor(MaterialTheme.colors.primary)
        )
      }
    }
    Spacer(Modifier.height(8.dp))
  }
}
