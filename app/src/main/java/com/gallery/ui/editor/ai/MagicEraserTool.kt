package com.gallery.ui.editor.ai

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.ui.editor.EditorViewModel

@Composable
fun MagicEraserTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val bitmap = baseBitmap ?: return
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val density = LocalDensity.current

    var brushSizeDp by remember { mutableFloatStateOf(24f) }
    val strokes = remember(bitmap) { mutableStateListOf<MutableList<Offset>>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                    .fillMaxWidth()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(bitmap) {
                        detectDragGestures(
                            onDragStart = { offset -> strokes.add(mutableStateListOf(offset)) },
                            onDrag = { change, _ -> strokes.lastOrNull()?.add(change.position) },
                        )
                    },
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthPx = brushSizeDp.dp.toPx()
                    strokes.forEach { stroke ->
                        for (i in 0 until stroke.size - 1) {
                            drawLine(
                                color = Color.Red.copy(alpha = 0.55f),
                                start = stroke[i],
                                end = stroke[i + 1],
                                strokeWidth = strokeWidthPx,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Tô lên vùng cần xóa", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = brushSizeDp,
                onValueChange = { brushSizeDp = it },
                valueRange = 5f..50f,
            )
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Button(
                onClick = {
                    val mask = buildMaskBitmap(
                        canvasSize = canvasSize,
                        strokes = strokes,
                        brushWidthPx = with(density) { brushSizeDp.dp.toPx() },
                        targetWidth = bitmap.width,
                        targetHeight = bitmap.height,
                    )
                    strokes.clear()
                    viewModel.runMagicEraser(mask)
                },
                enabled = strokes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Xóa vật thể") }
        }
        if (strokes.isNotEmpty()) {
            TextButton(onClick = { strokes.clear() }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa nét vẽ")
            }
        }
    }
}

private fun buildMaskBitmap(
    canvasSize: IntSize,
    strokes: List<List<Offset>>,
    brushWidthPx: Float,
    targetWidth: Int,
    targetHeight: Int,
): Bitmap {
    val width = canvasSize.width.coerceAtLeast(1)
    val height = canvasSize.height.coerceAtLeast(1)
    val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(maskBitmap)
    canvas.drawColor(android.graphics.Color.BLACK)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = brushWidthPx.coerceAtLeast(1f)
        strokeCap = android.graphics.Paint.Cap.ROUND
        isAntiAlias = true
    }
    strokes.forEach { stroke ->
        for (i in 0 until stroke.size - 1) {
            canvas.drawLine(stroke[i].x, stroke[i].y, stroke[i + 1].x, stroke[i + 1].y, paint)
        }
    }
    return Bitmap.createScaledBitmap(maskBitmap, targetWidth, targetHeight, true)
}
