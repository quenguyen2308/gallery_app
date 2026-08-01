package com.gallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.gallery.R
import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumId

enum class AlbumPickerMode { SINGLE, MULTI }

@Composable
fun AlbumPickerDialog(
    albums: List<Album>,
    mode: AlbumPickerMode,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
    onCreateNew: () -> Unit,
    title: String = stringResource(R.string.action_add_to_album),
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cancel))
                            }
                        },
                        actions = {
                            if (mode == AlbumPickerMode.MULTI) {
                                TextButton(
                                    onClick = { onConfirm(selected.toList()) },
                                    enabled = selected.isNotEmpty(),
                                ) { Text(stringResource(R.string.ok)) }
                            }
                        },
                    )
                },
            ) { padding ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDismiss(); onCreateNew() },
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.action_create_album))
                            }
                        }
                    }
                    // Show ALL albums; only Custom ones are selectable (Auto albums are read-only folders)
                    items(albums, key = { it.id.toString() }) { album ->
                        val roomId = (album.id as? AlbumId.Custom)?.roomId
                        val isCustom = roomId != null
                        val isSelected = roomId != null && roomId in selected
                        PickerAlbumCard(
                            album = album,
                            isSelected = isSelected,
                            showCheckbox = mode == AlbumPickerMode.MULTI && isCustom,
                            enabled = isCustom,
                            onClick = {
                                if (roomId == null) return@PickerAlbumCard
                                when (mode) {
                                    AlbumPickerMode.SINGLE -> onConfirm(listOf(roomId))
                                    AlbumPickerMode.MULTI -> selected = if (isSelected) selected - roomId else selected + roomId
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerAlbumCard(
    album: Album,
    isSelected: Boolean,
    showCheckbox: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (album.coverUri != null) {
            AsyncImage(
                model = album.coverUri,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!enabled) Modifier.then(Modifier) else Modifier),
            )
        } else {
            Icon(
                Icons.Rounded.PhotoAlbum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.6f else 0.3f),
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
            )
        }
        // gradient scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = if (enabled) 0.6f else 0.4f),
                    )
                ),
        )
        // disabled overlay
        if (!enabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.4f)),
            )
        }
        if (showCheckbox) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            )
        } else if (isSelected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "${album.itemCount}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

