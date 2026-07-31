package com.gallery.ui.albums

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.domain.model.AlbumId
import com.gallery.domain.model.MediaItem
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.MediaSelectionScaffold
import com.gallery.ui.components.SimpleMediaGrid

@Composable
fun AlbumDetailScreen(
    albumId: AlbumId,
    albumName: String,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onOpenViewer: (mediaId: Long, viewerList: List<MediaItem>) -> Unit,
) {
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(albumId) {
        viewModel.observeAlbumMedia(albumId).collect { items = it }
    }
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val currentAlbumRoomId = (albumId as? AlbumId.Custom)?.roomId

    MediaSelectionScaffold(
        viewModel = viewModel,
        items = items,
        title = albumName,
        onBack = onBack,
        currentAlbumId = currentAlbumRoomId,
    ) { padding ->
        SimpleMediaGrid(
            items = items,
            selectedIds = selectedIds,
            selectionMode = selectionMode,
            emptyText = stringResource(R.string.empty_photos),
            onItemClick = { item ->
                if (selectionMode) viewModel.toggleSelected(item.id) else onOpenViewer(item.id, items)
            },
            onItemLongClick = { item -> if (!selectionMode) viewModel.enterSelection(item.id) },
            modifier = Modifier.padding(padding),
        )
    }
}
