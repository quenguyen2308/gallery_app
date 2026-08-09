package com.gallery.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.gallery.domain.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val externalUri: Uri = MediaStore.Files.getContentUri("external")

    private val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DATE_TAKEN,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.DURATION,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.MediaColumns.RELATIVE_PATH,
        MediaStore.MediaColumns.DATA,
    )

    /** Emits the full media library every time MediaStore changes. */
    fun observeAllMedia(): Flow<List<MediaItem>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(externalUri, true, observer)
        trySend(Unit)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.map { queryMedia(mediaTypeSelection(), mediaTypeArgs()) }

    suspend fun getMediaItem(mediaId: Long): MediaItem? = withContext(Dispatchers.IO) {
        // mediaTypeSelection() must be parenthesized: SQL binds AND tighter than OR, so without
        // the parens this reads as "MEDIA_TYPE = image OR (MEDIA_TYPE = video AND _ID = ?)" —
        // matching every single image row in the library regardless of the requested id.
        val selection = "(${mediaTypeSelection()}) AND ${MediaStore.Files.FileColumns._ID} = ?"
        queryMedia(selection, mediaTypeArgs() + mediaId.toString()).firstOrNull()
    }

    suspend fun getMediaItems(mediaIds: List<Long>): List<MediaItem> = withContext(Dispatchers.IO) {
        if (mediaIds.isEmpty()) return@withContext emptyList()
        val placeholders = mediaIds.joinToString(",") { "?" }
        // Same parenthesization fix as getMediaItem — see comment there.
        val selection = "(${mediaTypeSelection()}) AND ${MediaStore.Files.FileColumns._ID} IN ($placeholders)"
        queryMedia(selection, mediaTypeArgs() + mediaIds.map { it.toString() })
    }

    suspend fun findOriginalPath(mediaId: Long): String? = withContext(Dispatchers.IO) {
        getMediaItem(mediaId)?.let { "${it.bucketName}/${it.displayName}" }
    }

    private fun mediaTypeSelection(): String =
        "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"

    private fun mediaTypeArgs(): Array<String> = arrayOf(
        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
    )

    private suspend fun queryMedia(selection: String, selectionArgs: Array<String>): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<MediaItem>()
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            context.contentResolver.query(externalUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val relativePathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val isVideo = cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val contentUri = ContentUris.withAppendedId(
                        if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    val dateAddedSec = cursor.getLong(dateAddedCol)
                    val dateTakenMillis = cursor.getLong(dateTakenCol).takeIf { it > 0 } ?: (dateAddedSec * 1000)
                    items += MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = cursor.getString(nameCol) ?: "",
                        dateAddedMillis = dateAddedSec * 1000,
                        dateTakenMillis = dateTakenMillis,
                        mimeType = cursor.getString(mimeCol) ?: "",
                        isVideo = isVideo,
                        durationMs = cursor.getLong(durationCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        sizeBytes = cursor.getLong(sizeCol),
                        bucketId = cursor.getString(bucketIdCol) ?: "",
                        bucketName = cursor.getString(bucketNameCol) ?: "",
                        relativePath = run {
                            val rel = cursor.getString(relativePathCol)?.trimEnd('/')
                            if (!rel.isNullOrBlank()) rel
                            else cursor.getString(dataCol)?.substringBeforeLast('/') ?: ""
                        },
                    )
                }
            }
            items
        }
}
