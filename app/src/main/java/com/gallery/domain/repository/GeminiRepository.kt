package com.gallery.domain.repository

import android.graphics.Bitmap

sealed interface GeminiResult {
    data class Success(val bitmap: Bitmap) : GeminiResult
    data class Error(val message: String) : GeminiResult
}

interface GeminiRepository {
    /**
     * [cacheKey] should uniquely identify (operation + params + source image version) so repeated
     * calls with unchanged inputs return the cached result instead of hitting the network again.
     */
    suspend fun editImage(cacheKey: String, prompt: String, images: List<Bitmap>): GeminiResult
}
