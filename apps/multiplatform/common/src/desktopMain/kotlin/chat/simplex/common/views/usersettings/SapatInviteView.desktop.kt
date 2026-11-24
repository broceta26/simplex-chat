package chat.simplex.common.views.usersettings

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
actual fun SapatInviteContactsView(inviteText: String) {
  Text(stringResource(MR.strings.sapat_invite_desktop), Modifier.padding(DEFAULT_PADDING), color = MaterialTheme.colors.secondary)
}
