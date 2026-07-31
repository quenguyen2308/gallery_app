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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.ui.editor.EditorViewModel
import com.gallery.ui.editor.FilterPreset
import com.gallery.ui.editor.ImageEffects

@Composable
fun FilterTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val previewBaseBitmap by viewModel.previewBaseBitmap.collectAsStateWithLifecycle()
    val matrix by viewModel.previewColorMatrix.collectAsStateWithLifecycle()
    val preset by viewModel.filterPreset.collectAsStateWithLifecycle()
    val intensity by viewModel.filterIntensity.collectAsStateWithLifecycle()
    val bitmap = previewBaseBitmap ?: baseBitmap ?: return
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val thumbBitmap = remember(bitmap) {
        Bitmap.createScaledBitmap(bitmap, 72, 72, true).asImageBitmap()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.colorMatrix(matrix),
                modifier = Modifier.fillMaxSize(),
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            items(FilterPreset.entries.toList()) { filter ->
                FilterThumbnail(
                    thumb = thumbBitmap,
                    filter = filter,
                    isSelected = filter == preset,
                    onClick = { viewModel.selectFilter(filter) },
                )
            }
        }

        if (preset != FilterPreset.NONE) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Cường độ", style = MaterialTheme.typography.labelSmall)
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
            contentDescription = filter.label,
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
        Text(filter.label, style = MaterialTheme.typography.labelSmall)
    }
}
