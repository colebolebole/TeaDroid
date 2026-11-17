package site.colebolebole.ezject.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Fallback palette (used when dynamic color is off / unavailable): a lime-green seed in the Gitea spirit.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF3B6A1E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBF396),
    onPrimaryContainer = Color(0xFF082100),
    secondary = Color(0xFF55624C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E7CB),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF386666),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBBEBEC),
    onTertiaryContainer = Color(0xFF002020),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8FAF0),
    onBackground = Color(0xFF191D16),
    surface = Color(0xFFF8FAF0),
    onSurface = Color(0xFF191D16),
    surfaceVariant = Color(0xFFDFE4D7),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796E),
    outlineVariant = Color(0xFFC3C8BB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5EA),
    surfaceContainer = Color(0xFFECEFE4),
    surfaceContainerHigh = Color(0xFFE6E9DE),
    surfaceContainerHighest = Color(0xFFE1E4D9),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA0D77C),
    onPrimary = Color(0xFF123800),
    primaryContainer = Color(0xFF235100),
    onPrimaryContainer = Color(0xFFBBF396),
    secondary = Color(0xFFBCCBB0),
    onSecondary = Color(0xFF273421),
    secondaryContainer = Color(0xFF3D4B36),
    onSecondaryContainer = Color(0xFFD8E7CB),
    tertiary = Color(0xFFA0CFCF),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF1E4E4E),
    onTertiaryContainer = Color(0xFFBBEBEC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF11140E),
    onBackground = Color(0xFFE1E4D9),
    surface = Color(0xFF11140E),
    onSurface = Color(0xFFE1E4D9),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BB),
    outline = Color(0xFF8D9387),
    outlineVariant = Color(0xFF43483F),
    surfaceContainerLowest = Color(0xFF0C0F09),
    surfaceContainerLow = Color(0xFF191D16),
    surfaceContainer = Color(0xFF1D211A),
    surfaceContainerHigh = Color(0xFF282C24),
    surfaceContainerHighest = Color(0xFF33362E),
)

@Composable
fun EZTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography(),
        content = content,
    )
}
