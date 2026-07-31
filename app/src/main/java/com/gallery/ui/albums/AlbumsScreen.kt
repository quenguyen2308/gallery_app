package com.gallery.ui.albums

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gallery.R
import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumId
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.FloatingBottomBarClearance
import com.gallery.ui.components.TextInputDialog

@Composable
fun AlbumsScreen(
    viewModel: GalleryViewModel,
    onOpenAlbum: (AlbumId) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSecureFolder: () -> Unit,
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var albumToRename by remember { mutableStateOf<Album?>(null) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.nav_albums), style = MaterialTheme.typography.titleLarge) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_create_album))
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 12.dp, top = 12.dp, end = 12.dp, bottom = FloatingBottomBarClearance,
            ),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    PinnedAlbumRow(Icons.Rounded.Favorite, stringResource(R.string.favorites_title), onOpenFavorites)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                    PinnedAlbumRow(Icons.Rounded.Delete, stringResource(R.string.trash_title), onOpenTrash)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                    PinnedAlbumRow(Icons.Rounded.Lock, stringResource(R.string.secure_folder_title), onOpenSecureFolder)
                }
            }
            if (albums.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.empty_albums))
                    }
                }
            } else {
                items(albums, key = { it.id.toString() }) { album ->
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
private fun PinnedAlbumRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (album.id is AlbumId.Custom) showMenu = true },
            ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    Icons.Rounded.PhotoAlbum,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.action_rename)) }, onClick = { showMenu = false; onRename() })
                DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = { showMenu = false; onDelete() })
            }
        }
        Text(
            album.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).padding(bottom = 0.dp),
        )
        Text(
            "${album.itemCount} ảnh",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 0.dp).padding(bottom = 10.dp),
        )
    }
}
