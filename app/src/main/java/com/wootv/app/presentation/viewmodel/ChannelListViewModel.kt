package com.wootv.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wootv.app.domain.model.Channel
import com.wootv.app.domain.usecase.channel.GetChannelsUseCase
import com.wootv.app.domain.usecase.channel.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelListViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    val allChannels: StateFlow<List<Channel>> = _allChannels

    val channels: StateFlow<List<Channel>> = _allChannels
        .combine(_selectedGroup) { all, group ->
            val filtered = if (group == null) all else all.filter { it.groupTitle == group }
            val (colombia, rest) = filtered.partition { channel ->
                val name = channel.name
                val g = channel.groupTitle ?: ""
                g.contains("colombia", ignoreCase = true) ||
                g.contains("co |", ignoreCase = true) ||
                g.contains("co:", ignoreCase = true) ||
                name.contains("colombia", ignoreCase = true) ||
                name.contains("co |", ignoreCase = true) ||
                name.startsWith("co:", ignoreCase = true) ||
                name.contains("(co)", ignoreCase = true) ||
                name.contains("[co]", ignoreCase = true)
            }
            colombia + rest
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val groups: StateFlow<List<String>> = MutableStateFlow(emptyList())

    fun loadChannels(playlistId: Long) {
        viewModelScope.launch {
            getChannelsUseCase(playlistId).collect { channelList ->
                _allChannels.value = channelList
            }
        }
    }

    fun setGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun toggleFavorite(channelId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(channelId, isFavorite)
        }
    }
}
