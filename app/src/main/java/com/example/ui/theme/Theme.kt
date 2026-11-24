package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.domain.ThemeAccent

@Immutable
data class MeenaGlowPalette(
    val primaryGlow: Color = NeonViolet,
    val secondaryGlow: Color = SoftBlue,
    val deepBackdrop: Color = CosmicBlack,
    val cardGlass: Color = GlassSurface,
    val cardBorder: Color = GlassBorder
)

val LocalMeenaGlow = staticCompositionLocalOf { MeenaGlowPalette() }

val MeenaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MeenaAiProTheme(
    themeAccent: ThemeAccent = ThemeAccent.COSMIC_VIOLET,
    content: @Composable () -> Unit
) {
    val primaryColor = when (themeAccent) {
        ThemeAccent.COSMIC_VIOLET -> NeonViolet
        ThemeAccent.CYBER_BLUE -> SoftBlue
        ThemeAccent.ROYAL_ROSE -> RoyalRose
        ThemeAccent.AURORA_TEAL -> AuroraTeal
    }
    val secondaryColor = when (themeAccent) {
        ThemeAccent.COSMIC_VIOLET -> SoftBlue
        ThemeAccent.CYBER_BLUE -> NeonViolet
        ThemeAccent.ROYAL_ROSE -> BrightViolet
        ThemeAccent.AURORA_TEAL -> ElectricBlue
    }

    val colorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = DeepPurple,
        onPrimaryContainer = BrightViolet,
        secondary = secondaryColor,
        onSecondary = CosmicBlack,
        secondaryContainer = GlassSurfaceElevated,
        onSecondaryContainer = TextPrimary,
        tertiary = NeonCyan,
        onTertiary = CosmicBlack,
        background = CosmicBlack,
        onBackground = TextPrimary,
        surface = MidnightVoid,
        onSurface = TextPrimary,
        surfaceVariant = GlassSurface,
        onSurfaceVariant = TextSecondary,
        outline = primaryColor.copy(alpha = 0.35f),
        outlineVariant = GlassBorderSubtle,
        error = ErrorRed,
        onError = CosmicBlack
    )

    val glowPalette = MeenaGlowPalette(
        primaryGlow = primaryColor,
        secondaryGlow = secondaryColor,
        deepBackdrop = CosmicBlack,
        cardGlass = GlassSurface,
        cardBorder = primaryColor.copy(alpha = 0.32f)
    )

    CompositionLocalProvider(LocalMeenaGlow provides glowPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MeenaTypography,
            shapes = MeenaShapes,
            content = content
        )
    }
}
