package com.wootv.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wootv.app.data.local.entity.ChannelEntity
import com.wootv.app.data.local.entity.EpgProgramEntity
import com.wootv.app.data.local.entity.PlaylistEntity
import com.wootv.app.data.local.entity.RecentChannelEntity

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        EpgProgramEntity::class,
        RecentChannelEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao
    abstract fun recentChannelDao(): RecentChannelDao
}
