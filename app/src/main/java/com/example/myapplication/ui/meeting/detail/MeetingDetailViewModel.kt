package com.example.myapplication.ui.meeting.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.LastKnownLocation
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MeetingDetailViewModel(
    private val meetingId: String,
    // TODO(임시) 서버 연결 전까지 Fake를 쓴다.
    private val repository: MeetingRepository = FakeMeetingRepository(),
    private val userRepository: UserRepository = FakeUserRepository(),
    nowMillis: Long = System.currentTimeMillis(),
) : ViewModel() {
    private val _state = MutableStateFlow(MeetingDetailState(nowMillis = nowMillis, myLocation = LastKnownLocation.point.value))
    val state: StateFlow<MeetingDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(meeting = UiState.Loading) }
        viewModelScope.launch {
            // 내가 누구인지 알아야 역할(호스트·확정·대기)을 판단할 수 있다.
            val me = userRepository.getCurrentUser().getOrNull()
            _state.update { it.copy(myUserId = me?.id.orEmpty(), myRecord = me?.record) }
            val result = repository.getMeeting(meetingId)
            _state.update {
                it.copy(
                    meeting = result.fold(
                        onSuccess = { meeting -> UiState.Success(meeting) },
                        onFailure = { e -> UiState.Error(if (e is IOException) ErrorType.NETWORK else ErrorType.SERVER) },
                    ),
                )
            }
        }
    }

    // 찜: 먼저 화면에 반영하고 저장한다. 실패하면 되돌리고 안내한다.
    fun onToggleFavorite() {
        val before = _state.value.loaded?.isFavorite ?: return
        _state.update { it.withFavorite(!before) }
        viewModelScope.launch {
            repository.setFavorite(meetingId, !before).onFailure {
                _state.update { state -> state.withFavorite(before).copy(message = "찜을 저장하지 못했어요. 다시 시도해주세요.") }
            }
        }
    }

    // 신청 취소: 대기 중에는 언제든 취소할 수 있고 매너온도에 영향이 없다. (service-rules 5장)
    fun onCancelApplication() {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.cancelApplication(meetingId)
                .onSuccess { reloadMeeting() }
                .onFailure { _state.update { s -> s.copy(message = "신청을 취소하지 못했어요. 다시 시도해주세요.") } }
            _state.update { it.copy(submitting = false) }
        }
    }

    // 안내를 보여 준 뒤 지운다.
    fun onMessageShown() = _state.update { it.copy(message = null) }

    fun onOpenSheet(sheet: DetailSheet) = _state.update { it.copy(openSheet = sheet) }
    fun onCloseSheet() = _state.update { it.copy(openSheet = null) }

    // 화면은 그대로 두고 모임 정보만 다시 받는다.
    private suspend fun reloadMeeting() {
        repository.getMeeting(meetingId).onSuccess { meeting ->
            _state.update { it.copy(meeting = UiState.Success(meeting)) }
        }
    }
}
