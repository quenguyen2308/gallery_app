package com.gallery.ui.photos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.gallery.ui.components.LocalHazeState
import com.gallery.ui.components.MediaSelectionScaffold
import com.gallery.ui.theme.FilterChipShape
import com.gallery.ui.theme.LocalExtendedColors
import com.gallery.ui.theme.SelectionOverlay
import com.gallery.ui.theme.SoftRoseCardBorder
import com.gallery.ui.theme.SoftRoseHeart
import com.gallery.ui.theme.SoftRosePrimary
import com.gallery.util.MediaDateGroup
import com.gallery.util.groupMediaByDate
import dev.chrisbanes.haze.haze
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

    val context = LocalContext.current
    val groups = remember(filteredItems) {
        groupMediaByDate(filteredItems, context)
    }
    val hazeState = LocalHazeState.current

    MediaSelectionScaffold(
        viewModel = viewModel,
        items = filteredItems,
        customTopBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            ) {
                // Top Header Row: Title & Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.nav_photos),
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-0.6).sp,
                        ),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Quick Select Button (Fluid Glass pill)
                        if (!selectionMode && filteredItems.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, SoftRoseCardBorder),
                                modifier = Modifier.clickable {
                                    viewModel.enterSelection(filteredItems.first().id)
                                },
                            ) {
                                Text(
                                    text = stringResource(R.string.action_select),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = SoftRosePrimary,
                                        fontSize = 12.5.sp,
                                    ),
                                )
                            }
                        }

                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search & Filter",
                                tint = if (showFilters) SoftRosePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
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
            val gridHazeModifier = if (hazeState != null) {
                Modifier.haze(hazeState)
            } else Modifier

            // Fluid Edge-to-Edge Photo Wall Grid (tight 2dp spacing, 4 columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .then(gridHazeModifier),
                contentPadding = PaddingValues(
                    start = 2.dp,
                    top = 2.dp,
                    end = 2.dp,
                    bottom = FloatingBottomBarClearance,
                ),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                groups.forEach { group ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_${group.key}") {
                        DateGroupHeader(
                            group = group,
                            selectionMode = selectionMode,
                            isFullySelected = selectedIds.containsAll(group.items.map { it.id }),
                            onToggleGroup = { viewModel.toggleGroupSelection(group.items.map { it.id }) },
                        )
                    }
                    items(group.items, key = { it.id }) { item ->
                        PhotoThumbnail(
                            item = item,
                            isSelected = item.id in selectedIds,
                            selectionMode = selectionMode,
                            cornerRadius = 4.dp,
                            showDurationText = true,
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateGroupHeader(
    group: MediaDateGroup,
    selectionMode: Boolean,
    isFullySelected: Boolean,
    onToggleGroup: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selectionMode) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleGroup)
                } else Modifier
            )
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 10.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isFullySelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = stringResource(R.string.action_select_all),
                modifier = Modifier.size(22.dp),
                tint = if (isFullySelected) SelectionOverlay else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = group.label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.weight(1f))

        group.subLabel?.let { sub ->
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    cornerRadius: androidx.compose.ui.unit.Dp = 4.dp,
    showDurationText: Boolean = true,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.88f else 1f,
        animationSpec = tween(150),
        label = "thumbnailScale",
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(cornerRadius))
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

        // Video Duration / Play Badge
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                if (showDurationText) {
                    Text(
                        text = formatDuration(item.durationMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                    )
                }
            }
        }

        // Discrete Favorite Badge
        if (item.isFavorite && !selectionMode) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFE05C5C),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(14.dp),
            )
        }

        // Selection Radio Circle Indicator
        if (selectionMode) {
            SelectionDot(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            )
        }
    }
}

@Composable
fun SelectionDot(isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
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
                modifier = Modifier.size(14.dp),
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
