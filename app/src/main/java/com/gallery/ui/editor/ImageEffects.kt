package com.gallery.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class FilterPreset(val label: String) {
    NONE("Không"),
    VIVID("Vivid"),
    MONO("Mono"),
    WARM("Warm"),
    COOL("Cool"),
    VINTAGE("Vintage"),
    DRAMATIC("Dramatic"),
}

/**
 * ColorMatrix-based adjustments/filters so slider drags can be live-previewed with a cheap
 * Compose [androidx.compose.ui.graphics.ColorFilter] instead of re-rendering pixels every frame.
 * Only sharpening (a real convolution) and crop/rotate/flip touch actual pixel buffers.
 */
object ImageEffects {

    private val IDENTITY = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    fun combinedMatrix(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        preset: FilterPreset,
        filterIntensity: Float,
    ): FloatArray {
        val adjust = adjustMatrix(brightness, contrast, saturation)
        if (preset == FilterPreset.NONE || filterIntensity <= 0f) return adjust
        val blendedFilter = lerp(IDENTITY, filterMatrix(preset), filterIntensity.coerceIn(0f, 1f))
        val result = android.graphics.ColorMatrix()
        result.setConcat(android.graphics.ColorMatrix(blendedFilter), android.graphics.ColorMatrix(adjust))
        return result.array
    }

    /** brightness/contrast/saturation each in -100..100, 0 = no change. */
    private fun adjustMatrix(brightness: Float, contrast: Float, saturation: Float): FloatArray {
        val saturationCm = android.graphics.ColorMatrix().apply { setSaturation((saturation + 100f) / 100f) }
        val contrastScale = (contrast + 100f) / 100f
        val brightnessOffset = brightness * 2.55f
        val offset = 127.5f * (1f - contrastScale) + brightnessOffset
        val contrastBrightnessCm = android.graphics.ColorMatrix(
            floatArrayOf(
                contrastScale, 0f, 0f, 0f, offset,
                0f, contrastScale, 0f, 0f, offset,
                0f, 0f, contrastScale, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        val result = android.graphics.ColorMatrix()
        result.setConcat(contrastBrightnessCm, saturationCm)
        return result.array
    }

    private fun filterMatrix(preset: FilterPreset): FloatArray = when (preset) {
        FilterPreset.NONE -> IDENTITY
        FilterPreset.VIVID -> {
            val sat = android.graphics.ColorMatrix().apply { setSaturation(1.5f) }
            val contrast = android.graphics.ColorMatrix(
                floatArrayOf(
                    1.15f, 0f, 0f, 0f, -19f,
                    0f, 1.15f, 0f, 0f, -19f,
                    0f, 0f, 1.15f, 0f, -19f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            val result = android.graphics.ColorMatrix()
            result.setConcat(contrast, sat)
            result.array
        }
        FilterPreset.MONO -> android.graphics.ColorMatrix().apply { setSaturation(0f) }.array
        FilterPreset.WARM -> floatArrayOf(
            1.12f, 0f, 0f, 0f, 8f,
            0f, 1.03f, 0f, 0f, 0f,
            0f, 0f, 0.88f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f,
        )
        FilterPreset.COOL -> floatArrayOf(
            0.90f, 0f, 0f, 0f, -6f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 1.14f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f,
        )
        FilterPreset.VINTAGE -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        FilterPreset.DRAMATIC -> {
            val sat = android.graphics.ColorMatrix().apply { setSaturation(0.8f) }
            val contrast = android.graphics.ColorMatrix(
                floatArrayOf(
                    1.4f, 0f, 0f, 0f, -55f,
                    0f, 1.4f, 0f, 0f, -55f,
                    0f, 0f, 1.4f, 0f, -55f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            val result = android.graphics.ColorMatrix()
            result.setConcat(contrast, sat)
            result.array
        }
    }

    private fun lerp(a: FloatArray, b: FloatArray, t: Float): FloatArray =
        FloatArray(a.size) { i -> a[i] + (b[i] - a[i]) * t }

    fun toComposeColorMatrix(array: FloatArray): androidx.compose.ui.graphics.ColorMatrix =
        androidx.compose.ui.graphics.ColorMatrix(array)

    suspend fun bake(bitmap: Bitmap, matrixArray: FloatArray): Bitmap = withContext(Dispatchers.Default) {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrixArray)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        output
    }

    /** amount in 0..100; 0 is a no-op. Simple 5-point unsharp-style convolution. */
    suspend fun sharpen(bitmap: Bitmap, amount: Float): Bitmap = withContext(Dispatchers.Default) {
        if (amount <= 0f) return@withContext bitmap
        val w = bitmap.width
        val h = bitmap.height
        val k = (amount / 100f) * 1.5f
        val src = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val dst = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    dst[idx] = src[idx]
                    continue
                }
                dst[idx] = convolvePixel(
                    center = src[idx],
                    up = src[idx - w],
                    down = src[idx + w],
                    left = src[idx - 1],
                    right = src[idx + 1],
                    k = k,
                )
            }
        }
        Bitmap.createBitmap(dst, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun convolvePixel(center: Int, up: Int, down: Int, left: Int, right: Int, k: Float): Int {
        fun channel(shift: Int): Int {
            val c = (center shr shift) and 0xFF
            val u = (up shr shift) and 0xFF
            val d = (down shr shift) and 0xFF
            val l = (left shr shift) and 0xFF
            val r = (right shr shift) and 0xFF
            val value = c * (1 + 4 * k) - k * (u + d + l + r)
            return value.roundToInt().coerceIn(0, 255)
        }
        val alpha = (center shr 24) and 0xFF
        return (alpha shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flip(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun capSize(bitmap: Bitmap, maxDimension: Int = 2048): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide
        val newWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
