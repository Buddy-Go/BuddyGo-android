package com.example.myapplication.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(
    // TODO(임시) 서버 연결 전까지 Fake를 쓴다.
    private val repository: MeetingRepository = FakeMeetingRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(meetings = UiState.Loading) }
        viewModelScope.launch {
            val result = repository.getMeetings()
            _state.update {
                it.copy(
                    meetings = result.fold(
                        onSuccess = { list -> UiState.Success(list) },
                        onFailure = { e -> UiState.Error(if (e is IOException) ErrorType.NETWORK else ErrorType.SERVER) },
                    ),
                )
            }
        }
    }

    // 같은 핀을 다시 누르면 선택이 풀린다.
    fun onSelectPin(key: String) = _state.update {
        it.copy(selectedPinKey = if (it.selectedPinKey == key) null else key)
    }

    fun onClearPin() = _state.update { it.copy(selectedPinKey = null) }

    fun onSetSheetExpanded(expanded: Boolean) = _state.update { it.copy(sheetExpanded = expanded) }

    fun onToggleSportChip(sport: Sport) = _state.update {
        it.copy(sportChip = if (it.sportChip == sport) null else sport, selectedPinKey = null)
    }

    fun onCameraMoved() = _state.update { it.withCameraMoved() }

    // TODO(임시) 지도 범위를 서버에 넘기는 호출이 정해지면 영역을 같이 보낸다.
    fun onResearch() {
        _state.update { it.afterResearch() }
        load()
    }

    fun onToggleFavorite(meetingId: String) = _state.update { it.withFavoriteToggled(meetingId) }

    fun onToggleFavoriteOnly() = _state.update { it.copy(favoriteOnly = !it.favoriteOnly, selectedPinKey = null) }
}
