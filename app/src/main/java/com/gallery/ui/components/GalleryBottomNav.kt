package com.gallery.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gallery.R
import com.gallery.ui.navigation.GalleryDestinations

@Composable
fun GalleryBottomNav(navController: NavController, modifier: Modifier = Modifier) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    FloatingBottomBar(modifier = modifier) {
        PillNavItem(
            selected = currentRoute == GalleryDestinations.PHOTOS,
            icon = Icons.Rounded.Image,
            label = stringResource(R.string.nav_photos),
            onClick = {
                navController.navigate(GalleryDestinations.PHOTOS) {
                    popUpTo(GalleryDestinations.PHOTOS) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
        PillNavItem(
            selected = currentRoute == GalleryDestinations.ALBUMS,
            icon = Icons.Rounded.PhotoAlbum,
            label = stringResource(R.string.nav_albums),
            onClick = {
                navController.navigate(GalleryDestinations.ALBUMS) {
                    popUpTo(GalleryDestinations.PHOTOS)
                    launchSingleTop = true
                }
            },
        )
    }
}
