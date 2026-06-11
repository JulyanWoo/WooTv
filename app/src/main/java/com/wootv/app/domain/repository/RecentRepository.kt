package com.wootv.app.domain.repository

import com.wootv.app.domain.model.RecentChannel
import kotlinx.coroutines.flow.Flow

interface RecentRepository {
    fun getRecentChannels(): Flow<List<RecentChannel>>
    suspend fun addRecentChannel(channelId: Long)
}
