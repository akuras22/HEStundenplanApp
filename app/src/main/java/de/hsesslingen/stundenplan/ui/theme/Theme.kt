package de.hsesslingen.stundenplan.ui.theme

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

// One UI leans on a vivid, single accent blue and near-true-black surfaces in dark mode.
private val OneUiBlue = Color(0xFF0381FE)
private val OneUiBlueLight = Color(0xFF4DA3FF)

private val OneUiDarkColorScheme = darkColorScheme(
    primary = OneUiBlueLight,
    onPrimary = Color(0xFF00214D),
    primaryContainer = Color(0xFF0A3B78),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFB8C7DB),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF1C1F24),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF282828),
)

private val OneUiLightColorScheme = lightColorScheme(
    primary = OneUiBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6FF),
    onPrimaryContainer = Color(0xFF001B3E),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEBEEF2),
    surfaceContainer = Color(0xFFF1F3F6),
    surfaceContainerHigh = Color(0xFFE9ECF1),
    surfaceContainerHighest = Color(0xFFE1E5EB),
)

@Composable
fun StundenplanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Samsung's own first-party apps (Wallet, Phone, Gallery, …) don't tint their neutral
    // surfaces/chrome by wallpaper — bottom-nav bars, dividers etc. stay a fixed, achromatic gray
    // regardless of Material You. Defaulting to Android's wallpaper-tinted dynamic color here was
    // exactly why the bottom nav's "neutral gray" highlight never quite matched Samsung's own
    // (it was picking up a color cast from the device wallpaper); OneUiDarkColorScheme/
    // OneUiLightColorScheme below are the actual neutral One UI palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> OneUiDarkColorScheme
        else -> OneUiLightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, shapes = OneUiShapes, content = content)
}
