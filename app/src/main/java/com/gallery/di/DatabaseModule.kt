package com.gallery.di

import android.content.Context
import androidx.room.Room
import com.gallery.data.local.db.AppDatabase
import com.gallery.data.local.db.dao.AlbumDao
import com.gallery.data.local.db.dao.AlbumMediaDao
import com.gallery.data.local.db.dao.EditHistoryDao
import com.gallery.data.local.db.dao.FavoriteDao
import com.gallery.data.local.db.dao.SecureItemDao
import com.gallery.data.local.db.dao.TrashDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME).build()

    @Provides
    fun provideAlbumDao(db: AppDatabase): AlbumDao = db.albumDao()

    @Provides
    fun provideAlbumMediaDao(db: AppDatabase): AlbumMediaDao = db.albumMediaDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideTrashDao(db: AppDatabase): TrashDao = db.trashDao()

    @Provides
    fun provideSecureItemDao(db: AppDatabase): SecureItemDao = db.secureItemDao()

    @Provides
    fun provideEditHistoryDao(db: AppDatabase): EditHistoryDao = db.editHistoryDao()
}
