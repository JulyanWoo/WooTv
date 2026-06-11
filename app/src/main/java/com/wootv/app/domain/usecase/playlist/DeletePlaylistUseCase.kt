package com.wootv.app.domain.usecase.playlist

import com.wootv.app.domain.model.Playlist
import com.wootv.app.domain.repository.PlaylistRepository
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlist: Playlist) {
        repository.deletePlaylist(playlist)
    }
}
