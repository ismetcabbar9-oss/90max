package com.ninetymax.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ninetymax.app.data.Area
import com.ninetymax.app.data.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFF1F5EFF), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE4FF), onPrimaryContainer = Color(0xFF001257),
    secondary = Color(0xFF0E8C7A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBF2EA), onSecondaryContainer = Color(0xFF00201B),
    tertiary = Color(0xFFB4582A), tertiaryContainer = Color(0xFFFFDBCB),
    background = Color(0xFFF7F8FB), onBackground = Color(0xFF14161C),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF14161C),
    surfaceVariant = Color(0xFFEDEFF5), onSurfaceVariant = Color(0xFF4A4F5C),
    surfaceContainer = Color(0xFFF1F3F8), surfaceContainerHigh = Color(0xFFEAEDF4),
    outline = Color(0xFFC4C8D3), outlineVariant = Color(0xFFDDE0E8),
    error = Color(0xFFBA1A1A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9FB4FF), onPrimary = Color(0xFF00218B),
    primaryContainer = Color(0xFF1B3FB8), onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFF6FD8C4), onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005145), onSecondaryContainer = Color(0xFFCBF2EA),
    tertiary = Color(0xFFFFB592), tertiaryContainer = Color(0xFF6E2F0B),
    background = Color(0xFF0E1015), onBackground = Color(0xFFE4E6EE),
    surface = Color(0xFF15181F), onSurface = Color(0xFFE4E6EE),
    surfaceVariant = Color(0xFF242833), onSurfaceVariant = Color(0xFFB9BECB),
    surfaceContainer = Color(0xFF1A1D25), surfaceContainerHigh = Color(0xFF22262F),
    outline = Color(0xFF5A606E), outlineVariant = Color(0xFF343946),
    error = Color(0xFFFFB4AB),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp),
)

@Composable
fun MaxTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, shapes = AppShapes, content = content)
}

fun areaColor(area: Area): Color = when (area) {
    Area.SKIN -> Color(0xFFE08A5E)
    Area.HAIR -> Color(0xFF9C6ADE)
    Area.BEARD -> Color(0xFF8D6E63)
    Area.ORAL -> Color(0xFF29A8D8)
    Area.BODY -> Color(0xFF26A69A)
    Area.POSTURE -> Color(0xFF43A047)
    Area.SLEEP -> Color(0xFF5C6BC0)
    Area.NUTRITION -> Color(0xFFF4A300)
    Area.SUPPLEMENT -> Color(0xFFD81B60)
    Area.WORKOUT -> Color(0xFF1F5EFF)
    Area.STYLE -> Color(0xFF546E7A)
    Area.GENERAL -> Color(0xFF78909C)
}

val TitleStyle: TextStyle @Composable get() = MaterialTheme.typography.titleMedium
