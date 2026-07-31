package com.gallery.ui.viewer

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoPlayerView(uri: Uri, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(uri)
                setMediaController(MediaController(context).also { it.setAnchorView(this) })
                setOnPreparedListener { it.isLooping = false }
                start()
            }
        },
    )
}
