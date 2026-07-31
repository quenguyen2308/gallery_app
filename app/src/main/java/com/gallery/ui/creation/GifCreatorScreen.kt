package com.gallery.ui.creation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gallery.ui.GalleryViewModel

@Composable
fun GifCreatorScreen(
    viewModel: GalleryViewModel,
    mediaIds: List<Long>,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var previewFrame by remember { mutableStateOf<Bitmap?>(null) }
    var gifBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mediaIds) {
        val order = mediaIds.withIndex().associate { (idx, id) -> id to idx }
        val items = viewModel.mediaItems.value.filter { it.id in order.keys }.sortedBy { order[it.id] }
        val bitmaps = items.mapNotNull { loadBitmap(context, it.uri, 360) }
        if (bitmaps.isNotEmpty()) {
            val width = bitmaps[0].width
            val height = bitmaps[0].height
            val uniform = bitmaps.map { Bitmap.createScaledBitmap(it, width, height, true) }
            previewFrame = uniform.first()
            gifBytes = GifEncoder.encode(uniform, frameDelayMs = 400)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo GIF") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, contentDescription = null) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.weight(1f))
                gifBytes != null -> {
                    previewFrame?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                    Text("Xem trước khung đầu tiên (${gifBytes!!.size / 1024} KB)")
                    Button(
                        onClick = {
                            viewModel.saveGif(gifBytes!!)
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Lưu GIF") }
                }
                else -> Text("Cần ít nhất 2 ảnh để tạo GIF", modifier = Modifier.weight(1f))
            }
        }
    }
}
