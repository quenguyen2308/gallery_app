package com.gallery.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gallery.R
import com.gallery.ui.navigation.GalleryDestinations

@Composable
fun GalleryBottomNav(navController: NavController, modifier: Modifier = Modifier) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    FloatingBottomBar(
        modifier = modifier,
    ) {
        val isPhotos = currentRoute == GalleryDestinations.PHOTOS
        val isAlbums = currentRoute == GalleryDestinations.ALBUMS
        val isFavorites = currentRoute == GalleryDestinations.FAVORITES

        PillNavItem(
            selected = isPhotos,
            icon = Icons.Rounded.Image,
            label = "Gallery",
            modifier = Modifier.weight(1f),
            onClick = {
                navController.navigate(GalleryDestinations.PHOTOS) {
                    popUpTo(GalleryDestinations.PHOTOS) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
        PillNavItem(
            selected = isAlbums,
            icon = Icons.Rounded.Collections,
            label = stringResource(R.string.nav_albums),
            modifier = Modifier.weight(1f),
            onClick = {
                navController.navigate(GalleryDestinations.ALBUMS) {
                    popUpTo(GalleryDestinations.PHOTOS)
                    launchSingleTop = true
                }
            },
        )
        PillNavItem(
            selected = isFavorites,
            icon = Icons.Rounded.Favorite,
            label = stringResource(R.string.favorites_title),
            modifier = Modifier.weight(1f),
            onClick = {
                navController.navigate(GalleryDestinations.FAVORITES) {
                    popUpTo(GalleryDestinations.PHOTOS)
                    launchSingleTop = true
                }
            },
        )
    }
}
