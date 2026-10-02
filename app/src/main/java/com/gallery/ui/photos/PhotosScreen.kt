package com.gallery.ui.photos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.gallery.ui.theme.SoftRosePrimary
import com.gallery.ui.theme.SoftRoseHeart
import com.gallery.ui.theme.SoftRoseHeartInactive
import com.gallery.ui.theme.SoftRoseCardBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gallery.R
import com.gallery.domain.model.MediaItem
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.FloatingBottomBarClearance
import com.gallery.ui.components.MediaSelectionScaffold
import com.gallery.ui.theme.FilterChipShape
import com.gallery.ui.theme.LocalExtendedColors
import com.gallery.ui.theme.SelectionOverlay
import com.gallery.ui.theme.ThumbnailShape
import com.gallery.util.MediaDateGroup
import com.gallery.util.groupMediaByDate
import java.util.concurrent.TimeUnit

enum class PhotoFilterCategory(val labelRes: Int) {
    ALL(R.string.filter_all),
    PHOTOS(R.string.filter_photos),
    VIDEOS(R.string.filter_videos),
    FAVORITES(R.string.filter_favorites),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    viewModel: GalleryViewModel,
    onOpenViewer: (mediaId: Long, viewerList: List<MediaItem>) -> Unit,
    onOpenFavorites: () -> Unit = {},
) {
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()

    var selectedFilter by rememberSaveable { mutableStateOf(PhotoFilterCategory.ALL) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val filteredItems = remember(mediaItems, selectedFilter) {
        when (selectedFilter) {
            PhotoFilterCategory.ALL -> mediaItems
            PhotoFilterCategory.PHOTOS -> mediaItems.filter { !it.isVideo }
            PhotoFilterCategory.VIDEOS -> mediaItems.filter { it.isVideo }
            PhotoFilterCategory.FAVORITES -> mediaItems.filter { it.isFavorite }
        }
    }

    MediaSelectionScaffold(
        viewModel = viewModel,
        items = filteredItems,
        customTopBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "My Memories",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            color = Color(0xFF2E2428),
                            letterSpacing = (-0.5).sp,
                        ),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search & Filter",
                                tint = if (showFilters) SoftRosePrimary else Color(0xFF4A3E42),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        IconButton(onClick = onOpenFavorites) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = stringResource(R.string.favorites_title),
                                tint = SoftRoseHeart,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                // Smooth slide-down filter chips when Search/Filter is activated
                androidx.compose.animation.AnimatedVisibility(
                    visible = showFilters,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val extendedColors = LocalExtendedColors.current
                        PhotoFilterCategory.values().forEach { category ->
                            val isSelected = category == selectedFilter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = category },
                                label = {
                                    Text(
                                        text = stringResource(category.labelRes),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                leadingIcon = if (category == PhotoFilterCategory.FAVORITES) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSelected) extendedColors.chipActiveContent else extendedColors.heartColor,
                                        )
                                    }
                                } else null,
                                shape = FilterChipShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = extendedColors.chipBackground,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = extendedColors.chipActiveBackground,
                                    selectedLabelColor = extendedColors.chipActiveContent,
                                ),
                                border = null,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (mediaItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_photos), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_photos),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 14.dp, top = 6.dp, end = 14.dp, bottom = FloatingBottomBarClearance),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ── Hero Section (Mockup 1 Signature: 2 large portrait cards side by side) ──
                if (filteredItems.size >= 2) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "featured_hero_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PhotoThumbnail(
                                item = filteredItems[0],
                                isSelected = filteredItems[0].id in selectedIds,
                                selectionMode = selectionMode,
                                aspectRatio = 0.82f,
                                cornerRadius = 24.dp,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (selectionMode) {
                                        viewModel.toggleSelected(filteredItems[0].id)
                                    } else {
                                        onOpenViewer(filteredItems[0].id, filteredItems)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) viewModel.enterSelection(filteredItems[0].id)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(filteredItems[0].id) },
                            )
                            PhotoThumbnail(
                                item = filteredItems[1],
                                isSelected = filteredItems[1].id in selectedIds,
                                selectionMode = selectionMode,
                                aspectRatio = 0.82f,
                                cornerRadius = 24.dp,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (selectionMode) {
                                        viewModel.toggleSelected(filteredItems[1].id)
                                    } else {
                                        onOpenViewer(filteredItems[1].id, filteredItems)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) viewModel.enterSelection(filteredItems[1].id)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(filteredItems[1].id) },
                            )
                        }
                    }
                }

                // ── Remaining Photos in 3-column rounded card grid ─────────────────────
                val gridPhotos = if (filteredItems.size >= 2) filteredItems.drop(2) else filteredItems
                items(gridPhotos, key = { it.id }) { item ->
                    PhotoThumbnail(
                        item = item,
                        isSelected = item.id in selectedIds,
                        selectionMode = selectionMode,
                        aspectRatio = 1f,
                        cornerRadius = 18.dp,
                        onClick = {
                            if (selectionMode) {
                                viewModel.toggleSelected(item.id)
                            } else {
                                onOpenViewer(item.id, filteredItems)
                            }
                        },
                        onLongClick = {
                            if (!selectionMode) viewModel.enterSelection(item.id)
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoThumbnail(
    item: MediaItem,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f,
    cornerRadius: androidx.compose.ui.unit.Dp = 18.dp,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.88f else 1f,
        animationSpec = tween(150),
        label = "thumbnailScale",
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(cornerRadius))
            .border(androidx.compose.foundation.BorderStroke(1.dp, com.gallery.ui.theme.SoftRoseCardBorder), RoundedCornerShape(cornerRadius))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!selectionMode) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    onLongClick()
                },
            ),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(RoundedCornerShape(cornerRadius)),
        )

        // Video Duration Badge
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                )
            }
        }

        // Iconic Frosted Circular Heart Badge (Mockup 1 signature)
        if (!selectionMode) {
            val badgePadding = if (cornerRadius > 20.dp) 10.dp else 6.dp
            val badgeSize = if (cornerRadius > 20.dp) 32.dp else 26.dp
            val iconSize = if (cornerRadius > 20.dp) 16.dp else 13.dp

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(badgePadding)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f))
                    .clickable(
                        enabled = onToggleFavorite != null,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onToggleFavorite?.invoke()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Toggle favorite",
                    tint = if (item.isFavorite) com.gallery.ui.theme.SoftRoseHeart else com.gallery.ui.theme.SoftRoseHeartInactive.copy(alpha = 0.85f),
                    modifier = Modifier.size(iconSize),
                )
            }
        }

        // Selection Dot
        if (selectionMode) {
            SelectionDot(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
fun SelectionDot(isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = if (isSelected) SelectionOverlay else Color.Black.copy(alpha = 0.35f),
                shape = CircleShape,
            )
            .border(
                width = 1.5.dp,
                color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.9f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
