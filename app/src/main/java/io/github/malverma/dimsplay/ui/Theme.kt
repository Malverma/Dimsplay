package io.github.malverma.dimsplay.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Color tokens from docs/design/DESIGN_SPEC.md. The app is dark only. */
object DimColors {
    val Bg = Color(0xFF0E0F13)
    val Surface = Color(0xFF16181D)
    val Track = Color(0xFF262931)
    val Text = Color(0xFFF2F3F5)
    val Muted = Color(0xFF8A8F98)
    val Faint = Color(0xFF5E636D)
    val Accent = Color(0xFFA78BFA)
    val White = Color(0xFFFFFFFF)
}

object DimType {
    val Title = TextStyle(fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold, color = DimColors.Text)
    val Subtitle = TextStyle(fontSize = 14.sp, lineHeight = 17.sp, color = DimColors.Muted)
    val RingValue = TextStyle(fontSize = 64.sp, lineHeight = 77.sp, fontWeight = FontWeight.Bold, color = DimColors.Text)
    val RingCaption = TextStyle(
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        color = DimColors.Muted,
    )
    val CardLabel = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = DimColors.Text)
    val CardValue = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = DimColors.Accent)
    val CardCaption = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, color = DimColors.Muted)
    val RangeLabel = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, color = DimColors.Faint)
    val Button = TextStyle(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, color = DimColors.Bg)
}

val CardShape = RoundedCornerShape(20.dp)

@Composable
fun DimsplayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DimColors.Accent,
            onPrimary = DimColors.Bg,
            background = DimColors.Bg,
            onBackground = DimColors.Text,
            surface = DimColors.Surface,
            onSurface = DimColors.Text,
            onSurfaceVariant = DimColors.Muted,
        ),
        content = content,
    )
}
