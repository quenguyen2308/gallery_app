package com.gallery.domain.model

data class SecureMediaItem(
    val mediaId: Long,
    val encryptedPath: String,
    val displayName: String,
    val mimeType: String,
    val addedAtMillis: Long,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}
