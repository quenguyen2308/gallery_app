package com.gallery.data.local.mediastore

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gallery.domain.repository.DeleteResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaFileOperations @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun mediaUri(mediaId: Long, isVideo: Boolean): Uri = ContentUris.withAppendedId(
        if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        mediaId,
    )

    suspend fun renameMedia(uri: Uri, newDisplayName: String): Boolean = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
        }
        try {
            context.contentResolver.update(uri, values, null, null) > 0
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Deletes rows immediately when possible. On Android 10 the OS may require a one-time
     * user grant (RecoverableSecurityException); on Android 11+ deleting media the app does
     * not own always requires routing through the system confirmation dialog.
     */
    suspend fun deleteMedia(uris: List<Uri>): DeleteResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            return@withContext DeleteResult.RequiresConfirmation(pendingIntent.intentSender)
        }
        try {
            var deleted = 0
            uris.forEach { uri -> deleted += context.contentResolver.delete(uri, null, null) }
            if (deleted > 0) DeleteResult.Success else DeleteResult.Error("Không xóa được tệp")
        } catch (e: RecoverableSecurityException) {
            DeleteResult.RequiresConfirmation(e.userAction.actionIntent.intentSender)
        } catch (e: SecurityException) {
            DeleteResult.Error(e.message ?: "Không có quyền xóa")
        }
    }

    /** Best-effort silent delete for background/WorkManager use — returns true only on success. */
    fun deleteMediaSilently(uri: Uri): Boolean = try {
        context.contentResolver.delete(uri, null, null) > 0
    } catch (e: SecurityException) {
        false
    }

    suspend fun saveBitmap(displayName: String, bitmap: Bitmap, subFolder: String): Uri? =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$subFolder")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = context.contentResolver.insert(collection, values) ?: return@withContext null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        }

    suspend fun saveGif(displayName: String, bytes: ByteArray, subFolder: String): Uri? =
        saveMediaBytes(displayName, "image/gif", bytes, subFolder)

    /** Generic byte writer used for GIF export and Secure Folder restore (images or videos). */
    suspend fun saveMediaBytes(displayName: String, mimeType: String, bytes: ByteArray, subFolder: String): Uri? =
        withContext(Dispatchers.IO) {
            val isVideo = mimeType.startsWith("video/")
            val baseDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$baseDir/$subFolder")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = context.contentResolver.insert(collection, values) ?: return@withContext null
            context.contentResolver.openOutputStream(uri)?.use { out -> out.write(bytes) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        }

    fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        stream.toByteArray()
    }
}
