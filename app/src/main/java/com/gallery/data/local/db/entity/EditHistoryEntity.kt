package com.gallery.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edit_history")
data class EditHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val editType: String,
    val paramsJson: String?,
    val createdAt: Long,
)
