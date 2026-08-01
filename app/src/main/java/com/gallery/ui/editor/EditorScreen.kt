package com.gallery.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.ui.components.FloatingBottomBar
import com.gallery.ui.components.PillNavItem
import com.gallery.ui.editor.ai.AiEnhanceTool
import com.gallery.ui.editor.ai.BackgroundReplaceTool
import com.gallery.ui.editor.ai.GeminiProcessingOverlay
import com.gallery.ui.editor.ai.MagicEraserTool
import com.gallery.ui.editor.ai.StyleTransferTool
import com.gallery.ui.editor.basic.AdjustTool
import com.gallery.ui.editor.basic.CropRotateTool
import com.gallery.ui.editor.basic.FilterTool

private enum class EditorTopTab { BASIC, AI }
private enum class BasicTool { CROP, ADJUST, FILTER }
private enum class AiTool { ERASER, BACKGROUND, ENHANCE, STYLE }

@Composable
fun EditorScreen(
    mediaId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    LaunchedEffect(mediaId) { viewModel.load(mediaId) }

    var topTab by remember { mutableStateOf(EditorTopTab.BASIC) }
    var basicTool by remember { mutableStateOf(BasicTool.CROP) }
    var aiTool by remember { mutableStateOf(AiTool.ERASER) }
    var showSaveOptions by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val baseBitmap by viewModel.baseBitmap.collectAsStateWithLifecycle()
    val processingLabel by viewModel.processingLabel.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is EditorEvent.Saved -> {
                    snackbarHostState.showSnackbar(if (event.overwritten) "Đã lưu" else "Đã lưu thành ảnh mới")
                    onSaved()
                }
            }
        }
    }

    // Leaving Adjust/Filter bakes the pending slider changes so Undo has a clean checkpoint.
    var previousTool by remember { mutableStateOf(basicTool) }
    LaunchedEffect(basicTool) {
        if (previousTool != basicTool && previousTool != BasicTool.CROP) {
            viewModel.commitCurrentEdits()
        }
        previousTool = basicTool
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = canUndo && !isBusy) {
                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Hoàn tác")
                    }
                    IconButton(onClick = viewModel::redo, enabled = canRedo && !isBusy) {
                        Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Làm lại")
                    }
                    IconButton(onClick = { showSaveOptions = true }, enabled = !isBusy && baseBitmap != null) {
                        Icon(Icons.Rounded.Check, contentDescription = "Lưu")
                    }
                },
            )
        },
    ) { padding ->
        if (isLoading || baseBitmap == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = topTab.ordinal) {
                    Tab(
                        selected = topTab == EditorTopTab.BASIC,
                        onClick = { topTab = EditorTopTab.BASIC },
                        text = { Text("Cơ bản") },
                    )
                    Tab(
                        selected = topTab == EditorTopTab.AI,
                        onClick = { topTab = EditorTopTab.AI },
                        text = { Text("AI Editing") },
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (topTab == EditorTopTab.AI) {
                        when (aiTool) {
                            AiTool.ERASER -> MagicEraserTool(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            AiTool.BACKGROUND -> BackgroundReplaceTool(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            AiTool.ENHANCE -> AiEnhanceTool(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            AiTool.STYLE -> StyleTransferTool(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        when (basicTool) {
                            BasicTool.CROP -> CropRotateTool(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            BasicTool.ADJUST -> AdjustTool(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                            BasicTool.FILTER -> FilterTool(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                if (topTab == EditorTopTab.BASIC) {
                    FloatingBottomBar {
                        PillNavItem(
                            selected = basicTool == BasicTool.CROP,
                            icon = Icons.Rounded.Crop,
                            label = "Cắt/Xoay",
                            onClick = { basicTool = BasicTool.CROP },
                        )
                        PillNavItem(
                            selected = basicTool == BasicTool.ADJUST,
                            icon = Icons.Rounded.Tune,
                            label = "Điều chỉnh",
                            onClick = { basicTool = BasicTool.ADJUST },
                        )
                        PillNavItem(
                            selected = basicTool == BasicTool.FILTER,
                            icon = Icons.Rounded.FilterVintage,
                            label = "Bộ lọc",
                            onClick = { basicTool = BasicTool.FILTER },
                        )
                    }
                } else {
                    FloatingBottomBar {
                        PillNavItem(
                            selected = aiTool == AiTool.ERASER,
                            icon = Icons.Rounded.AutoFixHigh,
                            label = "Xóa vật thể",
                            onClick = { aiTool = AiTool.ERASER },
                        )
                        PillNavItem(
                            selected = aiTool == AiTool.BACKGROUND,
                            icon = Icons.Rounded.Wallpaper,
                            label = "Nền",
                            onClick = { aiTool = AiTool.BACKGROUND },
                        )
                        PillNavItem(
                            selected = aiTool == AiTool.ENHANCE,
                            icon = Icons.Rounded.HighQuality,
                            label = "Nâng cấp",
                            onClick = { aiTool = AiTool.ENHANCE },
                        )
                        PillNavItem(
                            selected = aiTool == AiTool.STYLE,
                            icon = Icons.Rounded.Palette,
                            label = "Phong cách",
                            onClick = { aiTool = AiTool.STYLE },
                        )
                    }
                }
            }

            processingLabel?.let { label -> GeminiProcessingOverlay(label = label) }
        }
    }

    if (showSaveOptions) {
        Dialog(onDismissRequest = { showSaveOptions = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                    ) {
                        Text(
                            text = "Lưu ảnh",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ghi đè lên ảnh gốc hay lưu thành một ảnh mới?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { showSaveOptions = false; viewModel.save(overwrite = true) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Ghi đè", fontWeight = FontWeight.Bold)
                        }
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        TextButton(
                            onClick = { showSaveOptions = false; viewModel.save(overwrite = false) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Lưu bản mới", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
