package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable

class SapatInviteLabels(
  val title: String, val permission: String, val grant: String, val none: String,
  val sent: String, val send: String, val next: (Int) -> String
)

// Shapat: pick people from the phone's contacts and send each an invite with our name and code. Android only.
@Composable
expect fun SapatInviteContactsView(inviteText: String, labels: SapatInviteLabels)
