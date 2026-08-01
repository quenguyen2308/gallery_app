package com.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// The bar itself is always a rice-milk (sữa gạo) translucent capsule regardless of system theme,
// so icon tints are fixed dark tones for contrast against that light cream rather than switching
// with light/dark theme.
// Pill bar is a few shades darker than the rice-milk screen background (0xFFF3E8D2) so it
// visually floats rather than blending into the page.
private val PillTone = Color(0xFFE5CEAB)
private val ActiveColor = Color(0xFF3A3A38)
private val InactiveColor = Color(0xFF8C8572)

// Active tab gets a translucent dark-gray overlay pill; inactive tabs have no fill.
private val IconBackgroundInactive = Color.Transparent
private val IconBackgroundActive = Color(0x26000000)

/**
 * FloatingBottomBar is rendered as an overlay (not a Scaffold `bottomBar` slot) so scrollable
 * content isn't clipped short and can be scrolled fully behind/above it. Screens that host a
 * scrollable list under the bar should add this as extra bottom content padding so the last row
 * can still be scrolled clear of the bar instead of staying stuck behind it.
 */
val FloatingBottomBarClearance = 96.dp

// Pill icon size reduced to ~75% of the original 22dp.
private val IconSize = 17.dp

/**
 * Fully transparent pill shared by every bottom bar in the app (nav, selection, editor tools):
 * floats above content with margin on all sides, no background fill and no shadow — a
 * Material3 Surface still renders its shadow shape even at alpha 0, so shadowElevation must
 * stay at 0 here or the "invisible" pill shows up as a solid grey capsule anyway.
 */
@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 20.dp),
        shape = RoundedCornerShape(50),
        color = PillTone.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Icon-only tab; active tab gets its own pill-in-pill highlight. `label` is used as the a11y description. */
@Composable
fun RowScope.PillNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) ActiveColor else InactiveColor,
        animationSpec = tween(200),
        label = "pillItemColor",
    )
    val iconBackground by animateColorAsState(
        targetValue = if (selected) IconBackgroundActive else IconBackgroundInactive,
        animationSpec = tween(200),
        label = "pillItemBackground",
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(IconSize * 2)
                .height(IconSize)
                .clip(RoundedCornerShape(50))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(IconSize))
        }
    }
}

/** Icon-only action button (share/delete/move/...) for selection-mode action bars. */
@Composable
fun RowScope.PillActionItem(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(IconSize * 2)
                .height(IconSize)
                .clip(RoundedCornerShape(50))
                .background(IconBackgroundActive),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = ActiveColor,
                modifier = Modifier.size(IconSize),
            )
        }
    }
}
