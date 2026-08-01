package com.gallery.ui.photos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gallery.R
import com.gallery.domain.model.MediaItem
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.FloatingBottomBarClearance
import com.gallery.ui.components.MediaSelectionScaffold
import com.gallery.ui.theme.ThumbnailShape
import com.gallery.util.MediaDateGroup
import com.gallery.util.groupMediaByDate
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    viewModel: GalleryViewModel,
    onOpenViewer: (mediaId: Long, viewerList: List<MediaItem>) -> Unit,
) {
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val groups = remember(mediaItems) { groupMediaByDate(mediaItems, context) }

    MediaSelectionScaffold(
        viewModel = viewModel,
        items = mediaItems,
        title = stringResource(R.string.nav_photos),
    ) { padding ->
        if (mediaItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_photos))
            }
            return@MediaSelectionScaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 6.dp, bottom = FloatingBottomBarClearance),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        ) {
            groups.forEach { group ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "header_${group.date}") {
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
                        onClick = {
                            if (selectionMode) {
                                viewModel.toggleSelected(item.id)
                            } else {
                                onOpenViewer(item.id, mediaItems)
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isFullySelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = stringResource(R.string.action_select_all),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onToggleGroup),
                tint = if (isFullySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
        }
        Text(text = group.label, style = MaterialTheme.typography.titleLarge)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.photo_count, group.items.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.88f else 1f,
        animationSpec = tween(150),
        label = "thumbnailScale",
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(ThumbnailShape)
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
                .clip(ThumbnailShape),
        )
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (selectionMode) {
            SelectionDot(
                isSelected = isSelected,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
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
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f),
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
                tint = MaterialTheme.colorScheme.onPrimary,
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
