package com.gallery.ui.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gallery.R
import com.gallery.ui.components.FloatingBottomBar
import com.gallery.ui.components.PillActionItem
import com.gallery.ui.theme.SelectionOverlay

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
                tint = if (isAllSelected) SelectionOverlay else MaterialTheme.colorScheme.onSurfaceVariant,
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
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingBottomBar(modifier = modifier) {
        PillActionItem(Icons.Rounded.Share, stringResource(R.string.action_share), onShare)
        PillActionItem(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(R.string.action_favorite),
            onClick = onToggleFavorite,
            tint = if (isFavorite) Color(0xFFB83030) else Color(0xFF3A3C40),
        )
        PillActionItem(Icons.Rounded.Delete, stringResource(R.string.action_delete), onDelete, tint = Color(0xFFA83636))
        PillActionItem(Icons.Rounded.MoreVert, stringResource(R.string.action_more), onMore)
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
        PillActionItem(Icons.Rounded.DeleteForever, stringResource(R.string.action_delete_forever), onDeleteForever, tint = Color(0xFFA83636)) //androidx.compose.ui.graphics.Color.Red
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
