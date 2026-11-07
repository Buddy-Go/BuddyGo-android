package com.example.myapplication.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.MapBounds
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.data.repository.UserRepository
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
    private val repository: MeetingRepository = Repositories.meetings,
    private val notificationRepository: NotificationRepository = Repositories.notifications,
    private val userRepository: UserRepository = Repositories.users,
) : ViewModel() {
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    init {
        load()
        // 조건 설정 화면에서 적용한 조건이나 종목 칩으로 바꾼 조건이 곧바로 목록과 지도에 반영된다.
        viewModelScope.launch {
            FilterStore.applied.collect { filter ->
                // 위치(구·동)를 고르면 지도 영역 기준은 풀리고 전체에서 그 지역을 찾는다.
                val clearBounds = filter.district != null && _state.value.searchBounds != null
                _state.update { it.copy(filter = filter, selectedPinKey = null, searchBounds = if (clearBounds) null else it.searchBounds) }
                if (clearBounds) load()
            }
        }
    }

    // 알림 벨의 점. 알림 목록을 보고 돌아왔을 때도 맞도록 화면이 다시 보일 때마다 불러온다.
    // 알림을 불러오지 못하면 점을 보여 주지 않는다. (없는 알림을 있다고 알리지 않는다)
    fun refreshNotifications() {
        viewModelScope.launch {
            // 마이페이지에서 알림을 껐으면 읽지 않은 알림이 있어도 점을 보이지 않는다. (service-rules 12장) 설정을 못 읽으면 켜져 있다고 본다.
            val enabled = userRepository.isNotificationEnabled().getOrDefault(true)
            val hasUnread = enabled && (notificationRepository.getNotifications().getOrNull()?.hasUnread() ?: false)
            _state.update { it.copy(hasUnreadNotifications = hasUnread) }
        }
    }

    // 개설·신청 같은 일을 하고 돌아왔을 때: 목록을 비우지 않고 새로 받는다. 실패하면 보던 목록을 그대로 둔다.
    fun refreshMeetings() {
        if (_state.value.meetings !is UiState.Success) return
        viewModelScope.launch {
            repository.getMeetings(_state.value.searchBounds).onSuccess { list ->
                _state.update { it.copy(meetings = UiState.Success(list)) }
            }
        }
    }

    fun load() {
        _state.update { it.copy(meetings = UiState.Loading) }
        viewModelScope.launch {
            // 재검색한 영역이 있으면 그 안의 모임만 받는다.
            val result = repository.getMeetings(_state.value.searchBounds)
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

    // 내 위치를 받았다. 위치를 못 얻었으면(null) 이전 값을 지우지 않는다.
    fun onMyLocation(location: GeoPoint?) {
        location?.let(LastKnownLocation::set) // 모임 상세 등 다른 화면도 같은 위치를 쓴다
        _state.update { it.copy(myLocation = location ?: it.myLocation) }
    }

    // 위치 권한은 앱을 켜고 한 번만 묻는다. 거절해도 화면을 열 때마다 다시 묻지 않는다.
    fun onLocationAsked() = _state.update { it.copy(locationAsked = true) }

    // 사용자가 지도를 옮기면 '현 지도에서 검색'이 나타나고, 그때 지도에 보이는 영역을 기억해 둔다.
    fun onCameraMoved(bounds: MapBounds? = null) = _state.update { it.withCameraMoved(bounds) }

    // 재검색: 기억해 둔 지도 영역 안의 모임을 다시 받는다.
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
