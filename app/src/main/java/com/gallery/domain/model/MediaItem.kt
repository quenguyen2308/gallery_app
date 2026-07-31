package com.gallery.domain.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAddedMillis: Long,
    val dateTakenMillis: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val bucketId: String,
    val bucketName: String,
    val isFavorite: Boolean = false,
)
