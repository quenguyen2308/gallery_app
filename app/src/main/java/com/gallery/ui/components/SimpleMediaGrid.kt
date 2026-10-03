package com.gallery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gallery.domain.model.MediaItem
import com.gallery.ui.photos.PhotoThumbnail

import dev.chrisbanes.haze.haze

@Composable
fun SimpleMediaGrid(
    items: List<MediaItem>,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    emptyText: String,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onToggleFavorite: ((MediaItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText)
        }
        return
    }

    val hazeState = LocalHazeState.current
    val hazeModifier = if (hazeState != null) Modifier.haze(hazeState) else Modifier

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize().then(hazeModifier),
        contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 6.dp, bottom = FloatingBottomBarClearance),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items, key = { it.id }) { item ->
            PhotoThumbnail(
                item = item,
                isSelected = item.id in selectedIds,
                selectionMode = selectionMode,
                aspectRatio = 1f,
                cornerRadius = 10.dp,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
                onToggleFavorite = { onToggleFavorite?.invoke(item) },
            )
        }
    }
}
