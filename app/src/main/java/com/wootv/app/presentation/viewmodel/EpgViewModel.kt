package com.wootv.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wootv.app.domain.model.EpgProgram
import com.wootv.app.domain.usecase.epg.GetEpgForChannelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val getEpgForChannelUseCase: GetEpgForChannelUseCase
) : ViewModel() {

    fun getPrograms(channelId: String): StateFlow<List<EpgProgram>> {
        return getEpgForChannelUseCase(channelId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
}
