package chat.simplex.common.views.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.sapatStyleB

// Šapat building blocks that follow the chosen style: graphite cards and amber buttons in style B, rounded classic ones in style A.

private val bRadius = RoundedCornerShape(12.dp)
private val aRadius = RoundedCornerShape(24.dp)

@Composable
fun SapatPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
  val b = sapatStyleB()
  Button(
    onClick, modifier.fillMaxWidth().height(if (b) 52.dp else 48.dp), enabled = enabled,
    shape = if (b) bRadius else aRadius, elevation = null,
    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary, contentColor = MaterialTheme.colors.onPrimary)
  ) {
    Text(text, fontWeight = if (b) FontWeight.SemiBold else FontWeight.Medium, fontSize = 16.sp)
  }
}

@Composable
fun SapatSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
  val b = sapatStyleB()
  OutlinedButton(
    onClick, modifier.height(if (b) 46.dp else 44.dp), enabled = enabled,
    shape = if (b) bRadius else aRadius,
    border = androidx.compose.foundation.BorderStroke(1.dp, if (b) MaterialTheme.colors.secondaryVariant else MaterialTheme.colors.primary.copy(alpha = 0.5f)),
    colors = ButtonDefaults.outlinedButtonColors(
      backgroundColor = if (b) MaterialTheme.colors.background else androidx.compose.ui.graphics.Color.Transparent,
      contentColor = if (b) MaterialTheme.colors.onBackground else MaterialTheme.colors.primary
    )
  ) {
    Text(text, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun SapatCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  val b = sapatStyleB()
  Column(
    modifier.fillMaxWidth().clip(RoundedCornerShape(if (b) 14.dp else 18.dp))
      .background(if (b) MaterialTheme.colors.surface else MaterialTheme.colors.primary.copy(alpha = 0.07f))
      .then(if (b) Modifier.border(1.dp, MaterialTheme.colors.secondaryVariant, RoundedCornerShape(14.dp)) else Modifier)
      .padding(16.dp),
    content = content
  )
}

// A small label above a value: monospace capitals in style B, plain secondary text in style A.
@Composable
fun SapatLabel(text: String, modifier: Modifier = Modifier) {
  if (sapatStyleB()) {
    Text(text.uppercase(), modifier, color = MaterialTheme.colors.secondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.6.sp)
  } else {
    Text(text, modifier, color = MaterialTheme.colors.secondary, fontSize = 14.sp)
  }
}

@Composable
fun SapatCodeText(code: String) {
  val b = sapatStyleB()
  Text(
    code, fontFamily = FontFamily.Monospace, fontSize = if (b) 26.sp else 28.sp, letterSpacing = if (b) 2.sp else 0.sp,
    fontWeight = if (b) FontWeight.Medium else FontWeight.Bold,
    color = if (b) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
  )
}

@Composable
fun sapatFieldColors(): TextFieldColors =
  if (sapatStyleB()) TextFieldDefaults.outlinedTextFieldColors(
    backgroundColor = MaterialTheme.colors.surface,
    unfocusedBorderColor = MaterialTheme.colors.secondaryVariant,
    focusedBorderColor = MaterialTheme.colors.primary,
    cursorColor = MaterialTheme.colors.primary,
    focusedLabelColor = MaterialTheme.colors.primary,
  ) else TextFieldDefaults.outlinedTextFieldColors()

fun sapatFieldShape(): androidx.compose.ui.graphics.Shape = if (sapatStyleB()) RoundedCornerShape(12.dp) else RoundedCornerShape(4.dp)
