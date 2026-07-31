package com.gallery.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gallery.R
import com.gallery.domain.model.MediaItem
import java.text.DateFormat
import java.util.Date

@Composable
fun MediaDetailsDialog(item: MediaItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.displayName) },
        text = {
            Column {
                val dateFormat = remember { DateFormat.getDateTimeInstance() }
                DetailRow("Ngày chụp", dateFormat.format(Date(item.dateTakenMillis)))
                DetailRow("Kích thước", "${item.width} × ${item.height}")
                DetailRow("Dung lượng", formatFileSize(item.sizeBytes))
                DetailRow("Loại", item.mimeType)
                DetailRow("Thư mục", item.bucketName)
                if (item.isVideo) {
                    DetailRow("Thời lượng", formatDuration(item.durationMs))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(kb)
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
