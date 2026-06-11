package com.wootv.app.domain.usecase.channel

import com.wootv.app.domain.repository.ChannelRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ChannelRepository
) {
    suspend operator fun invoke(id: Long, isFavorite: Boolean) {
        repository.toggleFavorite(id, isFavorite)
    }
}
