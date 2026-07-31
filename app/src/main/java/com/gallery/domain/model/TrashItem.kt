package com.gallery.domain.model

data class TrashItem(
    val media: MediaItem,
    val deletedAtMillis: Long,
    val expiresAtMillis: Long,
) {
    val daysLeft: Int
        get() {
            val remainingMs = expiresAtMillis - System.currentTimeMillis()
            return (remainingMs / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
        }
}
