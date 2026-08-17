package com.gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.gallery.data.local.db.dao.AlbumDao
import com.gallery.data.local.db.dao.AlbumMediaDao
import com.gallery.data.local.db.dao.EditHistoryDao
import com.gallery.data.local.db.dao.FavoriteDao
import com.gallery.data.local.db.dao.SecureItemDao
import com.gallery.data.local.db.dao.TrashDao
import com.gallery.data.local.db.entity.AlbumEntity
import com.gallery.data.local.db.entity.AlbumMediaEntity
import com.gallery.data.local.db.entity.EditHistoryEntity
import com.gallery.data.local.db.entity.FavoriteEntity
import com.gallery.data.local.db.entity.SecureItemEntity
import com.gallery.data.local.db.entity.TrashEntity
import com.gallery.data.local.HiddenAlbumsStore
import com.gallery.data.local.mediastore.MediaFileOperations
import com.gallery.data.local.mediastore.MediaStoreDataSource
import com.gallery.data.local.secure.SecureStorage
import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumId
import com.gallery.domain.model.storageKey
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.SecureMediaItem
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.DeleteResult
import com.gallery.domain.repository.EditSaveResult
import com.gallery.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TRASH_RETENTION_DAYS = 30L

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val mediaFileOperations: MediaFileOperations,
    private val secureStorage: SecureStorage,
    private val albumDao: AlbumDao,
    private val albumMediaDao: AlbumMediaDao,
    private val favoriteDao: FavoriteDao,
    private val trashDao: TrashDao,
    private val secureItemDao: SecureItemDao,
    private val editHistoryDao: EditHistoryDao,
    private val hiddenAlbumsStore: HiddenAlbumsStore,
) : MediaRepository {

    override fun observeMediaItems(): Flow<List<MediaItem>> = combine(
        mediaStoreDataSource.observeAllMedia(),
        favoriteDao.observeFavoriteIds(),
        trashDao.observeTrashedIds(),
        secureItemDao.observeSecureIds(),
    ) { all, favIds, trashIds, secureIds ->
        val favSet = favIds.toSet()
        val trashSet = trashIds.toSet()
        val secureSet = secureIds.toSet()
        all.asSequence()
            .filter { it.id !in trashSet && it.id !in secureSet }
            .map { it.copy(isFavorite = it.id in favSet) }
            .toList()
    }.combine(hiddenAlbumFilter()) { items, (hiddenBuckets, hiddenMediaIds) ->
        if (hiddenBuckets.isEmpty() && hiddenMediaIds.isEmpty()) items
        else items.filter { it.bucketId !in hiddenBuckets && it.id !in hiddenMediaIds }
    }

    private fun hiddenAlbumFilter(): Flow<Pair<Set<String>, Set<Long>>> = combine(
        hiddenAlbumsStore.hiddenKeys,
        albumDao.observeAlbums(),
        albumMediaDao.observeAll(),
    ) { hiddenKeys, albums, mappings ->
        if (hiddenKeys.isEmpty()) return@combine emptySet<String>() to emptySet()
        val hiddenBuckets = hiddenKeys
            .filter { it.startsWith("auto:") }
            .mapTo(mutableSetOf()) { it.removePrefix("auto:") }
        val hiddenCustomIds = albums
            .filter { "custom:${it.id}" in hiddenKeys }
            .mapTo(mutableSetOf()) { it.id }
        val hiddenMediaIds = mappings
            .filter { it.albumId in hiddenCustomIds }
            .mapTo(mutableSetOf()) { it.mediaId }
        hiddenBuckets to hiddenMediaIds
    }

    override fun observeMediaItem(mediaId: Long): Flow<MediaItem?> = combine(
        mediaStoreDataSource.observeAllMedia(),
        favoriteDao.observeIsFavorite(mediaId),
    ) { all, isFav ->
        all.find { it.id == mediaId }?.copy(isFavorite = isFav)
    }

    override fun observeAlbums(): Flow<List<Album>> = combine(
        mediaStoreDataSource.observeAllMedia(),
        albumDao.observeAlbums(),
        albumMediaDao.observeAll(),
        trashDao.observeTrashedIds(),
        secureItemDao.observeSecureIds(),
    ) { allMedia, customAlbums, mappings, trashIdList, secureIdList ->
        val trashIds = trashIdList.toSet()
        val secureIds = secureIdList.toSet()

        val visibleMedia = allMedia.filter { it.id !in trashIds && it.id !in secureIds }
        val mediaById = visibleMedia.associateBy { it.id }

        val autoAlbums = buildAutoAlbums(visibleMedia)

        val mappingsByAlbum = mappings.groupBy { it.albumId }
        val custom = customAlbums.map { entity ->
            val orderedIds = mappingsByAlbum[entity.id].orEmpty()
                .sortedByDescending { it.addedAt }
                .map { it.mediaId }
            val coverUri = entity.coverMediaId?.let { mediaById[it]?.uri }
                ?: orderedIds.firstNotNullOfOrNull { mediaById[it]?.uri }
            Album(
                id = AlbumId.Custom(entity.id),
                name = entity.name,
                coverUri = coverUri,
                itemCount = orderedIds.count { mediaById.containsKey(it) },
                isDefault = entity.isDefault,
            )
        }
        autoAlbums + custom
    }

    override fun observeAlbumMedia(albumId: AlbumId): Flow<List<MediaItem>> = when (albumId) {
        is AlbumId.Auto -> observeMediaItems().map { items -> items.filter { it.bucketId == albumId.bucketId } }
        is AlbumId.Custom -> combine(
            albumMediaDao.observeMediaIdsForAlbum(albumId.roomId),
            observeMediaItems(),
        ) { ids, all ->
            val byId = all.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }
    }

    override fun observeFavorites(): Flow<List<MediaItem>> = combine(
        favoriteDao.observeFavoriteIds(),
        observeMediaItems(),
    ) { ids, all ->
        val order = ids.withIndex().associate { (idx, id) -> id to idx }
        all.filter { it.id in order.keys }.sortedBy { order[it.id] }
    }

    override fun observeTrash(): Flow<List<TrashItem>> = combine(
        trashDao.observeAll(),
        mediaStoreDataSource.observeAllMediaIncludingTrashed(),
    ) { entries, allMedia ->
        val byId = allMedia.associateBy { it.id }
        entries.mapNotNull { entry ->
            byId[entry.mediaId]?.let { media -> TrashItem(media, entry.deletedAt, entry.expiresAt) }
        }
    }

    override fun observeSecureItems(): Flow<List<SecureMediaItem>> = secureItemDao.observeAll().map { entries ->
        entries.map {
            SecureMediaItem(
                mediaId = it.mediaId,
                encryptedPath = it.encryptedPath,
                displayName = it.originalDisplayName,
                mimeType = it.mimeType,
                addedAtMillis = it.addedAt,
            )
        }
    }

    override suspend fun setFavorite(mediaIds: List<Long>, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (isFavorite) {
            val now = System.currentTimeMillis()
            mediaIds.forEach { favoriteDao.insert(FavoriteEntity(it, now)) }
        } else {
            favoriteDao.deleteAll(mediaIds)
        }
    }

    override suspend fun moveToTrash(mediaIds: List<Long>): DeleteResult = withContext(Dispatchers.IO) {
        val items = mediaStoreDataSource.getMediaItems(mediaIds)
        val uris = items.map { it.uri }
        val result = if (uris.isNotEmpty()) mediaFileOperations.trashMedia(uris, trash = true) else DeleteResult.Success
        if (result is DeleteResult.Success) {
            finalizeTrashMove(mediaIds)
        }
        result
    }

    override suspend fun finalizeTrashMove(mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expiresAt = now + TimeUnit.DAYS.toMillis(TRASH_RETENTION_DAYS)
        val entries = mediaIds.map { id ->
            val originalPath = mediaStoreDataSource.findOriginalPath(id) ?: ""
            TrashEntity(mediaId = id, originalPath = originalPath, deletedAt = now, expiresAt = expiresAt)
        }
        trashDao.insertAll(entries)
    }

    override suspend fun restoreFromTrash(mediaIds: List<Long>): DeleteResult = withContext(Dispatchers.IO) {
        val items = mediaStoreDataSource.getMediaItems(mediaIds)
        val uris = items.map { it.uri }
        val result = if (uris.isNotEmpty()) mediaFileOperations.trashMedia(uris, trash = false) else DeleteResult.Success
        if (result is DeleteResult.Success) {
            finalizeRestoreFromTrash(mediaIds)
        }
        result
    }

    override suspend fun finalizeRestoreFromTrash(mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        trashDao.deleteAll(mediaIds)
    }

    override suspend fun deleteForever(mediaIds: List<Long>): DeleteResult = withContext(Dispatchers.IO) {
        val items = mediaStoreDataSource.getMediaItems(mediaIds)
        val uris = items.map { it.uri }
        if (uris.isEmpty()) return@withContext DeleteResult.Error("Không tìm thấy tệp")
        val result = mediaFileOperations.deleteMedia(uris)
        if (result is DeleteResult.Success) {
            finalizeDelete(mediaIds)
        }
        result
    }

    override suspend fun finalizeDelete(mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        trashDao.deleteAll(mediaIds)
        favoriteDao.deleteAll(mediaIds)
        albumMediaDao.removeMediaFromAllAlbums(mediaIds)
    }

    /**
     * Runs on a WorkManager background thread with no Activity to resolve a system
     * confirmation dialog. Deletion is attempted silently (succeeds for app-owned files);
     * anything the OS blocks stays in the trash tombstone and is retried on the next run.
     */
    override suspend fun purgeExpiredTrash() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expired = trashDao.getExpired(now)
        if (expired.isEmpty()) return@withContext
        val items = mediaStoreDataSource.getMediaItems(expired.map { it.mediaId })
        val itemsById = items.associateBy { it.id }
        val cleanedUpIds = expired.mapNotNull { entry ->
            val media = itemsById[entry.mediaId] ?: return@mapNotNull entry.mediaId // already gone, drop tombstone
            if (mediaFileOperations.deleteMediaSilently(media.uri)) entry.mediaId else null
        }
        if (cleanedUpIds.isNotEmpty()) {
            finalizeDelete(cleanedUpIds)
        }
    }

    override suspend fun createAlbum(name: String): Long = withContext(Dispatchers.IO) {
        albumDao.insert(AlbumEntity(name = name, coverMediaId = null, createdAt = System.currentTimeMillis()))
    }

    override suspend fun renameAlbum(albumId: Long, name: String) = withContext(Dispatchers.IO) {
        albumDao.rename(albumId, name)
    }

    override suspend fun deleteAlbum(albumId: Long) = withContext(Dispatchers.IO) {
        albumMediaDao.clearAlbum(albumId)
        albumDao.deleteById(albumId)
    }

    override suspend fun addToAlbum(albumId: Long, mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        albumMediaDao.insertAll(mediaIds.map { AlbumMediaEntity(albumId, it, now) })
    }

    override suspend fun removeFromAlbum(albumId: Long, mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        albumMediaDao.remove(albumId, mediaIds)
    }

    override suspend fun moveToAlbum(fromAlbumId: Long, toAlbumId: Long, mediaIds: List<Long>) =
        withContext(Dispatchers.IO) {
            albumMediaDao.remove(fromAlbumId, mediaIds)
            val now = System.currentTimeMillis()
            albumMediaDao.insertAll(mediaIds.map { AlbumMediaEntity(toAlbumId, it, now) })
        }

    override fun observeHiddenAlbumKeys(): kotlinx.coroutines.flow.Flow<Set<String>> = hiddenAlbumsStore.hiddenKeys

    override suspend fun setAlbumHidden(id: AlbumId, hidden: Boolean) {
        hiddenAlbumsStore.setHidden(id.storageKey(), hidden)
    }

    override suspend fun renameMedia(mediaId: Long, newDisplayName: String): Boolean = withContext(Dispatchers.IO) {
        val media = mediaStoreDataSource.getMediaItem(mediaId) ?: return@withContext false
        mediaFileOperations.renameMedia(media.uri, newDisplayName)
    }

    override suspend fun createImageFromBitmap(displayName: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        mediaFileOperations.saveBitmap(displayName, bitmap, "Gallery Collages")
    }

    override suspend fun writeGifBytes(displayName: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        mediaFileOperations.saveGif(displayName, bytes, "Gallery GIFs")
    }

    override suspend fun moveToSecureFolder(mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        val items = mediaStoreDataSource.getMediaItems(mediaIds)
        val now = System.currentTimeMillis()
        items.forEach { media ->
            context.contentResolver.openInputStream(media.uri)?.use { input ->
                val encryptedPath = secureStorage.encryptToFile(input)
                secureItemDao.insert(
                    SecureItemEntity(
                        mediaId = media.id,
                        encryptedPath = encryptedPath,
                        originalDisplayName = media.displayName,
                        mimeType = media.mimeType,
                        addedAt = now,
                    ),
                )
                // Best-effort: succeeds immediately pre-Android 11 or for app-owned files.
                // On newer OS versions for camera-owned photos this may require a follow-up
                // system confirmation which Secure Folder does not block on.
                mediaFileOperations.deleteMediaSilently(media.uri)
            }
        }
        finalizeDelete(mediaIds)
    }

    override suspend fun restoreFromSecureFolder(mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        mediaIds.forEach { mediaId ->
            val entry = secureItemDao.getByMediaId(mediaId) ?: return@forEach
            val bytes = secureStorage.openDecryptedStream(entry.encryptedPath).use { it.readBytes() }
            mediaFileOperations.saveMediaBytes(entry.originalDisplayName, entry.mimeType, bytes, "")
            secureStorage.delete(entry.encryptedPath)
            secureItemDao.delete(mediaId)
        }
    }

    override suspend fun decryptSecureThumbnail(mediaId: Long): Bitmap? = withContext(Dispatchers.IO) {
        val entry = secureItemDao.getByMediaId(mediaId) ?: return@withContext null
        if (entry.mimeType.startsWith("video/")) return@withContext null
        secureStorage.openDecryptedStream(entry.encryptedPath).use {
            android.graphics.BitmapFactory.decodeStream(it)
        }
    }

    override suspend fun saveEditedImage(
        originalMediaId: Long,
        bitmap: Bitmap,
        overwrite: Boolean,
        editType: String,
        paramsJson: String?,
    ): EditSaveResult = withContext(Dispatchers.IO) {
        val original = mediaStoreDataSource.getMediaItem(originalMediaId)
        var overwrittenUri: android.net.Uri? = null
        if (overwrite && original != null) {
            overwrittenUri = try {
                context.contentResolver.openOutputStream(original.uri, "wt")?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                original.uri
            } catch (e: Exception) {
                null
            }
        }

        val finalUri = overwrittenUri
            ?: mediaFileOperations.saveBitmap("edited_${System.currentTimeMillis()}.jpg", bitmap, "Gallery Edits")
            ?: return@withContext EditSaveResult.Error("Không thể lưu ảnh")

        val targetMediaId = if (overwrittenUri != null) originalMediaId else ContentUris.parseId(finalUri)
        editHistoryDao.insert(
            EditHistoryEntity(
                mediaId = targetMediaId,
                editType = editType,
                paramsJson = paramsJson,
                createdAt = System.currentTimeMillis(),
            ),
        )
        EditSaveResult.Success(finalUri, overwritten = overwrittenUri != null)
    }

    // Every distinct folder (MediaStore bucket) becomes a browsable auto-album — not just a
    // hardcoded whitelist — so photos saved by other apps, or by this app's own Edit/Collage/GIF
    // output folders, still show up somewhere in the Album tab instead of only in the unified
    // Photos tab.
    private fun buildAutoAlbums(media: List<MediaItem>): List<Album> = media
        .filter { it.bucketName.isNotBlank() }
        .groupBy { it.bucketId to it.bucketName }
        .map { (key, items) ->
            val (bucketId, bucketName) = key
            Album(
                id = AlbumId.Auto(bucketId),
                name = bucketName.replaceFirstChar { it.uppercase() },
                coverUri = items.firstOrNull()?.uri,
                itemCount = items.size,
                isDefault = true,
            )
        }
        .sortedBy { it.name }
}
