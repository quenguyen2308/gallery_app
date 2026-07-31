package com.gallery.ui.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoveToInbox
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gallery.R
import com.gallery.ui.components.FloatingBottomBar
import com.gallery.ui.components.PillActionItem

/**
 * No close button here on purpose: selection mode is only exited via the system back gesture
 * (see the BackHandler wired around each screen that hosts this bar), not by tapping anything in
 * the bar itself — so toggling everything off keeps the select-all circle around to reselect.
 */
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onToggleSelectAll: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.items_selected, selectedCount)) },
        navigationIcon = {
            Icon(
                imageVector = if (isAllSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = stringResource(R.string.action_select_all),
                tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 14.dp)
                    .clickable(onClick = onToggleSelectAll),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(),
    )
}

@Composable
fun SelectionActionBar(
    isFavorite: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingBottomBar(modifier = modifier) {
        PillActionItem(Icons.Rounded.Share, stringResource(R.string.action_share), onShare)
        PillActionItem(Icons.Rounded.Delete, stringResource(R.string.action_delete), onDelete)
        PillActionItem(Icons.Rounded.MoveToInbox, stringResource(R.string.action_move), onMove)
        PillActionItem(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(R.string.action_favorite),
            onClick = onToggleFavorite,
        )
        PillActionItem(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy), onCopy)
        PillActionItem(Icons.Rounded.MoreHoriz, stringResource(R.string.action_more), onMore)
    }
}

@Composable
fun TrashSelectionActionBar(
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingBottomBar(modifier = modifier) {
        PillActionItem(Icons.Rounded.Restore, stringResource(R.string.action_restore), onRestore)
        PillActionItem(Icons.Rounded.DeleteForever, stringResource(R.string.action_delete_forever), onDeleteForever)
    }
}

@Composable
fun SecureSelectionActionBar(
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingBottomBar(modifier = modifier) {
        PillActionItem(Icons.Rounded.Restore, stringResource(R.string.action_restore), onRestore)
    }
}
