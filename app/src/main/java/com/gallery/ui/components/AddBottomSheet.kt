package com.gallery.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoveToInbox
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.res.stringResource
import com.gallery.R

data class AddSheetActions(
    val onEdit: () -> Unit = {},
    val onDetails: () -> Unit = {},
    val onRename: () -> Unit = {},
    val onWallpaper: () -> Unit = {},
    val onContactPhoto: () -> Unit = {},
    val onCollage: () -> Unit = {},
    val onSlideshow: () -> Unit = {},
    val onCreateGif: () -> Unit = {},
    val onCreateAlbum: () -> Unit = {},
    val onCopy: () -> Unit = {},
    val onMove: () -> Unit = {},
    val onAddToAlbum: () -> Unit = {},
    val onSecureFolder: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
fun AddBottomSheet(
    selectionCount: Int,
    actions: AddSheetActions,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (selectionCount == 1) {
            SheetItem(Icons.Rounded.Edit, stringResource(R.string.action_edit)) { onDismiss(); actions.onEdit() }
            SheetItem(Icons.Rounded.Info, stringResource(R.string.action_details)) { onDismiss(); actions.onDetails() }
            SheetItem(Icons.Rounded.Edit, stringResource(R.string.action_rename)) { onDismiss(); actions.onRename() }
            SheetItem(Icons.Rounded.Wallpaper, stringResource(R.string.action_set_wallpaper)) { onDismiss(); actions.onWallpaper() }
            SheetItem(Icons.Rounded.Contacts, stringResource(R.string.action_set_contact_photo)) { onDismiss(); actions.onContactPhoto() }
        } else {
            SheetItem(Icons.Rounded.Dashboard, stringResource(R.string.action_collage)) { onDismiss(); actions.onCollage() }
            SheetItem(Icons.Rounded.PlayCircle, stringResource(R.string.action_slideshow)) { onDismiss(); actions.onSlideshow() }
            SheetItem(Icons.Rounded.Animation, stringResource(R.string.action_create_gif)) { onDismiss(); actions.onCreateGif() }
            SheetItem(Icons.Rounded.CreateNewFolder, stringResource(R.string.action_create_album)) { onDismiss(); actions.onCreateAlbum() }
        }
        SheetItem(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy_to_album)) { onDismiss(); actions.onCopy() }
        SheetItem(Icons.Rounded.MoveToInbox, stringResource(R.string.action_move_to_album)) { onDismiss(); actions.onMove() }
        SheetItem(Icons.Rounded.AddPhotoAlternate, stringResource(R.string.action_add_to_album)) { onDismiss(); actions.onAddToAlbum() }
        SheetItem(Icons.Rounded.Lock, stringResource(R.string.action_secure_folder)) { onDismiss(); actions.onSecureFolder() }
    }
}

@Composable
private fun SheetItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
