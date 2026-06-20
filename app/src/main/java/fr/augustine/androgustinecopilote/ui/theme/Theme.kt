package fr.augustine.androgustinecopilote.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ShellOrange,
    onPrimary = Color.White,
    primaryContainer = ShellOrange,
    onPrimaryContainer = Color.White,
    secondary = ShellOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2B1A12),
    onSecondaryContainer = Color.White,
    tertiary = RacingCyan,
    onTertiary = NavyBlueBackground,
    background = NavyBlueBackground,
    onBackground = Color.White,
    surface = NavyBlueBackground,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF102638),
    onSurfaceVariant = Color.White,
    surfaceContainer = Color(0xFF071B2B),
    error = DangerRed,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = ShellOrange,
    onPrimary = Color.White,
    primaryContainer = ShellOrange,
    onPrimaryContainer = Color.White,
    secondary = ShellOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2B1A12),
    onSecondaryContainer = Color.White,
    tertiary = RacingCyan,
    onTertiary = NavyBlueBackground,
    background = NavyBlueBackground,
    onBackground = Color.White,
    surface = NavyBlueBackground,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF102638),
    onSurfaceVariant = Color.White,
    surfaceContainer = Color(0xFF071B2B),
    error = DangerRed,
    onError = Color.White,
)

@Composable
fun AndroGustineCopiloteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
