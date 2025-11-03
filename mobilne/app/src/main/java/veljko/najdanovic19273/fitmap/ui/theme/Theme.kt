package veljko.najdanovic19273.fitmap.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = SteelBlue,
    tertiary = GoldLight,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = TextPrimary,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = DarkCard,
    onPrimaryContainer = TextPrimary,
    secondaryContainer = DeepBlue,
    onSecondaryContainer = TextPrimary,
    error = AccentRed,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = GoldDark,
    secondary = SteelBlue,
    tertiary = GoldPrimary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    primaryContainer = LightCard,
    onPrimaryContainer = Color.Black,
    secondaryContainer = Color(0xFFE8EAF6),
    onSecondaryContainer = Color.Black,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun FitMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color je onemogućen za konzistentan profesionalan izgled
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}