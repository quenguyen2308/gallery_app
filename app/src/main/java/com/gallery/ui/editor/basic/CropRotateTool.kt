package com.gallery.ui.editor.basic

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
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
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canhub.cropper.CropImageView
import com.gallery.R
import com.gallery.ui.editor.EditorViewModel

private data class RatioOption(val label: String, val aspectX: Int?, val aspectY: Int?)

@Composable
fun CropRotateTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val bitmap = baseBitmap ?: return

    val freeLabel = stringResource(R.string.crop_ratio_free)
    val ratioOptions = remember(freeLabel) {
        listOf(
            RatioOption(freeLabel, null, null),
            RatioOption("1:1", 1, 1),
            RatioOption("16:9", 16, 9),
            RatioOption("4:3", 4, 3),
            RatioOption("3:2", 3, 2),
        )
    }
    var selectedRatio by remember(freeLabel) { mutableStateOf(ratioOptions[0]) }
    var cropImageViewRef by remember { mutableStateOf<CropImageView?>(null) }

    LaunchedEffect(bitmap) {
        cropImageViewRef?.setImageBitmap(bitmap)
    }

    LaunchedEffect(selectedRatio) {
        cropImageViewRef?.let { view ->
            if (selectedRatio.aspectX != null && selectedRatio.aspectY != null) {
                view.setAspectRatio(selectedRatio.aspectX!!, selectedRatio.aspectY!!)
                view.setFixedAspectRatio(true)
            } else {
                view.clearAspectRatio()
                view.setFixedAspectRatio(false)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    CropImageView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundColor(AndroidColor.BLACK)
                        guidelines = CropImageView.Guidelines.ON
                        cropShape = CropImageView.CropShape.RECTANGLE
                        isAutoZoomEnabled = true
                        setMultiTouchEnabled(true)
                        setCenterMoveEnabled(true)
                        isShowProgressBar = false
                        setImageBitmap(bitmap)
                        cropImageViewRef = this
                    }
                },
                update = { view ->
                    cropImageViewRef = view
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
            IconButton(onClick = {
                cropImageViewRef?.rotateImage(-90)
            }) {
                Icon(Icons.AutoMirrored.Rounded.RotateLeft, contentDescription = stringResource(R.string.crop_rotate_left))
            }
            IconButton(onClick = {
                cropImageViewRef?.rotateImage(90)
            }) {
                Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = stringResource(R.string.crop_rotate_right))
            }
            IconButton(onClick = {
                cropImageViewRef?.flipImageHorizontally()
            }) {
                Icon(Icons.Rounded.Flip, contentDescription = stringResource(R.string.crop_flip_horizontal))
            }
            IconButton(onClick = {
                cropImageViewRef?.flipImageVertically()
            }) {
                Icon(
                    Icons.Rounded.Flip,
                    contentDescription = stringResource(R.string.crop_flip_vertical),
                    modifier = Modifier.rotate(90f),
                )
            }
            Button(onClick = {
                val cropped: Bitmap? = cropImageViewRef?.getCroppedImage()
                if (cropped != null) {
                    viewModel.applyCrop(cropped)
                }
            }) {
                Text(stringResource(R.string.crop_apply))
            }
        }
    }
}
