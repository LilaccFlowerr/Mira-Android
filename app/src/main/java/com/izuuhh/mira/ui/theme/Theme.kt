package com.izuuhh.mira.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The palette from the desktop app's Theme.qml. */
data class MiraColors(
    val base: Color, val panel: Color, val elevated: Color, val hover: Color,
    val text: Color, val muted: Color, val primary: Color, val onPrimary: Color,
    val container: Color, val outline: Color, val error: Color, val dark: Boolean,
)

val LocalMira = staticCompositionLocalOf<MiraColors> { error("MiraTheme missing") }

object Mira {
    val colors: MiraColors @Composable get() = LocalMira.current
}

private fun c(hex: Long) = Color(0xFF000000 or hex)

fun miraColors(dark: Boolean, accent: Int) = MiraColors(
    base = c(if (dark) 0x111015 else 0xeee9f1),
    panel = c(if (dark) 0x1b191f else 0xfaf7fc),
    elevated = c(if (dark) 0x252229 else 0xeee8f2),
    hover = c(if (dark) 0x302b35 else 0xe4dce9),
    text = c(if (dark) 0xeee7f0 else 0x211b26),
    muted = c(if (dark) 0xa99fad else 0x716675),
    primary = c(when (accent) { 1 -> if (dark) 0xacd5bd else 0x34684d; 2 -> if (dark) 0xefbaa2 else 0x885239; else -> if (dark) 0xd0bcff else 0x6b4ea0 }),
    onPrimary = c(if (dark) 0x2e223b else 0xffffff),
    container = c(when (accent) { 1 -> if (dark) 0x2d4437 else 0xd6eddd; 2 -> if (dark) 0x4a352b else 0xf8ded1; else -> if (dark) 0x3e3056 else 0xe8dcfb }),
    outline = c(if (dark) 0x3d3643 else 0xd6cbdc),
    error = c(if (dark) 0xffb4ab else 0x9c2929),
    dark = dark,
)

@Composable
fun MiraTheme(dark: Boolean, accent: Int, content: @Composable () -> Unit) {
    val m = miraColors(dark, accent)
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = m.primary, onPrimary = m.onPrimary,
        primaryContainer = m.container, onPrimaryContainer = m.text,
        background = m.base, onBackground = m.text,
        surface = m.panel, onSurface = m.text, onSurfaceVariant = m.muted,
        surfaceVariant = m.elevated, surfaceContainer = m.elevated, surfaceContainerHigh = m.elevated,
        outline = m.outline, outlineVariant = m.outline, error = m.error,
    )
    CompositionLocalProvider(LocalMira provides m) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
