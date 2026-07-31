package com.gallery.data.remote.gemini

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.gallery.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GeminiApiResult {
    data class Success(val bitmap: Bitmap) : GeminiApiResult
    data class Failure(val message: String) : GeminiApiResult
}

private const val TAG = "GeminiApiClient"

/**
 * Google deprecated the Imagen APIs (shutting down mid-2026) in favor of image generation/editing
 * built directly into Gemini ("Nano Banana" models). This surface is new and still evolving — the
 * model name is read from `GEMINI_MODEL` in local.properties (see BuildConfig) so it can be
 * swapped without a source change; response field-name parsing below is the other place to adjust
 * if Google's schema shifts.
 */
@Singleton
class GeminiApiClient @Inject constructor() {

    private val modelName: String
        get() = BuildConfig.GEMINI_MODEL.ifBlank { "gemini-2.5-flash-image" }
    private val endpoint: String
        get() = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    suspend fun editImage(prompt: String, images: List<Bitmap>): GeminiApiResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return@withContext GeminiApiResult.Failure("Chưa cấu hình GEMINI_API_KEY")

        val requestBody = buildRequestBody(prompt, images)

        try {
            val connection = (URL("$endpoint?key=$apiKey").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 90_000
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()

            if (responseCode !in 200..299) {
                val message = extractErrorMessage(responseText) ?: "Lỗi API Gemini (mã $responseCode)"
                Log.w(TAG, "Gemini call failed: HTTP $responseCode — $message")
                return@withContext GeminiApiResult.Failure(message)
            }

            val bitmap = extractImage(responseText)
            if (bitmap == null) {
                Log.w(TAG, "Gemini returned no image part: ${responseText.take(500)}")
                return@withContext GeminiApiResult.Failure("Gemini không trả về ảnh kết quả")
            }
            GeminiApiResult.Success(bitmap)
        } catch (e: IOException) {
            Log.e(TAG, "IOException calling Gemini", e)
            GeminiApiResult.Failure("Không thể kết nối tới Gemini API: ${e.message}")
        }
    }

    private fun buildRequestBody(prompt: String, images: List<Bitmap>): JSONObject {
        val parts = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            images.forEach { bitmap ->
                put(
                    JSONObject().put(
                        "inline_data",
                        JSONObject()
                            .put("mime_type", "image/jpeg")
                            .put("data", bitmap.toBase64Jpeg()),
                    ),
                )
            }
        }
        return JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            put("generationConfig", JSONObject().put("responseModalities", JSONArray(listOf("TEXT", "IMAGE"))))
        }
    }

    private fun extractErrorMessage(responseText: String): String? = try {
        JSONObject(responseText).optJSONObject("error")?.optString("message")
    } catch (e: Exception) {
        null
    }

    private fun extractImage(responseText: String): Bitmap? = try {
        val candidates = JSONObject(responseText).optJSONArray("candidates")
        var found: Bitmap? = null
        if (candidates != null) {
            outer@ for (i in 0 until candidates.length()) {
                val parts = candidates.getJSONObject(i).optJSONObject("content")?.optJSONArray("parts") ?: continue
                for (j in 0 until parts.length()) {
                    val part = parts.getJSONObject(j)
                    val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                    val data = inlineData?.optString("data")
                    if (!data.isNullOrEmpty()) {
                        val bytes = Base64.decode(data, Base64.DEFAULT)
                        found = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        break@outer
                    }
                }
            }
        }
        found
    } catch (e: Exception) {
        null
    }

    private fun Bitmap.toBase64Jpeg(): String {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
