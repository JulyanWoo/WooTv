package com.wootv.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_channels",
    indices = [Index("channelId")]
)
data class RecentChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    val lastWatchedAt: Long
)
