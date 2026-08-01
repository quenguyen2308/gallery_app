package com.gallery.ui.editor.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

private data class EnhanceLevel(val level: Int, val label: String, val description: String)

@Composable
fun AiEnhanceTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val bitmap = baseBitmap ?: return
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    val denoiseLabel = stringResource(R.string.enhance_denoise)
    val upscaleLabel = stringResource(R.string.enhance_upscale)
    val maxQualityLabel = stringResource(R.string.enhance_max_quality)
    val chooseLevel = stringResource(R.string.enhance_choose_level)
    val enhanceLevels = remember(denoiseLabel, upscaleLabel, maxQualityLabel) {
        listOf(
            EnhanceLevel(1, "x1", denoiseLabel),
            EnhanceLevel(2, "x2", upscaleLabel),
            EnhanceLevel(3, "x3", maxQualityLabel),
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
        Text(
            chooseLevel,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            enhanceLevels.forEach { level ->
                OutlinedButton(
                    onClick = { viewModel.runAiEnhance(level.level) },
                    modifier = Modifier.weight(1f),
                ) {
                    Column {
                        Text(level.label)
                        Text(level.description, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
