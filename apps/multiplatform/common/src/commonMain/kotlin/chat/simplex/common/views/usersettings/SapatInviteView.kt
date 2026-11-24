package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable

// Shapat: pick people from the phone's contacts and send each an invite with our name and code. Android only.
@Composable
expect fun SapatInviteContactsView(inviteText: String)
