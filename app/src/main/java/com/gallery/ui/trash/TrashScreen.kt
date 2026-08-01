package com.gallery.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gallery.R
import com.gallery.domain.model.TrashItem
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.ConfirmDialog
import com.gallery.ui.components.FloatingBottomBarClearance
import com.gallery.ui.photos.SelectionDot
import com.gallery.ui.selection.SelectionTopBar
import com.gallery.ui.selection.TrashSelectionActionBar
import com.gallery.ui.theme.ThumbnailShape

@Composable
fun TrashScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
) {
    val trashItems by viewModel.trash.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    // selectedIds is shared across every screen; intersect with this screen's own ids before any
    // destructive call (especially deleteForever, which is permanent) so a stale id from another
    // screen can never slip through here.
    val safeIds = remember(trashItems, selectedIds) {
        val trashIds = trashItems.map { it.media.id }.toSet()
        selectedIds.filterTo(mutableSetOf()) { it in trashIds }
    }

    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }
    var showDeleteForeverConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    isAllSelected = trashItems.isNotEmpty() && selectedIds.containsAll(trashItems.map { it.media.id }),
                    onToggleSelectAll = { viewModel.toggleGroupSelection(trashItems.map { it.media.id }) },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.trash_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) }
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (trashItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_photos))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 6.dp, bottom = FloatingBottomBarClearance),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                ) {
                    items(trashItems, key = { it.media.id }) { trashItem ->
                        TrashThumbnail(
                            trashItem = trashItem,
                            isSelected = trashItem.media.id in selectedIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelected(trashItem.media.id)
                                } else {
                                    viewModel.enterSelection(trashItem.media.id)
                                }
                            },
                            onLongClick = { if (!selectionMode) viewModel.enterSelection(trashItem.media.id) },
                        )
                    }
                }
            }

            if (selectionMode && safeIds.isNotEmpty()) {
                TrashSelectionActionBar(
                    onRestore = { viewModel.restoreFromTrash(safeIds) },
                    onDeleteForever = { showDeleteForeverConfirm = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    if (showDeleteForeverConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_forever_title),
            message = stringResource(R.string.confirm_delete_forever_message, safeIds.size),
            confirmLabel = stringResource(R.string.action_delete_forever),
            onDismiss = { showDeleteForeverConfirm = false },
            onConfirm = { viewModel.deleteForever(safeIds) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashThumbnail(
    trashItem: TrashItem,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(ThumbnailShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!selectionMode) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    onLongClick()
                },
            ),
    ) {
        AsyncImage(
            model = trashItem.media.uri,
            contentDescription = trashItem.media.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = stringResource(R.string.trash_days_left, trashItem.daysLeft),
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(MaterialTheme.shapes.small)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        if (selectionMode) {
            SelectionDot(
                isSelected = isSelected,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
    }
}
