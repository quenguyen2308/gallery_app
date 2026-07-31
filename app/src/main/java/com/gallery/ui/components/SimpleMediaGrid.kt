package com.gallery.ui.components

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
import com.gallery.domain.model.MediaItem
import com.gallery.ui.photos.PhotoThumbnail

@Composable
fun SimpleMediaGrid(
    items: List<MediaItem>,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    emptyText: String,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = FloatingBottomBarClearance),
    ) {
        items(items, key = { it.id }) { item ->
            PhotoThumbnail(
                item = item,
                isSelected = item.id in selectedIds,
                selectionMode = selectionMode,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
            )
        }
    }
}
