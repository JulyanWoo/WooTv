package com.wootv.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wootv.app.domain.model.Channel
import com.wootv.app.domain.model.EpgProgram
import com.wootv.app.domain.repository.ChannelRepository
import com.wootv.app.domain.repository.EpgRepository
import com.wootv.app.domain.usecase.recent.AddRecentChannelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val addRecentChannelUseCase: AddRecentChannelUseCase
) : ViewModel() {

    private val _channel = MutableStateFlow<Channel?>(null)
    val channel: StateFlow<Channel?> = _channel

    private val _currentProgram = MutableStateFlow<EpgProgram?>(null)
    val currentProgram: StateFlow<EpgProgram?> = _currentProgram

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var playlistChannels: List<Channel> = emptyList()
    private var currentIndex: Int = -1

    fun loadChannel(channelId: Long) {
        viewModelScope.launch {
            val ch = channelRepository.getChannelById(channelId)
            _channel.value = ch
            addRecentChannelUseCase(channelId)

            ch?.let {
                val program = epgRepository.getCurrentProgram(it.tvgId ?: it.id.toString())
                _currentProgram.value = program
            }
        }
    }

    fun loadPlaylistChannels(playlistId: Long, currentChannelId: Long) {
        viewModelScope.launch {
            channelRepository.getChannelsByPlaylist(playlistId).collect { channels ->
                playlistChannels = channels
                currentIndex = channels.indexOfFirst { it.id == currentChannelId }
            }
        }
    }

    fun playNext() {
        if (currentIndex < playlistChannels.size - 1) {
            currentIndex++
            val next = playlistChannels[currentIndex]
            _channel.value = next
            _isPlaying.value = true
        }
    }

    fun playPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            val prev = playlistChannels[currentIndex]
            _channel.value = prev
            _isPlaying.value = true
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun hasNext(): Boolean = currentIndex < playlistChannels.size - 1
    fun hasPrevious(): Boolean = currentIndex > 0
}
