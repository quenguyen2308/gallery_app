package com.gallery.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gallery.data.local.db.dao.AlbumDao
import com.gallery.data.local.db.dao.AlbumMediaDao
import com.gallery.data.local.db.dao.EditHistoryDao
import com.gallery.data.local.db.dao.FavoriteDao
import com.gallery.data.local.db.dao.SecureItemDao
import com.gallery.data.local.db.dao.TrashDao
import com.gallery.data.local.db.entity.AlbumEntity
import com.gallery.data.local.db.entity.AlbumMediaEntity
import com.gallery.data.local.db.entity.EditHistoryEntity
import com.gallery.data.local.db.entity.FavoriteEntity
import com.gallery.data.local.db.entity.SecureItemEntity
import com.gallery.data.local.db.entity.TrashEntity

@Database(
    entities = [
        AlbumEntity::class,
        AlbumMediaEntity::class,
        FavoriteEntity::class,
        TrashEntity::class,
        SecureItemEntity::class,
        EditHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun albumMediaDao(): AlbumMediaDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun trashDao(): TrashDao
    abstract fun secureItemDao(): SecureItemDao
    abstract fun editHistoryDao(): EditHistoryDao

    companion object {
        const val DATABASE_NAME = "gallery.db"
    }
}
