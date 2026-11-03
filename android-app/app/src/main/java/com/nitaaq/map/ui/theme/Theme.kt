package com.nitaaq.map.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class ThemeMode { System, Light, Dark }

private val LightColors = lightColorScheme(
    primary = Brand.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1EAFF),
    onPrimaryContainer = Color(0xFF0B2A78),
    secondary = Brand.GreenDeep,
    onSecondary = Color.White,
    secondaryContainer = LightTokens.successContainer,
    onSecondaryContainer = Color(0xFF0B3D1E),
    tertiary = Color(0xFF0F766E),
    background = LightTokens.background,
    onBackground = LightTokens.onSurface,
    surface = LightTokens.surface,
    onSurface = LightTokens.onSurface,
    surfaceVariant = LightTokens.surfaceContainer,
    onSurfaceVariant = LightTokens.onSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightTokens.surfaceContainerLow,
    surfaceContainer = LightTokens.surfaceContainer,
    surfaceContainerHigh = LightTokens.surfaceContainerHigh,
    surfaceContainerHighest = Color(0xFFDCE5F0),
    outline = LightTokens.outline,
    outlineVariant = LightTokens.outlineVariant,
    error = LightTokens.error,
    errorContainer = LightTokens.errorContainer,
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6E9BFF),
    onPrimary = Color(0xFF041A4D),
    primaryContainer = Color(0xFF173A86),
    onPrimaryContainer = Color(0xFFDDE6FF),
    secondary = DarkTokens.success,
    onSecondary = Color(0xFF052E14),
    secondaryContainer = DarkTokens.successContainer,
    onSecondaryContainer = Color(0xFFC9F7D8),
    tertiary = Brand.Teal,
    background = DarkTokens.background,
    onBackground = DarkTokens.onSurface,
    surface = DarkTokens.surface,
    onSurface = DarkTokens.onSurface,
    surfaceVariant = DarkTokens.surfaceContainer,
    onSurfaceVariant = DarkTokens.onSurfaceVariant,
    surfaceContainerLowest = Color(0xFF08131F),
    surfaceContainerLow = DarkTokens.surfaceContainerLow,
    surfaceContainer = DarkTokens.surfaceContainer,
    surfaceContainerHigh = DarkTokens.surfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF26445F),
    outline = DarkTokens.outline,
    outlineVariant = DarkTokens.outlineVariant,
    error = DarkTokens.error,
    errorContainer = DarkTokens.errorContainer,
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Tokens Material 3 has no role for. */
@Immutable
data class NitaaqColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    /** The brand gradient. Used in exactly two places: the primary CTA and the countdown bar. */
    val brandGradient: Brush,
    val brandGradientColors: List<Color>,
    /** QR modules are always dark-on-white for scanner reliability, in both themes. */
    val qrInk: Color,
    val qrPaper: Color,
    val isDark: Boolean,
)

private val gradientColors = listOf(Brand.Blue, Brand.Green)

private val LightExtended = NitaaqColors(
    success = LightTokens.success,
    successContainer = LightTokens.successContainer,
    warning = LightTokens.warning,
    warningContainer = LightTokens.warningContainer,
    brandGradient = Brush.linearGradient(gradientColors),
    brandGradientColors = gradientColors,
    qrInk = Brand.Navy,
    qrPaper = Color.White,
    isDark = false,
)

private val DarkExtended = LightExtended.copy(
    success = DarkTokens.success,
    successContainer = DarkTokens.successContainer,
    warning = DarkTokens.warning,
    warningContainer = DarkTokens.warningContainer,
    isDark = true,
)

val LocalNitaaqColors = staticCompositionLocalOf { LightExtended }

/** True when the user disabled animations (Developer options / Accessibility "Remove animations"). */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun NitaaqTheme(
    themeMode: ThemeMode = ThemeMode.System,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    CompositionLocalProvider(
        LocalNitaaqColors provides if (dark) DarkExtended else LightExtended,
        LocalReducedMotion provides reducedMotion,
        // The product is Arabic-first: keep RTL even on devices set to another language.
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = NitaaqTypography,
            shapes = NitaaqShapes,
            content = content,
        )
    }
}

object NitaaqTheme {
    val colors: NitaaqColors
        @Composable get() = LocalNitaaqColors.current
}
