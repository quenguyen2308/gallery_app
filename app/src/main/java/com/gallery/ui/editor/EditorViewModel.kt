package com.gallery.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ColorMatrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.repository.EditSaveResult
import com.gallery.domain.repository.GeminiRepository
import com.gallery.domain.repository.GeminiResult
import com.gallery.domain.repository.MediaRepository
import com.gallery.util.isNetworkAvailable
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AdjustParams(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val sharpness: Float = 0f,
)

sealed interface EditorEvent {
    data class Message(val text: String) : EditorEvent
    data class Saved(val overwritten: Boolean) : EditorEvent
}

private const val MAX_HISTORY = 12

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaRepository,
    private val geminiRepository: GeminiRepository,
) : ViewModel() {

    private var originalMediaId: Long = -1L
    private val history = mutableListOf<Bitmap>()
    private var historyIndex = -1

    /** Bumped on every committed edit so Gemini cache keys change when the source image changes. */
    private var historyVersion = 0

    private val _processingLabel = MutableStateFlow<String?>(null)
    val processingLabel: StateFlow<String?> = _processingLabel.asStateFlow()

    private val _baseBitmap = MutableStateFlow<Bitmap?>(null)
    val baseBitmap: StateFlow<Bitmap?> = _baseBitmap.asStateFlow()

    /** Base bitmap with sharpening baked in (recomputed on slider release, not per-frame). */
    private val _previewBaseBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBaseBitmap: StateFlow<Bitmap?> = _previewBaseBitmap.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _adjustParams = MutableStateFlow(AdjustParams())
    val adjustParams: StateFlow<AdjustParams> = _adjustParams.asStateFlow()

    private val _filterPreset = MutableStateFlow(FilterPreset.NONE)
    val filterPreset: StateFlow<FilterPreset> = _filterPreset.asStateFlow()
    private val _filterIntensity = MutableStateFlow(1f)
    val filterIntensity: StateFlow<Float> = _filterIntensity.asStateFlow()

    /** Cheap live-preview matrix (brightness/contrast/saturation + filter) applied via ColorFilter. */
    val previewColorMatrix: StateFlow<ColorMatrix> = combine(
        _adjustParams, _filterPreset, _filterIntensity,
    ) { adjust, preset, intensity ->
        ImageEffects.toComposeColorMatrix(
            ImageEffects.combinedMatrix(adjust.brightness, adjust.contrast, adjust.saturation, preset, intensity),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ColorMatrix())

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    fun load(mediaId: Long) {
        if (originalMediaId == mediaId && history.isNotEmpty()) return
        originalMediaId = mediaId
        viewModelScope.launch {
            _isLoading.value = true
            val item = repository.observeMediaItem(mediaId).first()
            val bitmap = item?.let {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it.uri)?.use(BitmapFactory::decodeStream)
                }
            }
            if (bitmap == null) {
                _events.send(EditorEvent.Message("Không thể mở ảnh để chỉnh sửa"))
                _isLoading.value = false
                return@launch
            }
            val capped = ImageEffects.capSize(bitmap)
            history.clear()
            history.add(capped)
            historyIndex = 0
            _baseBitmap.value = capped
            _previewBaseBitmap.value = capped
            resetTransientParams()
            updateUndoRedoFlags()
            _isLoading.value = false
        }
    }

    fun updateBrightness(value: Float) {
        _adjustParams.value = _adjustParams.value.copy(brightness = value)
    }

    fun updateContrast(value: Float) {
        _adjustParams.value = _adjustParams.value.copy(contrast = value)
    }

    fun updateSaturation(value: Float) {
        _adjustParams.value = _adjustParams.value.copy(saturation = value)
    }

    fun updateSharpness(value: Float) {
        _adjustParams.value = _adjustParams.value.copy(sharpness = value)
    }

    /** Recomputes the sharpened working bitmap — call on slider release, not on every drag tick. */
    fun refreshSharpnessPreview() = viewModelScope.launch {
        val base = _baseBitmap.value ?: return@launch
        val sharpness = _adjustParams.value.sharpness
        _isBusy.value = true
        _previewBaseBitmap.value = if (sharpness > 0f) ImageEffects.sharpen(base, sharpness) else base
        _isBusy.value = false
    }

    fun selectFilter(preset: FilterPreset) {
        _filterPreset.value = preset
        if (preset != FilterPreset.NONE && _filterIntensity.value <= 0f) _filterIntensity.value = 1f
    }

    fun updateFilterIntensity(value: Float) {
        _filterIntensity.value = value
    }

    /** Bakes any pending Adjust/Filter changes into the history. No-op if nothing changed. */
    fun commitCurrentEdits() = viewModelScope.launch { commitCurrentEditsSuspend() }

    private suspend fun commitCurrentEditsSuspend() {
        val params = _adjustParams.value
        val preset = _filterPreset.value
        val intensity = _filterIntensity.value
        val hasChange = params != AdjustParams() || preset != FilterPreset.NONE
        if (!hasChange) return
        val source = _previewBaseBitmap.value ?: _baseBitmap.value ?: return
        _isBusy.value = true
        val matrix = ImageEffects.combinedMatrix(params.brightness, params.contrast, params.saturation, preset, intensity)
        val baked = ImageEffects.bake(source, matrix)
        pushHistory(baked)
        _isBusy.value = false
    }

    fun applyCrop(croppedBitmap: Bitmap) = viewModelScope.launch {
        commitCurrentEditsSuspend()
        pushHistory(croppedBitmap)
    }

    fun rotate(clockwise: Boolean) = viewModelScope.launch {
        commitCurrentEditsSuspend()
        val base = _baseBitmap.value ?: return@launch
        _isBusy.value = true
        val rotated = withContext(Dispatchers.Default) { ImageEffects.rotate(base, if (clockwise) 90f else -90f) }
        pushHistory(rotated)
        _isBusy.value = false
    }

    fun flip(horizontal: Boolean) = viewModelScope.launch {
        commitCurrentEditsSuspend()
        val base = _baseBitmap.value ?: return@launch
        _isBusy.value = true
        val flipped = withContext(Dispatchers.Default) { ImageEffects.flip(base, horizontal) }
        pushHistory(flipped)
        _isBusy.value = false
    }

    fun undo() {
        if (historyIndex <= 0) return
        historyIndex--
        applyHistoryPointer()
    }

    fun redo() {
        if (historyIndex >= history.size - 1) return
        historyIndex++
        applyHistoryPointer()
    }

    private fun applyHistoryPointer() {
        val bitmap = history[historyIndex]
        _baseBitmap.value = bitmap
        _previewBaseBitmap.value = bitmap
        resetTransientParams()
        updateUndoRedoFlags()
    }

    fun save(overwrite: Boolean) = viewModelScope.launch {
        commitCurrentEditsSuspend()
        val finalBitmap = _baseBitmap.value ?: return@launch
        _isBusy.value = true
        when (val result = repository.saveEditedImage(originalMediaId, finalBitmap, overwrite, "basic", null)) {
            is EditSaveResult.Success -> _events.send(EditorEvent.Saved(result.overwritten))
            is EditSaveResult.Error -> _events.send(EditorEvent.Message(result.message))
        }
        _isBusy.value = false
    }

    fun runMagicEraser(maskBitmap: Bitmap) = runGeminiOperation(
        cacheKeySuffix = "erase",
        label = "Gemini đang xóa vật thể...",
        prompt = "Remove the object marked in white on the second (mask) image from the first image. " +
            "Fill the erased area naturally so it blends seamlessly with the surrounding background. " +
            "Return only the edited photo, keeping everything else unchanged.",
        extraImages = listOf(maskBitmap),
    )

    fun runBackgroundReplace(prompt: String) = runGeminiOperation(
        cacheKeySuffix = "bg:$prompt",
        label = "Gemini đang thay nền...",
        prompt = "Keep the main subject of this photo exactly as-is, and replace the background with: " +
            "$prompt. Blend lighting and edges naturally. Return only the edited photo.",
    )

    fun runAiEnhance(level: Int) = runGeminiOperation(
        cacheKeySuffix = "enhance:$level",
        label = "Gemini đang nâng cấp chất lượng...",
        prompt = when (level) {
            1 -> "Denoise this photo and slightly improve clarity while keeping it fully realistic and unchanged in content."
            2 -> "Upscale and sharpen this photo, enhancing fine detail and clarity while keeping it fully realistic."
            else -> "Upscale this photo to maximum quality: enhance detail, sharpness, and clarity as much as possible " +
                "while keeping it fully realistic and unchanged in content."
        },
    )

    fun runStyleTransfer(styleLabel: String, stylePrompt: String) = runGeminiOperation(
        cacheKeySuffix = "style:$styleLabel",
        label = "Gemini đang áp dụng phong cách $styleLabel...",
        prompt = "Redraw this photo in the following art style: $stylePrompt. Keep the composition and subject " +
            "recognizable. Return only the resulting image.",
    )

    private fun runGeminiOperation(
        cacheKeySuffix: String,
        label: String,
        prompt: String,
        extraImages: List<Bitmap> = emptyList(),
    ) = viewModelScope.launch {
        commitCurrentEditsSuspend()
        val base = _baseBitmap.value ?: return@launch

        if (!isNetworkAvailable(context)) {
            _events.send(EditorEvent.Message("Cần kết nối mạng để dùng AI Editing"))
            return@launch
        }

        _processingLabel.value = label
        val cacheKey = "$historyVersion:$cacheKeySuffix"
        when (val result = geminiRepository.editImage(cacheKey, prompt, listOf(base) + extraImages)) {
            is GeminiResult.Success -> pushHistory(result.bitmap)
            is GeminiResult.Error -> _events.send(EditorEvent.Message(result.message))
        }
        _processingLabel.value = null
    }

    private fun resetTransientParams() {
        _adjustParams.value = AdjustParams()
        _filterPreset.value = FilterPreset.NONE
        _filterIntensity.value = 1f
    }

    private fun pushHistory(bitmap: Bitmap) {
        while (history.size > historyIndex + 1) history.removeAt(history.size - 1)
        history.add(bitmap)
        if (history.size > MAX_HISTORY) {
            history.removeAt(0)
        }
        historyIndex = history.size - 1
        historyVersion++
        _baseBitmap.value = bitmap
        _previewBaseBitmap.value = bitmap
        resetTransientParams()
        updateUndoRedoFlags()
    }

    private fun updateUndoRedoFlags() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < history.size - 1
    }
}
