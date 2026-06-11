package com.wootv.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wootv.app.domain.model.Playlist
import com.wootv.app.domain.usecase.playlist.AddPlaylistUseCase
import com.wootv.app.domain.usecase.playlist.DeletePlaylistUseCase
import com.wootv.app.domain.usecase.playlist.GetPlaylistsUseCase
import com.wootv.app.domain.usecase.playlist.RefreshPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addPlaylistUseCase: AddPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val refreshPlaylistUseCase: RefreshPlaylistUseCase
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            try {
                val current = getPlaylistsUseCase().first()
                if (current.isEmpty()) {
                    addPlaylist(
                        name = "Colombia",
                        url = "https://iptv-org.github.io/iptv/countries/co.m3u",
                        epgUrl = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addPlaylist(name: String, url: String, epgUrl: String?) {
        viewModelScope.launch {
            val id = addPlaylistUseCase(name, url, epgUrl)
            val playlist = Playlist(
                id = id,
                name = name,
                url = url,
                epgUrl = epgUrl,
                lastRefreshed = null
            )
            refreshPlaylist(playlist)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            deletePlaylistUseCase(playlist)
        }
    }

    fun refreshPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            refreshPlaylistUseCase(playlist)
        }
    }
}
