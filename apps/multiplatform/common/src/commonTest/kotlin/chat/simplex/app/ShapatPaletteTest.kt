package chat.simplex.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import chat.simplex.common.ui.theme.*
import kotlin.test.Test
import kotlin.test.assertTrue

class ShapatPaletteTest {
  private fun contrast(a: Color, b: Color): Float {
    val l1 = maxOf(a.luminance(), b.luminance())
    val l2 = minOf(a.luminance(), b.luminance())
    return (l1 + 0.05f) / (l2 + 0.05f)
  }

  private fun check(name: String, pairs: List<Triple<String, Color, Color>>) =
    pairs.forEach { (what, fg, bg) -> assertTrue(contrast(fg, bg) >= 4.5f, "$name $what contrast ${contrast(fg, bg)}") }

  @Test
  fun goldTextIsReadable() {
    val c = GoldColorPalette
    val a = GoldColorPaletteApp
    check("gold", listOf(
      Triple("text on background", c.onBackground, c.background),
      Triple("muted on background", c.secondary, c.background),
      Triple("error on background", c.error, c.background),
      Triple("text on received", c.onBackground, a.receivedMessage),
      Triple("on accent", c.onPrimary, c.primary),
      Triple("on sent", c.onPrimary, a.sentMessage),
    ))
  }

  @Test
  fun nordTextIsReadable() {
    val c = NordColorPalette
    val a = NordColorPaletteApp
    check("nord", listOf(
      Triple("text on background", c.onBackground, c.background),
      Triple("muted on background", c.secondary, c.background),
      Triple("error on background", c.error, c.background),
      Triple("accent on background", c.primary, c.background),
      Triple("text on sent", c.onBackground, a.sentMessage),
      Triple("muted on surface", c.secondary, c.surface),
    ))
  }
}
