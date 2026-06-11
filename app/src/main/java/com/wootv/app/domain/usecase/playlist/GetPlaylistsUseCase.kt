package com.wootv.app.domain.usecase.playlist

import com.wootv.app.domain.model.Playlist
import com.wootv.app.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaylistsUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return repository.getAllPlaylists()
    }
}
