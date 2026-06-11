package com.wootv.app.data.repository

import com.wootv.app.data.local.db.RecentChannelDao
import com.wootv.app.data.local.entity.RecentChannelEntity
import com.wootv.app.domain.model.RecentChannel
import com.wootv.app.domain.repository.RecentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentRepositoryImpl @Inject constructor(
    private val recentChannelDao: RecentChannelDao
) : RecentRepository {

    override fun getRecentChannels(): Flow<List<RecentChannel>> {
        return recentChannelDao.getRecentChannels().map { entities ->
            entities.map { entity ->
                RecentChannel(
                    id = entity.id,
                    channelId = entity.channelId,
                    lastWatchedAt = entity.lastWatchedAt
                )
            }
        }
    }

    override suspend fun addRecentChannel(channelId: Long) {
        val entity = RecentChannelEntity(
            channelId = channelId,
            lastWatchedAt = System.currentTimeMillis()
        )
        recentChannelDao.insert(entity)
    }
}
