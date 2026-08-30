package com.gallery.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.domain.model.MediaItem
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.AddBottomSheet
import com.gallery.ui.components.AddSheetActions
import com.gallery.ui.components.AlbumPickerDialog
import com.gallery.ui.components.AlbumPickerMode
import com.gallery.ui.components.FloatingBottomBar
import com.gallery.ui.components.MediaDetailsDialog
import com.gallery.ui.components.PillActionItem
import com.gallery.ui.components.TextInputDialog
import com.gallery.ui.util.shareMedia

@Composable
fun ImageViewerScreen(
    items: List<MediaItem>,
    startMediaId: Long,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onOpenEditor: (mediaId: Long) -> Unit,
) {
    if (items.isEmpty()) {
        onBack()
        return
    }
    val context = LocalContext.current
    val startIndex = items.indexOfFirst { it.id == startMediaId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })
    var showDetails by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showAlbumPicker by remember { mutableStateOf(false) }
    var showCreateAlbum by remember { mutableStateOf(false) }
    // `items` is a static snapshot passed in once (GalleryViewModel.setViewerContext), so it never
    // reflects later favorite toggles on its own — track overrides locally for instant feedback.
    var favoriteOverrides by remember { mutableStateOf(emptyMap<Long, Boolean>()) }
    val currentItem = items.getOrNull(pagerState.currentPage.coerceIn(0, items.lastIndex))?.let { item ->
        favoriteOverrides[item.id]?.let { item.copy(isFavorite = it) } ?: item
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            if (item.isVideo) {
                VideoPlayerView(uri = item.uri, modifier = Modifier.fillMaxSize())
            } else {
                ZoomableAsyncImage(model = item.uri, contentDescription = item.displayName, modifier = Modifier.fillMaxSize())
            }
        }

        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            actions = {
                currentItem?.let {
                    IconButton(onClick = { showDetails = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = stringResource(R.string.action_details), tint = Color.White)
                    }
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more), tint = Color.White)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        currentItem?.let { item ->
            FloatingBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter).wrapContentWidth(),
            ) {
                PillActionItem(
                    icon = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.action_share),
                    onClick = { shareMedia(context, listOf(item.uri), item.mimeType) },
                )
                PillActionItem(
                    icon = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(if (item.isFavorite) R.string.action_unfavorite else R.string.action_favorite),
                    onClick = {
                        val newValue = !item.isFavorite
                        favoriteOverrides = favoriteOverrides + (item.id to newValue)
                        viewModel.setFavorite(listOf(item.id), newValue)
                    },
                    tint = if (item.isFavorite) Color(0xFFD4537E) else Color(0xFF3A3C40),
                )
                PillActionItem(
                    icon = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    onClick = { onOpenEditor(item.id) },
                )
                PillActionItem(
                    icon = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    onClick = { viewModel.moveToTrash(listOf(item.id)) { onBack() } },
                    tint = Color(0xFFA83636),
                )
            }
        }
    }

    if (showDetails && currentItem != null) {
        MediaDetailsDialog(item = currentItem, onDismiss = { showDetails = false })
    }

    if (showAddSheet && currentItem != null) {
        AddBottomSheet(
            selectionCount = 1,
            actions = AddSheetActions(
                onDetails = { showDetails = true },
                onRename = { showRename = true },
                onWallpaper = { com.gallery.ui.util.setAsWallpaper(context, currentItem.uri) },
                onContactPhoto = { com.gallery.ui.util.assignAsContactPhoto(context, currentItem.uri) },
                onAddToAlbum = { showAlbumPicker = true },
                onSecureFolder = { viewModel.moveToSecureFolder(listOf(currentItem.id)); onBack() },
            ),
            onDismiss = { showAddSheet = false },
        )
    }

    if (showRename && currentItem != null) {
        TextInputDialog(
            title = stringResource(R.string.action_rename),
            initialValue = currentItem.displayName,
            onDismiss = { showRename = false },
            onConfirm = { name ->
                viewModel.renameMedia(currentItem.id, name)
                showRename = false
            },
        )
    }

    if (showAlbumPicker && currentItem != null) {
        val albums by viewModel.albums.collectAsStateWithLifecycle()
        AlbumPickerDialog(
            albums = albums,
            mode = AlbumPickerMode.MULTI,
            onDismiss = { showAlbumPicker = false },
            onConfirm = { targetAlbumIds ->
                targetAlbumIds.forEach { viewModel.addToAlbum(it, listOf(currentItem.id)) }
                showAlbumPicker = false
            },
            onCreateNew = { showAlbumPicker = false; showCreateAlbum = true },
        )
    }

    if (showCreateAlbum && currentItem != null) {
        TextInputDialog(
            title = stringResource(R.string.action_create_album),
            hint = stringResource(R.string.new_album_name_hint),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { showCreateAlbum = false },
            onConfirm = { name ->
                viewModel.createAlbumFromSelection(name, listOf(currentItem.id))
                showCreateAlbum = false
            },
        )
    }
}
