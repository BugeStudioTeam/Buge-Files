package com.buge.files

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private data class Seed(val primary: Color, val secondary: Color, val tertiary: Color)

private val palette = mapOf(
    ColorSource.INDIGO to Seed(Color(0xFF5146BF), Color(0xFF5D5A72), Color(0xFF8A3B69)),
    ColorSource.OCEAN to Seed(Color(0xFF006782), Color(0xFF4D626C), Color(0xFF526600)),
    ColorSource.FOREST to Seed(Color(0xFF176C42), Color(0xFF4E6355), Color(0xFF38656F)),
    ColorSource.SUNSET to Seed(Color(0xFF9C4300), Color(0xFF755946), Color(0xFF755172)),
    ColorSource.ORCHID to Seed(Color(0xFF8047A0), Color(0xFF6A5B6D), Color(0xFF81515C))
)

private fun lightScheme(seed: Seed): ColorScheme = lightColorScheme(
    primary = seed.primary,
    onPrimary = Color.White,
    primaryContainer = seed.primary.copy(alpha = .16f).compositeOver(Color(0xFFFFFFFF)),
    onPrimaryContainer = Color(0xFF20104D),
    secondary = seed.secondary,
    onSecondary = Color.White,
    secondaryContainer = seed.secondary.copy(alpha = .14f).compositeOver(Color.White),
    onSecondaryContainer = Color(0xFF171B25),
    tertiary = seed.tertiary,
    onTertiary = Color.White,
    tertiaryContainer = seed.tertiary.copy(alpha = .15f).compositeOver(Color.White),
    onTertiaryContainer = Color(0xFF31111F),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCF9FC),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFCF9FC),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE9E7EE),
    onSurfaceVariant = Color(0xFF47464F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F3F8),
    surfaceContainer = Color(0xFFF0EDF2),
    surfaceContainerHigh = Color(0xFFEAE7EC),
    surfaceContainerHighest = Color(0xFFE4E1E7),
    outline = Color(0xFF77747D),
    outlineVariant = Color(0xFFC8C4CD)
)

private fun darkScheme(seed: Seed): ColorScheme = darkColorScheme(
    primary = seed.primary.copy(alpha = .72f).compositeOver(Color.White),
    onPrimary = Color(0xFF251C57),
    primaryContainer = seed.primary.copy(alpha = .6f).compositeOver(Color(0xFF121114)),
    onPrimaryContainer = Color(0xFFE6DDFF),
    secondary = seed.secondary.copy(alpha = .70f).compositeOver(Color.White),
    onSecondary = Color(0xFF25302A),
    secondaryContainer = Color(0xFF3A4840),
    onSecondaryContainer = Color(0xFFD7E8D9),
    tertiary = seed.tertiary.copy(alpha = .72f).compositeOver(Color.White),
    onTertiary = Color(0xFF401A30),
    tertiaryContainer = Color(0xFF66364E),
    onTertiaryContainer = Color(0xFFFFD9E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141216),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF141216),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF48464F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0F0D10),
    surfaceContainerLow = Color(0xFF1D1B1F),
    surfaceContainer = Color(0xFF211F23),
    surfaceContainerHigh = Color(0xFF2B292D),
    surfaceContainerHighest = Color(0xFF353337),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454F)
)

private val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex_regular, FontWeight.Normal),
    Font(R.font.google_sans_flex_medium, FontWeight.Medium),
    Font(R.font.google_sans_flex_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_flex_bold, FontWeight.Bold)
)

private val BugeTypography = Typography(
    displaySmall = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
)

@Composable
fun BugeTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.theme) {
        ThemePreference.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val colorScheme = if (settings.colorSource == ColorSource.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val seed = palette[settings.colorSource] ?: palette.getValue(ColorSource.INDIGO)
        if (dark) darkScheme(seed) else lightScheme(seed)
    }
    MaterialTheme(colorScheme = colorScheme, typography = BugeTypography, content = content)
}
