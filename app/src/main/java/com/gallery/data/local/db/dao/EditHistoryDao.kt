package com.gallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gallery.data.local.db.entity.EditHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EditHistoryDao {

    @Query("SELECT * FROM edit_history WHERE mediaId = :mediaId ORDER BY createdAt DESC")
    fun observeForMedia(mediaId: Long): Flow<List<EditHistoryEntity>>

    @Insert
    suspend fun insert(entry: EditHistoryEntity): Long
}
