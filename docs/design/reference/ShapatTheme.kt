// REFERENCA, ne gotov kod. Preuzmi vrednosti iz tokens.json i ugradi ih u POSTOJEĆI sistem tema aplikacije
// (proveri kako repo već definiše teme i prilagođene boje pre nego što išta dodaješ).
// Paket i nazivi su pretpostavka.
package chat.simplex.common.ui.theme

import androidx.compose.ui.graphics.Color

data class ShapatPalette(
    val bg: Color, val surface: Color,
    val incomingBubble: Color, val onIncoming: Color,
    val outgoingBubble: Color, val onOutgoing: Color,
    val onBg: Color, val muted: Color,
    val accent: Color, val onAccent: Color,
    val divider: Color, val danger: Color,
)

val ShapatGold = ShapatPalette(
    bg = Color(0xFF0B1B3A), surface = Color(0xFF12285A),
    incomingBubble = Color(0xFF1B3470), onIncoming = Color(0xFFF1ECE0),
    outgoingBubble = Color(0xFFD9A93C), onOutgoing = Color(0xFF0B1B3A),
    onBg = Color(0xFFF1ECE0), muted = Color(0xFF8C9CC0),
    accent = Color(0xFFD9A93C), onAccent = Color(0xFF0B1B3A),
    divider = Color(0xFF2A3E6B), danger = Color(0xFFFF6B6B),
)

val ShapatTmux = ShapatPalette(
    bg = Color(0xFF2E3440), surface = Color(0xFF3B4252),
    incomingBubble = Color(0xFF2E3440), onIncoming = Color(0xFFD8DEE9), // nema balona, samo linije
    outgoingBubble = Color(0xFF2E3440), onOutgoing = Color(0xFFA3BE8C),
    onBg = Color(0xFFD8DEE9), muted = Color(0xFFA9B3CA),
    accent = Color(0xFF88C0D0), onAccent = Color(0xFF2E3440),
    divider = Color(0xFF4C566A), danger = Color(0xFFE58A93),
)
