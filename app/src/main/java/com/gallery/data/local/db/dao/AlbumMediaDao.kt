package com.gallery.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gallery.data.local.db.entity.AlbumMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumMediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<AlbumMediaEntity>)

    @Query("DELETE FROM album_media WHERE albumId = :albumId AND mediaId IN (:mediaIds)")
    suspend fun remove(albumId: Long, mediaIds: List<Long>)

    @Query("DELETE FROM album_media WHERE mediaId IN (:mediaIds)")
    suspend fun removeMediaFromAllAlbums(mediaIds: List<Long>)

    @Query("DELETE FROM album_media WHERE albumId = :albumId")
    suspend fun clearAlbum(albumId: Long)

    @Query("SELECT * FROM album_media")
    fun observeAll(): Flow<List<AlbumMediaEntity>>

    @Query("SELECT mediaId FROM album_media WHERE albumId = :albumId ORDER BY addedAt DESC")
    fun observeMediaIdsForAlbum(albumId: Long): Flow<List<Long>>

    @Query("SELECT albumId FROM album_media WHERE mediaId = :mediaId")
    fun observeAlbumIdsForMedia(mediaId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM album_media WHERE albumId = :albumId")
    fun observeCountForAlbum(albumId: Long): Flow<Int>

    @Query("SELECT mediaId FROM album_media WHERE albumId = :albumId ORDER BY addedAt DESC LIMIT 1")
    suspend fun latestMediaId(albumId: Long): Long?
}
