package com.gallery.ui.editor.basic

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.ui.editor.EditorViewModel
import com.gallery.ui.editor.FilterPreset
import com.gallery.ui.editor.ImageEffects

@Composable
fun FilterTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val previewBaseBitmap by viewModel.previewBaseBitmap.collectAsStateWithLifecycle()
    val initialBitmap by viewModel.initialBitmap.collectAsStateWithLifecycle()
    val matrix by viewModel.previewColorMatrix.collectAsStateWithLifecycle()
    val preset by viewModel.filterPreset.collectAsStateWithLifecycle()
    val intensity by viewModel.filterIntensity.collectAsStateWithLifecycle()
    val bitmap = previewBaseBitmap ?: baseBitmap ?: return
    var isComparing by remember { mutableStateOf(false) }
    val thumbBitmap = remember(bitmap) {
        Bitmap.createScaledBitmap(bitmap, 72, 72, true).asImageBitmap()
    }
    val filterNoneLabel = stringResource(R.string.filter_none)

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

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            items(FilterPreset.entries.toList()) { filter ->
                val label = if (filter == FilterPreset.NONE) filterNoneLabel else filter.label
                FilterThumbnail(
                    thumb = thumbBitmap,
                    filter = filter,
                    label = label,
                    isSelected = filter == preset,
                    onClick = { viewModel.selectFilter(filter) },
                )
            }
        }

        if (preset != FilterPreset.NONE) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.filter_intensity), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = intensity,
                    onValueChange = viewModel::updateFilterIntensity,
                    valueRange = 0f..1f,
                )
            }
        }
    }
}

@Composable
private fun FilterThumbnail(
    thumb: androidx.compose.ui.graphics.ImageBitmap,
    filter: FilterPreset,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val filterMatrix = remember(filter) {
        ColorFilter.colorMatrix(ImageEffects.toComposeColorMatrix(ImageEffects.combinedMatrix(0f, 0f, 0f, filter, 1f)))
    }
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = thumb,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            colorFilter = filterMatrix,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                ),
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
