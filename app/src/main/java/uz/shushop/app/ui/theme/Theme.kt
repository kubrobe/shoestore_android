package uz.shushop.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The colours of Shushop: black and white like the design, one blue for "unread", one red for sale. */
object Palette {
    val Ink = Color(0xFF111113)          // text, buttons, the bottom pill
    val Paper = Color(0xFFFFFFFF)
    val Mist = Color(0xFFF3F3F5)         // shoe tiles, inputs
    val Fog = Color(0xFFE4E4E8)          // lines
    val Ash = Color(0xFF8A8A92)          // quiet text
    val Slate = Color(0xFF55555C)        // normal secondary text
    val Blue = Color(0xFF2F6BFF)         // the unread dot
    val Sale = Color(0xFFD64545)
    val Good = Color(0xFF2E8B57)

    // The notification list is a dark screen in the design
    val Night = Color(0xFF0E0E10)
    val NightCard = Color(0xFF1A1A1D)
    val NightLine = Color(0xFF2B2B30)
    val NightMuted = Color(0xFF9B9BA4)
}

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun ShushopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Ink,
            onPrimary = Palette.Paper,
            secondary = Palette.Ink,
            onSecondary = Palette.Paper,
            background = Palette.Paper,
            onBackground = Palette.Ink,
            surface = Palette.Paper,
            onSurface = Palette.Ink,
            surfaceVariant = Palette.Mist,
            onSurfaceVariant = Palette.Slate,
            outline = Palette.Fog,
            error = Palette.Sale,
        ),
        typography = AppTypography,
        content = content,
    )
}
