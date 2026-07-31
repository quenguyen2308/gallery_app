package com.gallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gallery.data.local.db.entity.SecureItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureItemDao {

    @Query("SELECT * FROM secure_items ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<SecureItemEntity>>

    @Query("SELECT mediaId FROM secure_items")
    fun observeSecureIds(): Flow<List<Long>>

    @Query("SELECT * FROM secure_items WHERE mediaId = :mediaId")
    suspend fun getByMediaId(mediaId: Long): SecureItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SecureItemEntity)

    @Query("DELETE FROM secure_items WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)
}
