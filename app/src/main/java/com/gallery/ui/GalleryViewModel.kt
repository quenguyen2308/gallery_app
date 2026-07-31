package com.gallery.ui

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.Album
import com.gallery.domain.model.AlbumId
import com.gallery.domain.model.MediaItem
import com.gallery.domain.model.SecureMediaItem
import com.gallery.domain.model.TrashItem
import com.gallery.domain.repository.DeleteResult
import com.gallery.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GalleryUiEvent {
    data class Message(val text: String) : GalleryUiEvent
    data class RequestDeleteConfirmation(val intentSender: IntentSender, val mediaIds: List<Long>) : GalleryUiEvent
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: MediaRepository,
) : ViewModel() {

    val mediaItems: StateFlow<List<MediaItem>> = repository.observeMediaItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = repository.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<MediaItem>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trash: StateFlow<List<TrashItem>> = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val secureItems: StateFlow<List<SecureMediaItem>> = repository.observeSecureItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _viewerItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val viewerItems: StateFlow<List<MediaItem>> = _viewerItems.asStateFlow()

    private val _events = Channel<GalleryUiEvent>(Channel.BUFFERED)
    val events: Flow<GalleryUiEvent> = _events.receiveAsFlow()

    fun observeAlbumMedia(albumId: AlbumId): Flow<List<MediaItem>> = repository.observeAlbumMedia(albumId)

    fun setViewerContext(items: List<MediaItem>) {
        _viewerItems.value = items
    }

    fun enterSelection(initialId: Long) {
        _selectionMode.value = true
        _selectedIds.value = setOf(initialId)
    }

    // Selection mode is only ever exited via clearSelection() (wired to the back gesture), never
    // implicitly when the selection count drops to zero — so the select-all circles stay on
    // screen and ready to reselect instead of the whole selection UI disappearing.
    fun toggleSelected(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
    }

    fun clearSelection() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun isGroupFullySelected(groupIds: List<Long>): Boolean =
        groupIds.isNotEmpty() && _selectedIds.value.containsAll(groupIds)

    fun toggleGroupSelection(groupIds: List<Long>) {
        val current = _selectedIds.value
        _selectedIds.value = if (isGroupFullySelected(groupIds)) {
            current - groupIds.toSet()
        } else {
            current + groupIds.toSet()
        }
    }

    fun setFavorite(ids: Collection<Long>, isFavorite: Boolean) = viewModelScope.launch {
        repository.setFavorite(ids.toList(), isFavorite)
    }

    fun moveToTrash(ids: Collection<Long>) = viewModelScope.launch {
        repository.moveToTrash(ids.toList())
        clearSelection()
        _events.send(GalleryUiEvent.Message("Đã chuyển vào thùng rác"))
    }

    fun restoreFromTrash(ids: Collection<Long>) = viewModelScope.launch {
        repository.restoreFromTrash(ids.toList())
        clearSelection()
    }

    fun deleteForever(ids: Collection<Long>) = viewModelScope.launch {
        when (val result = repository.deleteForever(ids.toList())) {
            is DeleteResult.Success -> {
                clearSelection()
                _events.send(GalleryUiEvent.Message("Đã xóa vĩnh viễn"))
            }
            is DeleteResult.RequiresConfirmation -> {
                _events.send(GalleryUiEvent.RequestDeleteConfirmation(result.intentSender, ids.toList()))
            }
            is DeleteResult.Error -> {
                _events.send(GalleryUiEvent.Message(result.message))
            }
        }
    }

    fun onDeleteConfirmed(ids: Collection<Long>) = viewModelScope.launch {
        repository.finalizeDelete(ids.toList())
        clearSelection()
    }

    fun createAlbum(name: String) = viewModelScope.launch {
        repository.createAlbum(name)
    }

    fun createAlbumFromSelection(name: String, ids: Collection<Long>) = viewModelScope.launch {
        val albumId = repository.createAlbum(name)
        repository.addToAlbum(albumId, ids.toList())
        clearSelection()
    }

    fun renameAlbum(albumId: Long, name: String) = viewModelScope.launch {
        repository.renameAlbum(albumId, name)
    }

    fun deleteAlbum(albumId: Long) = viewModelScope.launch {
        repository.deleteAlbum(albumId)
    }

    fun addToAlbum(albumId: Long, ids: Collection<Long>) = viewModelScope.launch {
        repository.addToAlbum(albumId, ids.toList())
        clearSelection()
    }

    fun moveToAlbum(fromAlbumId: Long, toAlbumId: Long, ids: Collection<Long>) = viewModelScope.launch {
        repository.moveToAlbum(fromAlbumId, toAlbumId, ids.toList())
        clearSelection()
    }

    fun renameMedia(mediaId: Long, newName: String) = viewModelScope.launch {
        val ok = repository.renameMedia(mediaId, newName)
        if (!ok) _events.send(GalleryUiEvent.Message("Không thể đổi tên tệp này"))
    }

    fun moveToSecureFolder(ids: Collection<Long>) = viewModelScope.launch {
        repository.moveToSecureFolder(ids.toList())
        clearSelection()
        _events.send(GalleryUiEvent.Message("Đã chuyển vào Secure Folder"))
    }

    fun restoreFromSecureFolder(ids: Collection<Long>) = viewModelScope.launch {
        repository.restoreFromSecureFolder(ids.toList())
        clearSelection()
    }

    suspend fun decryptSecureThumbnail(mediaId: Long) = repository.decryptSecureThumbnail(mediaId)

    fun saveCollage(bitmap: android.graphics.Bitmap) = viewModelScope.launch {
        val name = "collage_${System.currentTimeMillis()}.jpg"
        val uri = repository.createImageFromBitmap(name, bitmap)
        _events.send(GalleryUiEvent.Message(if (uri != null) "Đã lưu ảnh ghép" else "Không thể lưu ảnh ghép"))
    }

    fun saveGif(bytes: ByteArray) = viewModelScope.launch {
        val name = "gif_${System.currentTimeMillis()}.gif"
        val uri = repository.writeGifBytes(name, bytes)
        _events.send(GalleryUiEvent.Message(if (uri != null) "Đã lưu GIF" else "Không thể lưu GIF"))
    }
}
