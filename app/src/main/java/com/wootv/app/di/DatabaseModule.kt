package com.wootv.app.di

import android.content.Context
import androidx.room.Room
import com.wootv.app.data.local.db.AppDatabase
import com.wootv.app.data.local.db.ChannelDao
import com.wootv.app.data.local.db.EpgDao
import com.wootv.app.data.local.db.PlaylistDao
import com.wootv.app.data.local.db.RecentChannelDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wootv_database"
        ).build()
    }

    @Provides fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
    @Provides fun provideChannelDao(db: AppDatabase): ChannelDao = db.channelDao()
    @Provides fun provideEpgDao(db: AppDatabase): EpgDao = db.epgDao()
    @Provides fun provideRecentChannelDao(db: AppDatabase): RecentChannelDao = db.recentChannelDao()
}
