package com.gallery.ui.editor.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.ui.editor.EditorViewModel

@Composable
fun BackgroundReplaceTool(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val bitmap = baseBitmap ?: return
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    var prompt by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(R.string.bg_replace_label)) },
                placeholder = { Text(stringResource(R.string.bg_replace_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.runBackgroundReplace(prompt.trim()) },
                enabled = prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { Text(stringResource(R.string.bg_replace_action)) }
        }
    }
}
