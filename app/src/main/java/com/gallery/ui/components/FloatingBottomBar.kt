package com.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

val LocalNavBarBottom = compositionLocalOf<Dp> { 0.dp }
val LocalHazeState = compositionLocalOf<HazeState?> { null }

val FloatingBottomBarClearance = 110.dp

/**
 * Floating Pill Dock — Slim, High-Transparency Frosted Mist (Trong Suốt Mờ Sương Siêu Gọn)
 * Highly translucent glassmorphism dock with optical backdrop blur, sleek height,
 * and delicate active frosted capsule.
 */
@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalHazeState.current,
    content: @Composable RowScope.() -> Unit,
) {
    val navBarBottom = LocalNavBarBottom.current

    val dockBlurModifier = if (hazeState != null) {
        Modifier.hazeChild(
            state = hazeState,
            style = HazeDefaults.style(
                backgroundColor = Color.White.copy(alpha = 0.12f),
                blurRadius = 24.dp,
                noiseFactor = 0.03f,
            ),
        )
    } else {
        Modifier.background(Color.White.copy(alpha = 0.25f), CircleShape)
    }

    Box(
        modifier = modifier
            .padding(bottom = navBarBottom + 12.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color(0x25000000),
                spotColor = Color(0x20000000),
            )
            .clip(CircleShape)
            .then(dockBlurModifier)
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.30f),
                        )
                    )
                ),
                CircleShape,
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * Navigation item:
 * - Active: Compact frosted capsule (alpha 0.32), salmon-rose icon & label, and tight centered dot indicator.
 * - Inactive: Clean dark slate icon & label directly on translucent frosted glass.
 */
@Composable
fun RowScope.PillNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val roseColor = Color(0xFFE27E8F)
    val inactiveColor = Color(0xFF363236)

    val contentColor by animateColorAsState(
        targetValue = if (selected) roseColor else inactiveColor,
        animationSpec = tween(180),
        label = "navColor",
    )
    val activeBgColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.32f) else Color.Transparent,
        animationSpec = tween(180),
        label = "activeBgColor",
    )
    val activeBorderColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.65f) else Color.Transparent,
        animationSpec = tween(180),
        label = "activeBorderColor",
    )

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x15000000),
                        spotColor = Color(0x10000000),
                    )
                } else Modifier
            )
            .background(activeBgColor)
            .border(BorderStroke(1.dp, activeBorderColor), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                color = contentColor,
                style = TextStyle(
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.1.sp,
                    lineHeight = 11.sp,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    ),
                ),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(1.dp))
            // Delicate dot indicator nestled closely under label
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(if (selected) roseColor else Color.Transparent),
            )
        }
    }
}

/** Action button for media selection action bar & image viewer dock. */
@Composable
fun RowScope.PillActionItem(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    val effectiveTint = tint ?: Color(0xFFE27E8F)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(22.dp),
        )
    }
}
