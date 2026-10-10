package com.izuuhh.mira.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Mira's palette: surfaces, text and one accent. Everything is derived from a single hue. */
data class MiraColors(
    val base: Color, val panel: Color, val elevated: Color, val hover: Color,
    val text: Color, val muted: Color, val primary: Color, val onPrimary: Color,
    val container: Color, val outline: Color, val error: Color, val dark: Boolean,
)

val LocalMira = staticCompositionLocalOf<MiraColors> { error("MiraTheme missing") }
val LocalCornerScale = staticCompositionLocalOf { 1f }
val LocalWavy = staticCompositionLocalOf { true }

/** A corner radius scaled by the user's "corners" setting. */
@Composable
@ReadOnlyComposable
fun corner(radius: Dp): Dp = radius * LocalCornerScale.current

object Mira {
    val colors: MiraColors @Composable get() = LocalMira.current
}

/** Ready-made accents: name to hue. */
val AccentPresets = listOf(
    "Violet" to 262, "Blue" to 212, "Teal" to 175, "Green" to 148,
    "Yellow" to 46, "Peach" to 18, "Rose" to 340,
)

val dynamicColorAvailable get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun miraColors(dark: Boolean, hue: Int, amoled: Boolean): MiraColors {
    val h = hue.toFloat()
    fun c(s: Float, l: Float) = Color.hsl(h, s, l)
    return if (dark) MiraColors(
        base = if (amoled) Color.Black else c(0.10f, 0.07f),
        panel = if (amoled) c(0.08f, 0.045f) else c(0.09f, 0.105f),
        elevated = if (amoled) c(0.10f, 0.09f) else c(0.10f, 0.145f),
        hover = c(0.11f, 0.19f),
        text = c(0.25f, 0.92f), muted = c(0.10f, 0.67f),
        primary = c(0.62f, 0.82f), onPrimary = c(0.35f, 0.17f),
        container = c(0.30f, 0.26f), outline = c(0.12f, 0.25f),
        error = Color(0xFFFFB4AB), dark = true,
    ) else MiraColors(
        base = c(0.22f, 0.93f), panel = c(0.45f, 0.98f), elevated = c(0.26f, 0.94f), hover = c(0.24f, 0.90f),
        text = c(0.28f, 0.12f), muted = c(0.10f, 0.45f),
        primary = c(0.40f, 0.40f), onPrimary = Color.White,
        container = c(0.62f, 0.91f), outline = c(0.20f, 0.84f),
        error = Color(0xFF9C2929), dark = false,
    )
}

@Composable
fun MiraTheme(
    dark: Boolean, hue: Int, amoled: Boolean, dynamic: Boolean,
    corners: Int, font: Int, textSize: Int, wavy: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val m = if (dynamic && dynamicColorAvailable) {
        val s = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        MiraColors(
            base = if (dark && amoled) Color.Black else s.background, panel = s.surfaceContainerLow,
            elevated = s.surfaceContainerHigh, hover = s.surfaceContainerHighest,
            text = s.onSurface, muted = s.onSurfaceVariant, primary = s.primary, onPrimary = s.onPrimary,
            container = s.primaryContainer, outline = s.outlineVariant, error = s.error, dark = dark,
        )
    } else miraColors(dark, hue, amoled)
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = m.primary, onPrimary = m.onPrimary,
        primaryContainer = m.container, onPrimaryContainer = m.text,
        background = m.base, onBackground = m.text,
        surface = m.panel, onSurface = m.text, onSurfaceVariant = m.muted,
        surfaceVariant = m.elevated, surfaceContainer = m.elevated, surfaceContainerHigh = m.elevated,
        outline = m.outline, outlineVariant = m.outline, error = m.error,
    )
    val family = when (font) { 1 -> FontFamily.Serif; 2 -> FontFamily.Monospace; else -> FontFamily.Default }
    val typography = Typography().let { t ->
        t.copy(
            displayLarge = t.displayLarge.copy(fontFamily = family), displayMedium = t.displayMedium.copy(fontFamily = family),
            displaySmall = t.displaySmall.copy(fontFamily = family), headlineLarge = t.headlineLarge.copy(fontFamily = family),
            headlineMedium = t.headlineMedium.copy(fontFamily = family), headlineSmall = t.headlineSmall.copy(fontFamily = family),
            titleLarge = t.titleLarge.copy(fontFamily = family), titleMedium = t.titleMedium.copy(fontFamily = family),
            titleSmall = t.titleSmall.copy(fontFamily = family), bodyLarge = t.bodyLarge.copy(fontFamily = family),
            bodyMedium = t.bodyMedium.copy(fontFamily = family), bodySmall = t.bodySmall.copy(fontFamily = family),
            labelLarge = t.labelLarge.copy(fontFamily = family), labelMedium = t.labelMedium.copy(fontFamily = family),
            labelSmall = t.labelSmall.copy(fontFamily = family),
        )
    }
    val density = LocalDensity.current
    val scaledDensity = Density(density.density, density.fontScale * when (textSize) { 0 -> 0.9f; 2 -> 1.15f; else -> 1f })
    CompositionLocalProvider(
        LocalMira provides m,
        LocalCornerScale provides when (corners) { 0 -> 0.3f; 2 -> 1.5f; else -> 1f },
        LocalWavy provides wavy,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            ProvideTextStyle(TextStyle(fontFamily = family), content)
        }
    }
}
