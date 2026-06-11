package com.wootv.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_programs",
    indices = [
        Index("channelId"),
        Index("startTime")
    ]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long,
    val playlistId: Long
)
