package com.wootv.app.domain.usecase.epg

import com.wootv.app.domain.repository.EpgRepository
import javax.inject.Inject

class RefreshEpgUseCase @Inject constructor(
    private val repository: EpgRepository
) {
    suspend operator fun invoke(playlistId: Long, epgUrl: String) {
        repository.refreshEpg(playlistId, epgUrl)
    }
}
