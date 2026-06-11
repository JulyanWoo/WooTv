package com.wootv.app.domain.repository

import com.wootv.app.domain.model.EpgProgram
import kotlinx.coroutines.flow.Flow

interface EpgRepository {
    fun getProgramsForChannel(channelId: String): Flow<List<EpgProgram>>
    suspend fun getCurrentProgram(channelId: String): EpgProgram?
    suspend fun refreshEpg(playlistId: Long, epgUrl: String)
}
