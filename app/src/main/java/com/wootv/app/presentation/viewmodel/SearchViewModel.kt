package com.wootv.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wootv.app.domain.model.Channel
import com.wootv.app.domain.usecase.channel.SearchChannelsUseCase
import com.wootv.app.domain.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchChannelsUseCase: SearchChannelsUseCase,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<Channel>>(emptyList())
    val results: StateFlow<List<Channel>> = _results

    private var searchJob: Job? = null

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (newQuery.isNotBlank()) {
                searchChannelsUseCase(newQuery).collect { channels ->
                    _results.value = channels
                }
            } else {
                _results.value = emptyList()
            }
        }
    }

    fun toggleFavorite(channelId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId, isFavorite)
            val currentQuery = _query.value
            if (currentQuery.isNotBlank()) {
                searchChannelsUseCase(currentQuery).collect { channels ->
                    _results.value = channels
                }
            }
        }
    }
}
