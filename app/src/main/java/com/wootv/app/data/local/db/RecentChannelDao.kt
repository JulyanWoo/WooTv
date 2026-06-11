package com.wootv.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wootv.app.data.local.entity.RecentChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentChannelDao {
    @Query("SELECT * FROM recent_channels ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentChannels(limit: Int = 10): Flow<List<RecentChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recent: RecentChannelEntity)

    @Query("DELETE FROM recent_channels WHERE lastWatchedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
