package com.gallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey
    val mediaId: Long,
    val originalPath: String,
    val deletedAt: Long,
    val expiresAt: Long,
)
