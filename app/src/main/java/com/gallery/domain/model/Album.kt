package com.gallery.domain.model

import android.net.Uri

sealed interface AlbumId {
    data class Auto(val bucketId: String) : AlbumId
    data class Custom(val roomId: Long) : AlbumId
}

fun AlbumId.storageKey(): String = when (this) {
    is AlbumId.Auto -> "auto:$bucketId"
    is AlbumId.Custom -> "custom:$roomId"
}

data class Album(
    val id: AlbumId,
    val name: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val isDefault: Boolean,
)
