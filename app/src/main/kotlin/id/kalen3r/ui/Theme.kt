package id.kalen3r.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object KalenColors {
    val Teal = Color(0xFF0F5257)
    val Gold = Color(0xFFE0A21B)
    val Cream = Color(0xFFFBF6EC)
    val Ink = Color(0xFF2B2118)
}

private val KalenLightColors = lightColorScheme(
    primary = KalenColors.Teal,
    onPrimary = KalenColors.Cream,
    secondary = KalenColors.Gold,
    onSecondary = KalenColors.Ink,
    background = KalenColors.Cream,
    onBackground = KalenColors.Ink,
    surface = KalenColors.Cream,
    onSurface = KalenColors.Ink,
)

/** Light-only theme (teal, gold, cream, ink). */
@Composable
fun KalenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KalenLightColors, content = content)
}
