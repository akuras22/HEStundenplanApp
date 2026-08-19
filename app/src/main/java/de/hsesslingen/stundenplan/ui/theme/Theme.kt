package de.hsesslingen.stundenplan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import de.hsesslingen.stundenplan.data.AccentPreset

/** The app's actually-resolved dark/light state (after applying the user's ThemeMode override),
 *  for descendants that need it but can't just call isSystemInDarkTheme() — that only reflects the
 *  OS setting, not the app's own override (see MainActivity). */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

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

/** Re-tints a base scheme's accent-related colors to [accent], optionally overriding
 *  [background]/surface too — used for accent presets and the fully custom option. Neutral colors
 *  (nav bar, text, dividers) are left as-is so the app keeps its One UI look regardless of accent. */
private fun tintedColorScheme(darkTheme: Boolean, accent: Color, background: Color?): ColorScheme {
    val base = if (darkTheme) OneUiDarkColorScheme else OneUiLightColorScheme
    val onAccent = if (accent.luminance() > 0.5f) Color(0xFF00214D) else Color.White
    val containerBase = if (darkTheme) Color.Black else Color.White
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent.copy(alpha = 0.24f).compositeOver(containerBase),
        secondary = accent,
        surfaceTint = accent,
        background = background ?: base.background,
        surface = background ?: base.surface,
    )
}

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
    accentPreset: AccentPreset = AccentPreset.DEFAULT,
    customAccentColor: Color? = null,
    customBackgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        accentPreset == AccentPreset.CUSTOM ->
            tintedColorScheme(darkTheme, customAccentColor ?: OneUiBlue, customBackgroundColor)
        accentPreset != AccentPreset.DEFAULT ->
            tintedColorScheme(darkTheme, accentPreset.color ?: OneUiBlue, null)
        darkTheme -> OneUiDarkColorScheme
        else -> OneUiLightColorScheme
    }
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colorScheme, shapes = OneUiShapes, content = content)
    }
}
