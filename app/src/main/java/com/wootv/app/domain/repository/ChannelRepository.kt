package com.wootv.app.domain.repository

import com.wootv.app.domain.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<Channel>>
    fun getAllChannels(): Flow<List<Channel>>
    suspend fun getChannelById(id: Long): Channel?
    fun getChannelsByGroup(playlistId: Long, group: String): Flow<List<Channel>>
    fun getGroupsByPlaylist(playlistId: Long): Flow<List<String>>
    fun searchChannels(query: String): Flow<List<Channel>>
    fun getFavoriteChannels(): Flow<List<Channel>>
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
}
