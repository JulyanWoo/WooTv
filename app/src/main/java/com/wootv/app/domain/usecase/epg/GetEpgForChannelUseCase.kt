package com.wootv.app.domain.usecase.epg

import com.wootv.app.domain.model.EpgProgram
import com.wootv.app.domain.repository.EpgRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEpgForChannelUseCase @Inject constructor(
    private val repository: EpgRepository
) {
    operator fun invoke(channelId: String): Flow<List<EpgProgram>> {
        return repository.getProgramsForChannel(channelId)
    }
}
