package com.gallery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
) {
    val customAlbums = albums.filter { it.id is AlbumId.Custom }
    var selected by remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_to_album)) },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.action_create_album)) },
                        leadingContent = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onCreateNew()
                            },
                    )
                }
                items(customAlbums, key = { (it.id as AlbumId.Custom).roomId }) { album ->
                    val roomId = (album.id as AlbumId.Custom).roomId
                    val isSelected = roomId in selected
                    ListItem(
                        headlineContent = { Text(album.name) },
                        leadingContent = {
                            if (mode == AlbumPickerMode.MULTI) {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            } else {
                                RadioButton(selected = isSelected, onClick = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = when (mode) {
                                    AlbumPickerMode.MULTI -> if (isSelected) selected - roomId else selected + roomId
                                    AlbumPickerMode.SINGLE -> setOf(roomId)
                                }
                            },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
