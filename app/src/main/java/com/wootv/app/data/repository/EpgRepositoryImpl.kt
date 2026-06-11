package com.wootv.app.data.repository

import com.wootv.app.data.local.db.EpgDao
import com.wootv.app.data.local.mapper.toDomain
import com.wootv.app.data.local.mapper.toEntity
import com.wootv.app.data.remote.Downloader
import com.wootv.app.data.remote.XmlTvParser
import com.wootv.app.domain.model.EpgProgram
import com.wootv.app.domain.repository.EpgRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val epgDao: EpgDao,
    private val downloader: Downloader,
    private val xmlTvParser: XmlTvParser
) : EpgRepository {

    override fun getProgramsForChannel(channelId: String): Flow<List<EpgProgram>> {
        return epgDao.getProgramsForChannel(channelId, System.currentTimeMillis()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCurrentProgram(channelId: String): EpgProgram? {
        return epgDao.getCurrentProgram(channelId, System.currentTimeMillis())?.toDomain()
    }

    override suspend fun refreshEpg(playlistId: Long, epgUrl: String) {
        val content = downloader.download(epgUrl)
        val programs = xmlTvParser.parse(content, playlistId)

        epgDao.deleteByPlaylist(playlistId)
        epgDao.insertAll(programs)
    }
}
