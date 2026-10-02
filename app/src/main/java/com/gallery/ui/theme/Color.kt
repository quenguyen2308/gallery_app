package com.gallery.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Mockup 1: Pastel Blossom (Soft Rose & Blush Pink) ───────────────────────
val SoftRoseSeed = Color(0xFFFF8DA1)
val SoftRoseBackground = Color(0xFFFFF7F9)
val SoftRoseSurface = Color(0xFFFFFFFF)
val SoftRoseSurfaceVariant = Color(0xFFFFF0F3)
val SoftRosePrimary = Color(0xFFFF6B8B)
val SoftRosePillBg = Color(0xF5FFFFFF)
val SoftRoseActiveTabBg = Color(0xFFFFEBF0)
val SoftRoseActiveColor = Color(0xFFD64D6C)
val SoftRoseInactiveColor = Color(0xFF9E8F94)
val SoftRoseHeart = Color(0xFFFF3D68)
val SoftRoseHeartInactive = Color(0xFFFF8DA1)
val SoftRoseCardBorder = Color(0x35FFB3C6)
val SoftRoseSelection = Color(0xFFFF6B8B)
val SoftRoseChipBg = Color(0xFFFFF0F3)
val SoftRoseChipBorder = Color(0xFFF5DCE3)

val ScrimColor = Color(0xCCFFFFFF)

@Immutable
data class ExtendedGalleryColors(
    val pillBackground: Color,
    val activeTabBackground: Color,
    val activeContentColor: Color,
    val inactiveContentColor: Color,
    val heartColor: Color,
    val selectionColor: Color,
    val chipBackground: Color,
    val chipActiveBackground: Color,
    val chipActiveContent: Color,
    val chipBorder: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedGalleryColors(
        pillBackground = SoftRosePillBg,
        activeTabBackground = SoftRoseActiveTabBg,
        activeContentColor = SoftRoseActiveColor,
        inactiveContentColor = SoftRoseInactiveColor,
        heartColor = SoftRoseHeart,
        selectionColor = SoftRoseSelection,
        chipBackground = SoftRoseChipBg,
        chipActiveBackground = SoftRosePrimary,
        chipActiveContent = Color.White,
        chipBorder = SoftRoseChipBorder,
    )
}

val SelectionOverlay: Color
    @Composable get() = LocalExtendedColors.current.selectionColor
