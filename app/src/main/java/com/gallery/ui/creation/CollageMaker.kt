package com.gallery.ui.creation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.ceil
import kotlin.math.sqrt

/** Lays out 2-6 photos on a simple grid. Not a curated layout engine — a functional MVP composer. */
fun createCollage(bitmaps: List<Bitmap>, cellSize: Int = 512, spacing: Int = 6): Bitmap {
    require(bitmaps.isNotEmpty()) { "Cần ít nhất 1 ảnh để tạo collage" }
    val count = bitmaps.size
    val columns = ceil(sqrt(count.toDouble())).toInt().coerceAtLeast(1)
    val rows = ceil(count.toDouble() / columns).toInt()
    val width = columns * cellSize + (columns - 1) * spacing
    val height = rows * cellSize + (rows - 1) * spacing

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)

    bitmaps.forEachIndexed { index, bitmap ->
        val col = index % columns
        val row = index / columns
        val x = col * (cellSize + spacing)
        val y = row * (cellSize + spacing)
        val cell = centerCropScale(bitmap, cellSize, cellSize)
        canvas.drawBitmap(cell, x.toFloat(), y.toFloat(), null)
    }
    return output
}

private fun centerCropScale(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    val srcRatio = src.width.toFloat() / src.height
    val targetRatio = targetWidth.toFloat() / targetHeight
    val cropWidth: Int
    val cropHeight: Int
    if (srcRatio > targetRatio) {
        cropHeight = src.height
        cropWidth = (src.height * targetRatio).toInt().coerceAtMost(src.width)
    } else {
        cropWidth = src.width
        cropHeight = (src.width / targetRatio).toInt().coerceAtMost(src.height)
    }
    val x = (src.width - cropWidth) / 2
    val y = (src.height - cropHeight) / 2
    val cropped = Bitmap.createBitmap(src, x, y, cropWidth, cropHeight)
    return Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
}
