package com.gallery.ui.util

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri

fun shareMedia(context: Context, uris: List<Uri>, mimeType: String = "*/*") {
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, null))
}

fun setAsWallpaper(context: Context, uri: Uri) {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val bitmap = android.graphics.BitmapFactory.decodeStream(input)
        WallpaperManager.getInstance(context).setBitmap(bitmap)
    }
}

fun assignAsContactPhoto(context: Context, uri: Uri) {
    val intent = Intent("android.intent.action.ATTACH_DATA").apply {
        setDataAndType(uri, "image/*")
        putExtra("noCrop", false)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
