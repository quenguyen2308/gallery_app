package com.gallery.data.repository

import android.graphics.Bitmap
import android.util.LruCache
import com.gallery.data.remote.gemini.GeminiApiClient
import com.gallery.data.remote.gemini.GeminiApiResult
import com.gallery.domain.repository.GeminiRepository
import com.gallery.domain.repository.GeminiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepositoryImpl @Inject constructor(
    private val apiClient: GeminiApiClient,
) : GeminiRepository {

    private val resultCache = LruCache<String, Bitmap>(10)

    override suspend fun editImage(cacheKey: String, prompt: String, images: List<Bitmap>): GeminiResult {
        resultCache.get(cacheKey)?.let { return GeminiResult.Success(it) }
        return when (val result = apiClient.editImage(prompt, images)) {
            is GeminiApiResult.Success -> {
                resultCache.put(cacheKey, result.bitmap)
                GeminiResult.Success(result.bitmap)
            }
            is GeminiApiResult.Failure -> GeminiResult.Error(result.message)
        }
    }
}
