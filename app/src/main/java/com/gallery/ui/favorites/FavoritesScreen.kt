package com.gallery.ui.favorites

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.domain.model.MediaItem
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.MediaSelectionScaffold
import com.gallery.ui.components.SimpleMediaGrid

@Composable
fun FavoritesScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onOpenViewer: (mediaId: Long, viewerList: List<MediaItem>) -> Unit,
) {
    val items by viewModel.favorites.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()

    MediaSelectionScaffold(
        viewModel = viewModel,
        items = items,
        title = stringResource(R.string.favorites_title),
        onBack = onBack,
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
