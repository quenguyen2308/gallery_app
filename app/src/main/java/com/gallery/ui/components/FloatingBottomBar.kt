package com.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nav bar bottom height captured at the root (GalleryApp) before any Scaffold can consume the
 * insets. FloatingBottomBar reads from here instead of WindowInsets.navigationBars so all pill
 * bars sit at the exact same Y regardless of how many nested Scaffolds wrap them.
 */
val LocalNavBarBottom = compositionLocalOf<Dp> { 0.dp }

// ── Màu sắc pill bar ──────────────────────────────────────────────────────────
/** Màu nền pill (thanh điều hướng nổi). */
private val PillBg = Color(0xFFE8E5E0)

/** Màu nền oval của tab đang active. */
private val ActiveTabBg = Color(0xFFCDC9C4)

/** Màu icon / chữ khi tab được chọn. */
private val ActiveColor = Color(0xFF3A3C40)

/** Màu icon / chữ khi tab chưa được chọn. */
private val InactiveColor = Color(0xFF8E8E94)

// ── Kích thước icon ───────────────────────────────────────────────────────────
private val NavIconSize = 22.dp
private val ActionIconSize = 20.dp

/**
 * Khoảng trống bottom cần thêm vào nội dung có thể scroll để hàng cuối
 * không bị khuất sau pill bar.
 */
val FloatingBottomBarClearance = 80.dp

/**
 * Pill nổi dùng chung cho nav bar lẫn action bar chọn media.
 * Không dùng `bottomBar` của Scaffold — nội dung scroll qua sau pill thay vì bị cắt ngắn.
 */
@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val navBarBottom = LocalNavBarBottom.current
    Surface(
        modifier = modifier.padding(bottom = navBarBottom + 36.dp),
        shape = RoundedCornerShape(50),
        color = PillBg.copy(alpha = 0.96f),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * Tab điều hướng — icon + nhãn; tab đang chọn có oval highlight phía sau.
 * Dùng `weight(1f)` nên bar cần có chiều rộng xác định từ phía caller.
 */
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
        label = "navColor",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) ActiveTabBg else Color.Transparent,
        animationSpec = tween(250),
        label = "navBg",
    )

    Box(
        modifier = Modifier
            .padding(vertical = 5.dp, horizontal = 10.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(NavIconSize),
        )
    }
}

/** Nút action icon-only cho action bar chọn media / viewer. */
@Composable
fun RowScope.PillActionItem(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = ActiveColor,
) {
    Box(
        modifier = Modifier
            .padding(vertical = 5.dp, horizontal = 5.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(ActionIconSize),
        )
    }
}
