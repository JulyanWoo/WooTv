package com.wootv.app.data.repository

import com.wootv.app.data.local.db.ChannelDao
import com.wootv.app.data.local.db.EpgDao
import com.wootv.app.data.local.db.PlaylistDao
import com.wootv.app.data.local.entity.PlaylistEntity
import com.wootv.app.data.local.mapper.toDomain
import com.wootv.app.data.remote.Downloader
import com.wootv.app.data.remote.M3UParser
import com.wootv.app.domain.model.Playlist
import com.wootv.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val channelDao: ChannelDao,
    private val epgDao: EpgDao,
    private val downloader: Downloader,
    private val m3uParser: M3UParser
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPlaylistById(id: Long): Playlist? {
        return playlistDao.getPlaylistById(id)?.toDomain()
    }

    override suspend fun addPlaylist(name: String, url: String, epgUrl: String?): Long {
        val entity = PlaylistEntity(name = name, url = url, epgUrl = epgUrl)
        return playlistDao.insert(entity)
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        val entity = playlistDao.getPlaylistById(playlist.id) ?: return
        epgDao.deleteByPlaylist(playlist.id)
        channelDao.deleteByPlaylist(playlist.id)
        playlistDao.delete(entity)
    }

    override suspend fun refreshPlaylist(playlist: Playlist): Int {
        val content = downloader.download(playlist.url)
        val channels = m3uParser.parse(content, playlist.id)

        channelDao.deleteByPlaylist(playlist.id)
        channelDao.insertAll(channels)
        playlistDao.updateLastRefreshed(playlist.id, System.currentTimeMillis())

        return channels.size
    }
}
