package com.gallery.ui.editor.basic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.ui.editor.EditorViewModel
import kotlin.math.roundToInt

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun AdjustTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val previewBaseBitmap by viewModel.previewBaseBitmap.collectAsStateWithLifecycle()
    val initialBitmap by viewModel.initialBitmap.collectAsStateWithLifecycle()
    val matrix by viewModel.previewColorMatrix.collectAsStateWithLifecycle()
    val params by viewModel.adjustParams.collectAsStateWithLifecycle()
    val bitmap = previewBaseBitmap ?: baseBitmap ?: return
    var isComparing by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isComparing = true
                            tryAwaitRelease()
                            isComparing = false
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val displayBitmap = if (isComparing && initialBitmap != null) initialBitmap!! else bitmap
            val displayColorFilter = if (isComparing) null else ColorFilter.colorMatrix(matrix)

            Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = displayColorFilter,
                modifier = Modifier.fillMaxSize(),
            )

            if (isComparing) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                ) {
                    Text(
                        text = "Ảnh gốc (Original)",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp),
                ) {
                    Text(
                        text = "Nhấn giữ ảnh để so sánh",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            AdjustSlider(stringResource(R.string.adjust_brightness), params.brightness, -100f..100f, viewModel::updateBrightness)
            AdjustSlider(stringResource(R.string.adjust_contrast), params.contrast, -100f..100f, viewModel::updateContrast)
            AdjustSlider(stringResource(R.string.adjust_saturation), params.saturation, -100f..100f, viewModel::updateSaturation)
            AdjustSlider(
                label = stringResource(R.string.adjust_sharpness),
                value = params.sharpness,
                range = 0f..100f,
                onValueChange = viewModel::updateSharpness,
                onValueChangeFinished = viewModel::refreshSharpnessPreview,
            )
        }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(value.roundToInt().toString(), style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}
