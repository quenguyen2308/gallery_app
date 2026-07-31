package com.gallery.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gallery.ui.GalleryUiEvent
import com.gallery.ui.GalleryViewModel
import com.gallery.ui.albums.AlbumDetailScreen
import com.gallery.ui.albums.AlbumsScreen
import com.gallery.ui.components.GalleryBottomNav
import com.gallery.ui.favorites.FavoritesScreen
import com.gallery.ui.photos.PhotosScreen
import com.gallery.ui.secure.SecureFolderScreen
import com.gallery.ui.trash.TrashScreen
import com.gallery.ui.viewer.ImageViewerScreen

@Composable
fun GalleryApp() {
    val viewModel: GalleryViewModel = hiltViewModel()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onDeleteConfirmed(pendingDeleteIds)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GalleryUiEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is GalleryUiEvent.RequestDeleteConfirmation -> {
                    pendingDeleteIds = event.mediaIds
                    deleteLauncher.launch(IntentSenderRequest.Builder(event.intentSender).build())
                }
            }
        }
    }

    // GalleryBottomNav is an overlay (not the Scaffold `bottomBar` slot) so the grid beneath it
    // isn't clipped short — content scrolls fully behind the floating pill instead of stopping
    // at a solid-background gap the size of the reserved bottomBar area.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = GalleryDestinations.PHOTOS,
                modifier = Modifier.padding(padding),
            ) {
                composable(GalleryDestinations.PHOTOS) {
                    PhotosScreen(
                        viewModel = viewModel,
                        onOpenViewer = { id, list ->
                            viewModel.setViewerContext(list)
                            navController.navigate(GalleryDestinations.viewerRoute(id))
                        },
                    )
                }
                composable(GalleryDestinations.ALBUMS) {
                    AlbumsScreen(
                        viewModel = viewModel,
                        onOpenAlbum = { albumId -> navController.navigate(GalleryDestinations.albumDetailRoute(albumId)) },
                        onOpenFavorites = { navController.navigate(GalleryDestinations.FAVORITES) },
                        onOpenTrash = { navController.navigate(GalleryDestinations.TRASH) },
                        onOpenSecureFolder = { navController.navigate(GalleryDestinations.SECURE_FOLDER) },
                    )
                }
                composable(GalleryDestinations.ALBUM_DETAIL) { entry ->
                    val albumIdArg = entry.arguments?.getString("albumIdArg")
                    if (albumIdArg == null) {
                        navController.popBackStack()
                    } else {
                        val albumId = decodeAlbumId(albumIdArg)
                        val albums by viewModel.albums.collectAsStateWithLifecycle()
                        val albumName = albums.find { it.id == albumId }?.name.orEmpty()
                        AlbumDetailScreen(
                            albumId = albumId,
                            albumName = albumName,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenViewer = { id, list ->
                                viewModel.setViewerContext(list)
                                navController.navigate(GalleryDestinations.viewerRoute(id))
                            },
                        )
                    }
                }
                composable(GalleryDestinations.VIEWER) { entry ->
                    val mediaId = entry.arguments?.getString("mediaId")?.toLongOrNull()
                    if (mediaId == null) {
                        navController.popBackStack()
                    } else {
                        val viewerItems by viewModel.viewerItems.collectAsStateWithLifecycle()
                        ImageViewerScreen(
                            items = viewerItems,
                            startMediaId = mediaId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenEditor = { id -> navController.navigate(GalleryDestinations.editorRoute(id)) },
                        )
                    }
                }
                composable(GalleryDestinations.EDITOR) { entry ->
                    val mediaId = entry.arguments?.getString("mediaId")?.toLongOrNull()
                    if (mediaId == null) {
                        navController.popBackStack()
                    } else {
                        com.gallery.ui.editor.EditorScreen(
                            mediaId = mediaId,
                            onBack = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }
                }
                composable(GalleryDestinations.FAVORITES) {
                    FavoritesScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenViewer = { id, list ->
                            viewModel.setViewerContext(list)
                            navController.navigate(GalleryDestinations.viewerRoute(id))
                        },
                    )
                }
                composable(GalleryDestinations.TRASH) {
                    TrashScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable(GalleryDestinations.SECURE_FOLDER) {
                    SecureFolderScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
            }

            if (!selectionMode && (currentRoute == GalleryDestinations.PHOTOS || currentRoute == GalleryDestinations.ALBUMS)) {
                GalleryBottomNav(navController, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
