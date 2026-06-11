package com.wootv.app.domain.usecase.playlist

import com.wootv.app.domain.repository.PlaylistRepository
import javax.inject.Inject

class AddPlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(name: String, url: String, epgUrl: String? = null): Long {
        return repository.addPlaylist(name, url, epgUrl)
    }
}
