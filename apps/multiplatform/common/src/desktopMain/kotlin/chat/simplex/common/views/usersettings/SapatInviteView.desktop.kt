package chat.simplex.common.views.usersettings

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.simplex.common.ui.theme.DEFAULT_PADDING

@Composable
actual fun SapatInviteContactsView(inviteText: String, labels: SapatInviteLabels) {
  Text(labels.none, Modifier.padding(DEFAULT_PADDING), color = MaterialTheme.colors.secondary)
}
