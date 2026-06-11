package com.wootv.app.data.repository

import com.wootv.app.data.local.db.ChannelDao
import com.wootv.app.data.local.mapper.toDomain
import com.wootv.app.domain.model.Channel
import com.wootv.app.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao
) : ChannelRepository {

    override fun getChannelsByPlaylist(playlistId: Long): Flow<List<Channel>> {
        return channelDao.getChannelsByPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllChannels(): Flow<List<Channel>> {
        return channelDao.getAllChannels().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChannelById(id: Long): Channel? {
        return channelDao.getChannelById(id)?.toDomain()
    }

    override fun getChannelsByGroup(playlistId: Long, group: String): Flow<List<Channel>> {
        return channelDao.getChannelsByGroup(playlistId, group).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getGroupsByPlaylist(playlistId: Long): Flow<List<String>> {
        return channelDao.getGroupsByPlaylist(playlistId)
    }

    override fun searchChannels(query: String): Flow<List<Channel>> {
        return channelDao.searchChannels(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteChannels(): Flow<List<Channel>> {
        return channelDao.getFavoriteChannels().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        channelDao.toggleFavorite(id, isFavorite)
    }
}
