package chat.simplex.common.ui.theme

import androidx.compose.material.Typography as MaterialTypography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import chat.simplex.common.platform.appPreferences

// Shapat: the chat screen looks like a terminal — Commodore 64 colours in the dark theme,
// a paper terminal in the light theme, monospace text in both.

private val C64Blue = Color(0xFF352879)
private val C64BlueDark = Color(0xFF2B2068)
private val C64Text = Color(0xFFB8B0F5)
private val C64TextDim = Color(0xFFA49BE8)
private val C64Bright = Color(0xFFE4E0FF)

private val PaperBg = Color(0xFFF4F1E8)
private val PaperSurface = Color(0xFFEAE6DA)
private val PaperText = Color(0xFF1E1E1E)
private val PaperDim = Color(0xFF6B6B6B)
private val PaperAccent = Color(0xFF2F4F9F)

fun retroTheme(theme: ThemeManager.ActiveTheme): ThemeManager.ActiveTheme {
  val dark = !theme.colors.isLight
  val colors = if (appPreferences.sapatStyle.get() == "T") NordColorPalette else if (dark) darkColors(
    primary = C64Bright,
    primaryVariant = C64Text,
    secondary = C64TextDim,
    secondaryVariant = C64BlueDark,
    background = C64Blue,
    surface = C64BlueDark,
    error = Color(0xFFFF8A80),
    onBackground = C64Text,
    onSurface = C64Text,
    onPrimary = C64Blue,
    onSecondary = C64Blue,
    onError = C64Blue,
  ) else lightColors(
    primary = PaperAccent,
    primaryVariant = PaperAccent,
    secondary = PaperDim,
    secondaryVariant = PaperSurface,
    background = PaperBg,
    surface = PaperSurface,
    error = Color(0xFFB3261E),
    onBackground = PaperText,
    onSurface = PaperText,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onError = Color.White,
  )
  val bg = colors.background
  val appColors = AppColors(
    title = colors.primary,
    primaryVariant2 = colors.primaryVariant,
    sentMessage = bg,
    sentQuote = colors.surface,
    receivedMessage = bg,
    receivedQuote = colors.surface,
  )
  return theme.copy(colors = colors, appColors = appColors, wallpaper = AppWallpaper(background = bg))
}

private val mono = FontFamily.Monospace

val RetroTypography = MaterialTypography(
  h1 = Typography.h1.copy(fontFamily = mono),
  h2 = Typography.h2.copy(fontFamily = mono),
  h3 = Typography.h3.copy(fontFamily = mono),
  h4 = Typography.h4.copy(fontFamily = mono),
  h5 = Typography.h5.copy(fontFamily = mono),
  h6 = Typography.h6.copy(fontFamily = mono),
  subtitle1 = Typography.subtitle1.copy(fontFamily = mono),
  subtitle2 = Typography.subtitle2.copy(fontFamily = mono),
  body1 = Typography.body1.copy(fontFamily = mono),
  body2 = Typography.body2.copy(fontFamily = mono),
  button = Typography.button.copy(fontFamily = mono),
  caption = Typography.caption.copy(fontFamily = mono),
  overline = Typography.overline.copy(fontFamily = mono),
)
