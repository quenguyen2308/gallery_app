package com.gallery.domain.repository

import android.net.Uri
import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumId
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.SecureMediaItem
import com.gallery.domain.model.TrashItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    /** All visible media (excludes trashed and secure-folder items), newest first. */
    fun observeMediaItems(): Flow<List<MediaItem>>

    fun observeMediaItem(mediaId: Long): Flow<MediaItem?>

    fun observeAlbums(): Flow<List<Album>>

    fun observeAlbumMedia(albumId: AlbumId): Flow<List<MediaItem>>

    fun observeFavorites(): Flow<List<MediaItem>>

    fun observeTrash(): Flow<List<TrashItem>>

    fun observeSecureItems(): Flow<List<SecureMediaItem>>

    // Favorites
    suspend fun setFavorite(mediaIds: List<Long>, isFavorite: Boolean)

    // Trash
    suspend fun moveToTrash(mediaIds: List<Long>): DeleteResult
    /** Cleans up Room bookkeeping after a trash confirmation was accepted by the user (API 30+). */
    suspend fun finalizeTrashMove(mediaIds: List<Long>)
    suspend fun restoreFromTrash(mediaIds: List<Long>): DeleteResult
    /** Cleans up Room bookkeeping after a restore-from-trash confirmation was accepted (API 30+). */
    suspend fun finalizeRestoreFromTrash(mediaIds: List<Long>)
    suspend fun deleteForever(mediaIds: List<Long>): DeleteResult

    /** Cleans up Room bookkeeping after a [DeleteResult.RequiresConfirmation] intent was confirmed by the user. */
    suspend fun finalizeDelete(mediaIds: List<Long>)
    suspend fun purgeExpiredTrash()

    // Albums (custom)
    suspend fun createAlbum(name: String): Long
    suspend fun renameAlbum(albumId: Long, name: String)
    suspend fun deleteAlbum(albumId: Long)
    suspend fun addToAlbum(albumId: Long, mediaIds: List<Long>)
    suspend fun removeFromAlbum(albumId: Long, mediaIds: List<Long>)
    suspend fun moveToAlbum(fromAlbumId: Long, toAlbumId: Long, mediaIds: List<Long>)

    // File ops
    suspend fun renameMedia(mediaId: Long, newDisplayName: String): Boolean
    suspend fun createImageFromBitmap(displayName: String, bitmap: android.graphics.Bitmap): Uri?
    suspend fun writeGifBytes(displayName: String, bytes: ByteArray): Uri?

    // Secure folder
    suspend fun moveToSecureFolder(mediaIds: List<Long>)
    suspend fun restoreFromSecureFolder(mediaIds: List<Long>)
    suspend fun decryptSecureThumbnail(mediaId: Long): android.graphics.Bitmap?

    // Hidden albums
    fun observeHiddenAlbumKeys(): kotlinx.coroutines.flow.Flow<Set<String>>
    suspend fun setAlbumHidden(id: AlbumId, hidden: Boolean)

    // Basic editor
    suspend fun saveEditedImage(
        originalMediaId: Long,
        bitmap: android.graphics.Bitmap,
        overwrite: Boolean,
        editType: String,
        paramsJson: String?,
    ): EditSaveResult
}

sealed interface DeleteResult {
    data object Success : DeleteResult
    data class RequiresConfirmation(val intentSender: android.content.IntentSender) : DeleteResult
    data class Error(val message: String) : DeleteResult
}

sealed interface EditSaveResult {
    data class Success(val uri: Uri, val overwritten: Boolean) : EditSaveResult
    data class Error(val message: String) : EditSaveResult
}
