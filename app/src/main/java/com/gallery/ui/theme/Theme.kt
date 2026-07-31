package com.gallery.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.TonalPalette

/**
 * Device dynamic color (Android 12+) is opt-in, not the default: it ties the whole app's palette
 * to whatever the current wallpaper happens to produce, which on a plain/neutral wallpaper reads
 * as flat and washed out. Defaulting to our own seed (via the same HCT algorithm Material You
 * uses) keeps the app looking intentional regardless of device wallpaper.
 */
@Composable
fun GalleryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDeviceDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        useDeviceDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> remember(darkTheme) { colorSchemeFromSeed(BrandSeed.toArgb(), darkTheme) }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GalleryTypography,
        shapes = GalleryShapes,
        content = content,
    )
}

@SuppressLint("RestrictedApi")
private fun colorSchemeFromSeed(seedArgb: Int, isDark: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seedArgb), isDark, 0.0)
    return buildColorScheme(
        p = scheme.primaryPalette,
        s = scheme.secondaryPalette,
        t = scheme.tertiaryPalette,
        n = scheme.neutralPalette,
        nv = scheme.neutralVariantPalette,
        e = scheme.errorPalette,
        isDark = isDark,
    )
}

@SuppressLint("RestrictedApi")
private fun buildColorScheme(
    p: TonalPalette,
    s: TonalPalette,
    t: TonalPalette,
    n: TonalPalette,
    nv: TonalPalette,
    e: TonalPalette,
    isDark: Boolean,
): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = Color(p.tone(80)),
        onPrimary = Color(p.tone(20)),
        primaryContainer = Color(p.tone(30)),
        onPrimaryContainer = Color(p.tone(90)),
        inversePrimary = Color(p.tone(40)),
        secondary = Color(s.tone(80)),
        onSecondary = Color(s.tone(20)),
        secondaryContainer = Color(s.tone(30)),
        onSecondaryContainer = Color(s.tone(90)),
        tertiary = Color(t.tone(80)),
        onTertiary = Color(t.tone(20)),
        tertiaryContainer = Color(t.tone(30)),
        onTertiaryContainer = Color(t.tone(90)),
        background = Color(n.tone(6)),
        onBackground = Color(n.tone(90)),
        surface = Color(n.tone(6)),
        onSurface = Color(n.tone(90)),
        surfaceVariant = Color(nv.tone(30)),
        onSurfaceVariant = Color(nv.tone(80)),
        surfaceTint = Color(p.tone(80)),
        inverseSurface = Color(n.tone(90)),
        inverseOnSurface = Color(n.tone(20)),
        error = Color(e.tone(80)),
        onError = Color(e.tone(20)),
        errorContainer = Color(e.tone(30)),
        onErrorContainer = Color(e.tone(90)),
        outline = Color(nv.tone(60)),
        outlineVariant = Color(nv.tone(30)),
        scrim = Color(n.tone(0)),
        surfaceBright = Color(n.tone(24)),
        surfaceDim = Color(n.tone(6)),
        surfaceContainer = Color(n.tone(12)),
        surfaceContainerHigh = Color(n.tone(17)),
        surfaceContainerHighest = Color(n.tone(22)),
        surfaceContainerLow = Color(n.tone(10)),
        surfaceContainerLowest = Color(n.tone(4)),
    )
} else {
    lightColorScheme(
        primary = Color(p.tone(40)),
        onPrimary = Color(p.tone(100)),
        primaryContainer = Color(p.tone(90)),
        onPrimaryContainer = Color(p.tone(10)),
        inversePrimary = Color(p.tone(80)),
        secondary = Color(s.tone(40)),
        onSecondary = Color(s.tone(100)),
        secondaryContainer = Color(s.tone(90)),
        onSecondaryContainer = Color(s.tone(10)),
        tertiary = Color(t.tone(40)),
        onTertiary = Color(t.tone(100)),
        tertiaryContainer = Color(t.tone(90)),
        onTertiaryContainer = Color(t.tone(10)),
        background = Color(n.tone(99)),
        onBackground = Color(n.tone(10)),
        surface = Color(n.tone(99)),
        onSurface = Color(n.tone(10)),
        surfaceVariant = Color(nv.tone(90)),
        onSurfaceVariant = Color(nv.tone(30)),
        surfaceTint = Color(p.tone(40)),
        inverseSurface = Color(n.tone(20)),
        inverseOnSurface = Color(n.tone(95)),
        error = Color(e.tone(40)),
        onError = Color(e.tone(100)),
        errorContainer = Color(e.tone(90)),
        onErrorContainer = Color(e.tone(10)),
        outline = Color(nv.tone(50)),
        outlineVariant = Color(nv.tone(80)),
        scrim = Color(n.tone(0)),
        surfaceBright = Color(n.tone(98)),
        surfaceDim = Color(n.tone(87)),
        surfaceContainer = Color(n.tone(94)),
        surfaceContainerHigh = Color(n.tone(92)),
        surfaceContainerHighest = Color(n.tone(90)),
        surfaceContainerLow = Color(n.tone(96)),
        surfaceContainerLowest = Color(n.tone(100)),
    )
}
