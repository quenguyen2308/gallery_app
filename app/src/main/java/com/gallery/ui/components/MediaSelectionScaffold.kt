package com.gallery.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.domain.model.AlbumId
import com.gallery.domain.model.MediaItem
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.creation.CollageScreen
import com.gallery.ui.creation.GifCreatorScreen
import com.gallery.ui.creation.SlideshowScreen
import com.gallery.ui.selection.SelectionActionBar
import com.gallery.ui.selection.SelectionTopBar
import com.gallery.ui.util.assignAsContactPhoto
import com.gallery.ui.util.setAsWallpaper
import com.gallery.ui.util.shareMedia

private enum class PendingAlbumAction { MOVE, COPY, ADD }

/**
 * Shared chrome for any media grid screen (Photos / Album detail / Favorites): swaps the top
 * bar and bottom nav for the selection toolbar + action bar, and hosts every dialog/fullscreen
 * flow reachable from the selection action bar and the "More" bottom sheet.
 */
@Composable
fun MediaSelectionScaffold(
    viewModel: GalleryViewModel,
    items: List<MediaItem>,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    currentAlbumId: Long? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val context = LocalContext.current
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    val selectedItems = remember(items, selectedIds) { items.filter { it.id in selectedIds } }
    val isAllFavorite = selectedItems.isNotEmpty() && selectedItems.all { it.isFavorite }

    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    var showAddSheet by remember { mutableStateOf(false) }
    var pendingAlbumAction by remember { mutableStateOf<PendingAlbumAction?>(null) }
    var showCreateAlbumForSelection by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showCollage by remember { mutableStateOf(false) }
    var showGif by remember { mutableStateOf(false) }
    var showSlideshow by remember { mutableStateOf(false) }

    if (showSlideshow) {
        SlideshowScreen(items = selectedItems, onClose = { showSlideshow = false })
        return
    }
    if (showCollage) {
        CollageScreen(viewModel = viewModel, mediaIds = selectedIds.toList(), onDone = { showCollage = false; viewModel.clearSelection() })
        return
    }
    if (showGif) {
        GifCreatorScreen(viewModel = viewModel, mediaIds = selectedIds.toList(), onDone = { showGif = false; viewModel.clearSelection() })
        return
    }

    Scaffold(
        topBar = {
            when {
                selectionMode -> SelectionTopBar(
                    selectedCount = selectedIds.size,
                    isAllSelected = items.isNotEmpty() && selectedIds.containsAll(items.map { it.id }),
                    onToggleSelectAll = { viewModel.toggleGroupSelection(items.map { it.id }) },
                )
                title != null -> TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        onBack?.let {
                            IconButton(onClick = it) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(padding)
            if (selectionMode && selectedItems.isNotEmpty()) {
                SelectionActionBar(
                    isFavorite = isAllFavorite,
                    onShare = { shareMedia(context, selectedItems.map { it.uri }) },
                    onDelete = { viewModel.moveToTrash(selectedIds) },
                    onMove = { pendingAlbumAction = PendingAlbumAction.MOVE },
                    onToggleFavorite = { viewModel.setFavorite(selectedIds, !isAllFavorite) },
                    onCopy = { pendingAlbumAction = PendingAlbumAction.COPY },
                    onMore = { showAddSheet = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    if (showAddSheet) {
        AddBottomSheet(
            selectionCount = selectedItems.size,
            actions = AddSheetActions(
                onDetails = { showDetails = true },
                onRename = { showRename = true },
                onWallpaper = { selectedItems.firstOrNull()?.let { setAsWallpaper(context, it.uri) } },
                onContactPhoto = { selectedItems.firstOrNull()?.let { assignAsContactPhoto(context, it.uri) } },
                onCollage = { showCollage = true },
                onSlideshow = { showSlideshow = true },
                onCreateGif = { showGif = true },
                onCreateAlbum = { showCreateAlbumForSelection = true },
                onAddToAlbum = { pendingAlbumAction = PendingAlbumAction.ADD },
                onSecureFolder = { viewModel.moveToSecureFolder(selectedIds) },
            ),
            onDismiss = { showAddSheet = false },
        )
    }

    if (showDetails) {
        selectedItems.firstOrNull()?.let { item ->
            MediaDetailsDialog(item = item, onDismiss = { showDetails = false })
        }
    }

    if (showRename) {
        selectedItems.firstOrNull()?.let { item ->
            TextInputDialog(
                title = "Đổi tên",
                initialValue = item.displayName,
                onDismiss = { showRename = false },
                onConfirm = { name ->
                    viewModel.renameMedia(item.id, name)
                    showRename = false
                },
            )
        }
    }

    if (showCreateAlbumForSelection) {
        TextInputDialog(
            title = "Tạo album mới",
            hint = "Tên album",
            confirmLabel = "Tạo",
            onDismiss = { showCreateAlbumForSelection = false },
            onConfirm = { name ->
                viewModel.createAlbumFromSelection(name, selectedIds)
                showCreateAlbumForSelection = false
            },
        )
    }

    pendingAlbumAction?.let { action ->
        val mode = if (action == PendingAlbumAction.ADD) AlbumPickerMode.MULTI else AlbumPickerMode.SINGLE
        AlbumPickerDialog(
            albums = albums,
            mode = mode,
            onDismiss = { pendingAlbumAction = null },
            onConfirm = { targetAlbumIds ->
                when (action) {
                    PendingAlbumAction.ADD -> targetAlbumIds.forEach { viewModel.addToAlbum(it, selectedIds) }
                    PendingAlbumAction.COPY -> targetAlbumIds.firstOrNull()?.let { viewModel.addToAlbum(it, selectedIds) }
                    PendingAlbumAction.MOVE -> targetAlbumIds.firstOrNull()?.let { target ->
                        if (currentAlbumId != null) {
                            viewModel.moveToAlbum(currentAlbumId, target, selectedIds)
                        } else {
                            viewModel.addToAlbum(target, selectedIds)
                        }
                    }
                }
                pendingAlbumAction = null
            },
            onCreateNew = { showCreateAlbumForSelection = true },
        )
    }
}
