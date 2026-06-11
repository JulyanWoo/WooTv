package com.wootv.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wootv.app.domain.model.Channel
import com.wootv.app.domain.model.Playlist
import com.wootv.app.domain.repository.ChannelRepository
import com.wootv.app.domain.usecase.playlist.AddPlaylistUseCase
import com.wootv.app.domain.usecase.playlist.DeletePlaylistUseCase
import com.wootv.app.domain.usecase.playlist.GetPlaylistsUseCase
import com.wootv.app.domain.usecase.playlist.RefreshPlaylistUseCase
import com.wootv.app.data.config.DefaultPlaylistsConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val addPlaylistUseCase: AddPlaylistUseCase,
    private val refreshPlaylistUseCase: RefreshPlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = getPlaylistsUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlistNameMap: StateFlow<Map<Long, String>> = playlists
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites: StateFlow<Boolean> = _showOnlyFavorites

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    val allChannels: StateFlow<List<Channel>> = _allChannels

    val filteredChannels: StateFlow<List<Channel>> = combine(
        _allChannels,
        _showOnlyFavorites
    ) { channels, showOnlyFavs ->
        if (showOnlyFavs) {
            channels.filter { it.isFavorite }
        } else {
            channels
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Get current playlists from DB
                val currentPlaylists = getPlaylistsUseCase().first()
                val currentUrls = currentPlaylists.map { it.url }.toSet()
                val defaultUrls = DefaultPlaylistsConfig.PLAYLISTS.map { it.second }.toSet()

                // Delete playlists from DB that are no longer in our defaults (e.g. old Colombia / TDT)
                currentPlaylists.forEach { playlist ->
                    if (playlist.url !in defaultUrls) {
                        try {
                            deletePlaylistUseCase(playlist)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // 2. Add missing default playlists
                val updatedCurrentPlaylists = getPlaylistsUseCase().first()
                val updatedUrls = updatedCurrentPlaylists.map { it.url }.toSet()
                val updatedNames = updatedCurrentPlaylists.map { it.name }.toSet()

                val missingPlaylists = DefaultPlaylistsConfig.PLAYLISTS.filter { 
                    it.second !in updatedUrls && it.first !in updatedNames 
                }

                missingPlaylists.forEach { (name, url) ->
                    try {
                        val id = addPlaylistUseCase(name, url, null)
                        val playlist = Playlist(id = id, name = name, url = url, epgUrl = null)
                        launch {
                            try {
                                refreshPlaylistUseCase(playlist)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 3. Refresh any playlist if it has not been refreshed recently (e.g. within 24h)
                val finalPlaylists = getPlaylistsUseCase().first()
                finalPlaylists.forEach { playlist ->
                    val age = System.currentTimeMillis() - (playlist.lastRefreshed ?: 0L)
                    if (playlist.lastRefreshed == null || age > 24 * 60 * 60 * 1000) {
                        launch {
                            try {
                                refreshPlaylistUseCase(playlist)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                // 4. Start listening to all channels
                loadAllChannels()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadAllChannels() {
        viewModelScope.launch {
            channelRepository.getAllChannels().collect { channels ->
                _allChannels.value = channels
            }
        }
    }

    fun setShowOnlyFavorites(show: Boolean) {
        _showOnlyFavorites.value = show
    }

    fun toggleFavorite(channelId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId, isFavorite)
        }
    }
}
