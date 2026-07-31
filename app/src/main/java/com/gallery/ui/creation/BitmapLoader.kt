package com.gallery.ui.creation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun loadBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? =
    withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        var sampleSize = 1
        val (w, h) = options.outWidth to options.outHeight
        while (w / sampleSize > maxDimension * 2 || h / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }
    }
