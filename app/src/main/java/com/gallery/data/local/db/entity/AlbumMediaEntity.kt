package com.gallery.data.local.db.entity

import androidx.room.Entity

@Entity(tableName = "album_media", primaryKeys = ["albumId", "mediaId"])
data class AlbumMediaEntity(
    val albumId: Long,
    val mediaId: Long,
    val addedAt: Long,
)
