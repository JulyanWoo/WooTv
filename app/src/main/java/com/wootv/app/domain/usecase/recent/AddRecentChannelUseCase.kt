package com.wootv.app.domain.usecase.recent

import com.wootv.app.domain.repository.RecentRepository
import javax.inject.Inject

class AddRecentChannelUseCase @Inject constructor(
    private val repository: RecentRepository
) {
    suspend operator fun invoke(channelId: Long) {
        repository.addRecentChannel(channelId)
    }
}
