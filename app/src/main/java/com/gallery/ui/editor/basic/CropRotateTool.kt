package com.gallery.ui.editor.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.ui.editor.EditorViewModel
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.RectCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty

private data class RatioOption(val label: String, val ratio: AspectRatio?)

@Composable
fun CropRotateTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val bitmap = baseBitmap ?: return
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    val freeLabel = stringResource(R.string.crop_ratio_free)
    val ratioOptions = remember(freeLabel) {
        listOf(
            RatioOption(freeLabel, null),
            RatioOption("1:1", AspectRatio(1f)),
            RatioOption("16:9", AspectRatio(16f / 9f)),
            RatioOption("4:3", AspectRatio(4f / 3f)),
            RatioOption("3:2", AspectRatio(3f / 2f)),
        )
    }
    var selectedRatio by remember(freeLabel) { mutableStateOf(ratioOptions[0]) }
    var triggerCrop by remember { mutableStateOf(false) }

    val handleSizePx = with(LocalDensity.current) { 20.dp.toPx() }
    val cropProperties = remember(selectedRatio, handleSizePx) {
        CropDefaults.properties(
            handleSize = handleSizePx,
            cropOutlineProperty = CropOutlineProperty(OutlineType.Rect, RectCropShape(0, "Rect")),
            aspectRatio = selectedRatio.ratio ?: AspectRatio.Original,
            fixedAspectRatio = selectedRatio.ratio != null,
        )
    }
    val cropStyle = remember { CropDefaults.style() }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
        ) {
            ImageCropper(
                modifier = Modifier.fillMaxSize(),
                imageBitmap = imageBitmap,
                contentDescription = null,
                cropStyle = cropStyle,
                cropProperties = cropProperties,
                crop = triggerCrop,
                onCropStart = {},
                onCropSuccess = { cropped ->
                    triggerCrop = false
                    viewModel.applyCrop(cropped.asAndroidBitmap())
                },
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        ) {
            items(ratioOptions) { option ->
                FilterChip(
                    selected = option == selectedRatio,
                    onClick = { selectedRatio = option },
                    label = { Text(option.label) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = { viewModel.rotate(clockwise = false) }) {
                Icon(Icons.AutoMirrored.Rounded.RotateLeft, contentDescription = stringResource(R.string.crop_rotate_left))
            }
            IconButton(onClick = { viewModel.rotate(clockwise = true) }) {
                Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = stringResource(R.string.crop_rotate_right))
            }
            IconButton(onClick = { viewModel.flip(horizontal = true) }) {
                Icon(Icons.Rounded.Flip, contentDescription = stringResource(R.string.crop_flip_horizontal))
            }
            IconButton(onClick = { viewModel.flip(horizontal = false) }) {
                Icon(
                    Icons.Rounded.Flip,
                    contentDescription = stringResource(R.string.crop_flip_vertical),
                    modifier = Modifier.rotate(90f),
                )
            }
            Button(onClick = { triggerCrop = true }) { Text(stringResource(R.string.crop_apply)) }
        }
    }
}
