package com.gallery.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
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
            SheetItem(Icons.Rounded.Edit, "Chỉnh sửa") { onDismiss(); actions.onEdit() }
            SheetItem(Icons.Rounded.Info, "Xem chi tiết") { onDismiss(); actions.onDetails() }
            SheetItem(Icons.Rounded.Edit, "Đổi tên") { onDismiss(); actions.onRename() }
            SheetItem(Icons.Rounded.Wallpaper, "Đặt làm hình nền") { onDismiss(); actions.onWallpaper() }
            SheetItem(Icons.Rounded.Contacts, "Đặt làm ảnh liên hệ") { onDismiss(); actions.onContactPhoto() }
        } else {
            SheetItem(Icons.Rounded.Dashboard, "Ảnh ghép") { onDismiss(); actions.onCollage() }
            SheetItem(Icons.Rounded.PlayCircle, "Slideshow") { onDismiss(); actions.onSlideshow() }
            SheetItem(Icons.Rounded.Animation, "Tạo GIF") { onDismiss(); actions.onCreateGif() }
            SheetItem(Icons.Rounded.CreateNewFolder, "Tạo album mới") { onDismiss(); actions.onCreateAlbum() }
        }
        SheetItem(Icons.Rounded.AddPhotoAlternate, "Thêm vào album") { onDismiss(); actions.onAddToAlbum() }
        SheetItem(Icons.Rounded.Lock, "Secure Folder") { onDismiss(); actions.onSecureFolder() }
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
