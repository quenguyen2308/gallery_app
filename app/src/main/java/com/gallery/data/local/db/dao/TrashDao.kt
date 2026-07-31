package com.gallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gallery.data.local.db.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Query("SELECT * FROM trash ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    @Query("SELECT mediaId FROM trash")
    fun observeTrashedIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TrashEntity>)

    @Query("DELETE FROM trash WHERE mediaId IN (:mediaIds)")
    suspend fun deleteAll(mediaIds: List<Long>)

    @Query("SELECT * FROM trash WHERE expiresAt <= :now")
    suspend fun getExpired(now: Long): List<TrashEntity>
}
