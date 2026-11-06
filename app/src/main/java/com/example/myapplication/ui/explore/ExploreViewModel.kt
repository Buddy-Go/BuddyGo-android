package com.example.myapplication.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeNotificationRepository
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.filter.FilterStore
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(
    // TODO(임시) 서버 연결 전까지 Fake를 쓴다.
    private val repository: MeetingRepository = FakeMeetingRepository(),
    private val notificationRepository: NotificationRepository = FakeNotificationRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    init {
        load()
        // 조건 설정 화면에서 적용한 조건이나 종목 칩으로 바꾼 조건이 곧바로 목록과 지도에 반영된다.
        viewModelScope.launch {
            FilterStore.applied.collect { filter ->
                _state.update { it.copy(filter = filter, selectedPinKey = null) }
            }
        }
    }

    // 알림 벨의 점. 알림 목록을 보고 돌아왔을 때도 맞도록 화면이 다시 보일 때마다 불러온다.
    // 알림을 불러오지 못하면 점을 보여 주지 않는다. (없는 알림을 있다고 알리지 않는다)
    fun refreshNotifications() {
        viewModelScope.launch {
            val hasUnread = notificationRepository.getNotifications().getOrNull()?.hasUnread() ?: false
            _state.update { it.copy(hasUnreadNotifications = hasUnread) }
        }
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

    // 종목 칩: 하나만 고르고 다시 누르면 해제. 종목이 바뀌면 이전 종목의 기준은 지워진다.
    fun onToggleSportChip(sport: Sport) = FilterStore.update { it.withSport(if (it.sport == sport) null else sport) }

    fun onCameraMoved() = _state.update { it.withCameraMoved() }

    // TODO(임시) 지도 범위를 서버에 넘기는 호출이 정해지면 영역을 같이 보낸다.
    fun onResearch() {
        FilterStore.update { it.copy(district = null) } // 재검색하면 위치 조건이 풀린다
        _state.update { it.afterResearch() }
        load()
    }

    // 찜: 먼저 화면에 반영하고 서버에 저장한다. 저장에 실패하면 원래 값으로 되돌린다.
    fun onToggleFavorite(meetingId: String) {
        val before = (_state.value.meetings as? UiState.Success)?.data?.firstOrNull { it.id == meetingId }?.isFavorite ?: return
        _state.update { it.withFavoriteSet(meetingId, !before) }
        viewModelScope.launch {
            repository.setFavorite(meetingId, !before).onFailure {
                _state.update { state -> state.withFavoriteSet(meetingId, before) }
            }
        }
    }

    fun onToggleFavoriteOnly() = _state.update { it.copy(favoriteOnly = !it.favoriteOnly, selectedPinKey = null) }
}
