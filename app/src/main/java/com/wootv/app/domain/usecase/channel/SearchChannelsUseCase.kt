package com.wootv.app.domain.usecase.channel

import com.wootv.app.domain.model.Channel
import com.wootv.app.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchChannelsUseCase @Inject constructor(
    private val repository: ChannelRepository
) {
    operator fun invoke(query: String): Flow<List<Channel>> {
        return repository.searchChannels(query)
    }
}
