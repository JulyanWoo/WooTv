package com.wootv.app.domain.repository

import com.wootv.app.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: Long): Playlist?
    suspend fun addPlaylist(name: String, url: String, epgUrl: String?): Long
    suspend fun deletePlaylist(playlist: Playlist)
    suspend fun refreshPlaylist(playlist: Playlist): Int
}
