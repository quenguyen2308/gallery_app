package com.gallery.ui.navigation

import com.gallery.domain.model.AlbumId
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

object GalleryDestinations {
    const val PHOTOS = "photos"
    const val ALBUMS = "albums"
    const val ALBUM_DETAIL = "album/{albumIdArg}"
    const val VIEWER = "viewer/{mediaId}"
    const val FAVORITES = "favorites"
    const val TRASH = "trash"
    const val SECURE_FOLDER = "secure"
    const val EDITOR = "editor/{mediaId}"

    fun albumDetailRoute(albumId: AlbumId) = "album/${encodeAlbumId(albumId)}"
    fun viewerRoute(mediaId: Long) = "viewer/$mediaId"
    fun editorRoute(mediaId: Long) = "editor/$mediaId"
}

fun encodeAlbumId(id: AlbumId): String {
    val raw = when (id) {
        is AlbumId.Auto -> "auto:${id.bucketId}"
        is AlbumId.Custom -> "custom:${id.roomId}"
    }
    return URLEncoder.encode(raw, UTF_8.name())
}

fun decodeAlbumId(arg: String): AlbumId {
    val raw = URLDecoder.decode(arg, UTF_8.name())
    val (type, value) = raw.split(":", limit = 2)
    return when (type) {
        "auto" -> AlbumId.Auto(value)
        else -> AlbumId.Custom(value.toLong())
    }
}
