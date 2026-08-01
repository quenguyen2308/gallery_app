package com.gallery.ui.editor.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.ui.editor.EditorViewModel

private data class ArtStyle(val label: String, val prompt: String)

@Composable
fun StyleTransferTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val bitmap = baseBitmap ?: return
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    val oilPainting = stringResource(R.string.style_oil_painting)
    val sketch = stringResource(R.string.style_sketch)
    val watercolor = stringResource(R.string.style_watercolor)
    val artStyles = remember(oilPainting, sketch, watercolor) {
        listOf(
            ArtStyle("Anime", "vibrant Japanese anime illustration, clean line art, cel shading"),
            ArtStyle(oilPainting, "classical oil painting, visible brush strokes, rich texture"),
            ArtStyle(sketch, "black and white pencil sketch, hand-drawn cross-hatching"),
            ArtStyle(watercolor, "soft watercolor painting, gentle color bleeding, paper texture"),
            ArtStyle("Cyberpunk", "neon-lit cyberpunk illustration, high contrast, futuristic city tones"),
            ArtStyle("Ghibli", "Studio Ghibli style hand-painted animation frame, soft pastel colors"),
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(artStyles) { style ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.runStyleTransfer(style.label, style.prompt) },
                    label = { Text(style.label) },
                )
            }
        }
    }
}
