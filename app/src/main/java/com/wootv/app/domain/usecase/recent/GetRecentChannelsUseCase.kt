package com.wootv.app.domain.usecase.recent

import com.wootv.app.domain.model.RecentChannel
import com.wootv.app.domain.repository.RecentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentChannelsUseCase @Inject constructor(
    private val repository: RecentRepository
) {
    operator fun invoke(): Flow<List<RecentChannel>> {
        return repository.getRecentChannels()
    }
}
