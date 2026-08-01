package com.gallery.ui.albums

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gallery.R
import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumId
import com.gallery.domain.model.storageKey
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.FloatingBottomBarClearance
import com.gallery.ui.components.TextInputDialog

@Composable
fun AlbumsScreen(
    viewModel: GalleryViewModel,
    onOpenAlbum: (AlbumId) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSecureFolder: () -> Unit,
    onOpenHideAlbums: () -> Unit,
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val hiddenKeys by viewModel.hiddenAlbumKeys.collectAsStateWithLifecycle()
    val trash by viewModel.trash.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var albumToRename by remember { mutableStateOf<Album?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }

    val visibleAlbums = remember(albums, hiddenKeys) {
        albums.filter { it.id.storageKey() !in hiddenKeys }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_create_album))
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    }
                    Box {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                                },
                                text = { Text(stringResource(R.string.action_hide_albums)) },
                                onClick = {
                                    showTopMenu = false
                                    onOpenHideAlbums()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = FloatingBottomBarClearance,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.all_albums),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }

            item {
                SystemAlbumCard(
                    name = stringResource(R.string.trash_title),
                    itemCount = trash.size,
                    coverUri = trash.firstOrNull()?.media?.uri,
                    icon = Icons.Rounded.Delete,
                    onClick = onOpenTrash,
                )
            }

            item {
                SystemAlbumCard(
                    name = stringResource(R.string.secure_folder_title),
                    itemCount = 0,
                    coverUri = null,
                    icon = Icons.Rounded.Lock,
                    onClick = onOpenSecureFolder,
                )
            }

            items(visibleAlbums, key = { it.id.toString() }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onOpenAlbum(album.id) },
                    onRename = { albumToRename = album },
                    onDelete = {
                        if (album.id is AlbumId.Custom) viewModel.deleteAlbum(album.id.roomId)
                    },
                )
            }
        }
    }

    if (showCreateDialog) {
        TextInputDialog(
            title = stringResource(R.string.action_create_album),
            hint = stringResource(R.string.new_album_name_hint),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createAlbum(name)
                showCreateDialog = false
            },
        )
    }

    albumToRename?.let { album ->
        TextInputDialog(
            title = stringResource(R.string.action_rename),
            initialValue = album.name,
            confirmLabel = stringResource(R.string.save),
            onDismiss = { albumToRename = null },
            onConfirm = { name ->
                if (album.id is AlbumId.Custom) viewModel.renameAlbum(album.id.roomId, name)
                albumToRename = null
            },
        )
    }
}

@Composable
private fun SystemAlbumCard(
    name: String,
    itemCount: Int,
    coverUri: android.net.Uri?,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        if (coverUri != null) {
            AsyncImage(
                model = coverUri,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.65f),
                    )
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "$itemCount",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (album.id is AlbumId.Custom) showMenu = true },
            ),
    ) {
        if (album.coverUri != null) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.PhotoAlbum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.65f),
                    )
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "${album.itemCount}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                onClick = { showMenu = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                onClick = { showMenu = false; onDelete() },
            )
        }
    }
}
