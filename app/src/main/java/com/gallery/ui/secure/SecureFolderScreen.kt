package com.gallery.ui.secure

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.R
import com.gallery.domain.model.SecureMediaItem
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.components.FloatingBottomBarClearance
import com.gallery.ui.photos.SelectionDot
import com.gallery.ui.theme.ThumbnailShape
import com.gallery.ui.util.authenticateWithBiometrics

@Composable
fun SecureFolderScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
) {
    val activity = LocalContext.current as FragmentActivity
    var unlocked by remember { mutableStateOf(false) }
    var authFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authenticateWithBiometrics(
            activity = activity,
            title = "Xác thực để mở Secure Folder",
            onSuccess = { unlocked = true },
            onError = { authFailed = true },
        )
    }

    if (authFailed && !unlocked) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    if (!unlocked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val secureItems by viewModel.secureItems.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    // selectedIds is shared across every screen; intersect with this screen's own ids before any
    // destructive call so a stale id from another screen can never slip through here.
    val safeIds = remember(secureItems, selectedIds) {
        val secureIds = secureItems.map { it.mediaId }.toSet()
        selectedIds.filterTo(mutableSetOf()) { it in secureIds }
    }

    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    Scaffold(
        topBar = {
            if (selectionMode) {
                com.gallery.ui.selection.SelectionTopBar(
                    selectedCount = selectedIds.size,
                    isAllSelected = secureItems.isNotEmpty() && selectedIds.containsAll(secureItems.map { it.mediaId }),
                    onToggleSelectAll = { viewModel.toggleGroupSelection(secureItems.map { it.mediaId }) },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.secure_folder_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null) }
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (secureItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.padding(bottom = 8.dp))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 6.dp, bottom = FloatingBottomBarClearance),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                ) {
                    items(secureItems, key = { it.mediaId }) { item ->
                        SecureThumbnail(
                            item = item,
                            isSelected = item.mediaId in selectedIds,
                            selectionMode = selectionMode,
                            decryptThumbnail = { viewModel.decryptSecureThumbnail(item.mediaId) },
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelected(item.mediaId)
                                } else {
                                    viewModel.enterSelection(item.mediaId)
                                }
                            },
                            onLongClick = { if (!selectionMode) viewModel.enterSelection(item.mediaId) },
                        )
                    }
                }
            }

            if (selectionMode && safeIds.isNotEmpty()) {
                com.gallery.ui.selection.SecureSelectionActionBar(
                    onRestore = { viewModel.restoreFromSecureFolder(safeIds) },
                    modifier = Modifier.align(Alignment.BottomCenter).wrapContentWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SecureThumbnail(
    item: SecureMediaItem,
    isSelected: Boolean,
    selectionMode: Boolean,
    decryptThumbnail: suspend () -> Bitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var bitmap by remember(item.mediaId) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.mediaId) {
        bitmap = decryptThumbnail()
    }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(ThumbnailShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (item.isVideo) Icons.Rounded.Movie else Icons.Rounded.Lock,
                    contentDescription = null,
                )
            }
        }
        if (selectionMode) {
            SelectionDot(
                isSelected = isSelected,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
    }
}
