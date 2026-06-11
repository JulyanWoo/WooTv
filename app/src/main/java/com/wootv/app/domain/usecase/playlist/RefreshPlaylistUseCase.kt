package com.wootv.app.domain.usecase.playlist

import com.wootv.app.domain.model.Playlist
import com.wootv.app.domain.repository.PlaylistRepository
import javax.inject.Inject

class RefreshPlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlist: Playlist): Int {
        return repository.refreshPlaylist(playlist)
    }
}
