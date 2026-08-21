package opb.myniceapp.dint.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import opb.myniceapp.dint.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5D50),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7EBDD),
    onPrimaryContainer = Color(0xFF14382E),
    secondary = Color(0xFF7C5C2E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7E3BF),
    onSecondaryContainer = Color(0xFF402A07),
    tertiary = Color(0xFF6D566E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4D9F1),
    onTertiaryContainer = Color(0xFF281629),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF1D1B18),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1D1B18),
    surfaceVariant = Color(0xFFE7E1D6),
    onSurfaceVariant = Color(0xFF4B463E),
    outline = Color(0xFF7D766C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBBD0C0),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF16483A),
    onPrimaryContainer = Color(0xFFD7EBDD),
    secondary = Color(0xFFE8C98E),
    onSecondary = Color(0xFF422C03),
    secondaryContainer = Color(0xFF5D4317),
    onSecondaryContainer = Color(0xFFF7E3BF),
    tertiary = Color(0xFFD8BCD6),
    onTertiary = Color(0xFF3D2940),
    tertiaryContainer = Color(0xFF543F56),
    onTertiaryContainer = Color(0xFFF4D9F1),
    background = Color(0xFF141311),
    onBackground = Color(0xFFE8E2DA),
    surface = Color(0xFF1B1916),
    onSurface = Color(0xFFE8E2DA),
    surfaceVariant = Color(0xFF4B463E),
    onSurfaceVariant = Color(0xFFD0C8BC),
    outline = Color(0xFF989082)
)

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val displayFontFamily = FontFamily(
    Font(GoogleFont("Fraunces"), googleFontProvider)
)

// Display font is scoped to display*/headlineLarge/headlineMedium only — the two places a
// one-off headline appears (HeroCard titles, the intervention PromptTitle). headlineSmall and
// everything smaller/more frequent (stat values, chip labels, list items) stay on the system
// font so a downloadable-font load never flickers visible, high-churn text.
val AppTypography: Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = displayFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = displayFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = displayFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = displayFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = displayFontFamily),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
