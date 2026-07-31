package com.gallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_items")
data class SecureItemEntity(
    @PrimaryKey
    val mediaId: Long,
    val encryptedPath: String,
    val originalDisplayName: String,
    val mimeType: String,
    val addedAt: Long,
)
