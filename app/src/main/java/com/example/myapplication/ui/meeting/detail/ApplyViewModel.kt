package com.example.myapplication.ui.meeting.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ApplyViewModel(
    private val meetingId: String,
    private val repository: MeetingRepository = Repositories.meetings,
    private val userRepository: UserRepository = Repositories.users,
) : ViewModel() {
    private val _state = MutableStateFlow(ApplyState())
    val state: StateFlow<ApplyState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(meeting = UiState.Loading) }
        viewModelScope.launch {
            val me = userRepository.getCurrentUser().getOrNull()
            val result = repository.getMeeting(meetingId)
            _state.update {
                it.copy(
                    me = me,
                    meeting = result.fold(
                        onSuccess = { m -> UiState.Success(m) },
                        onFailure = { e -> UiState.Error(if (e is IOException) ErrorType.NETWORK else ErrorType.SERVER) },
                    ),
                )
            }
        }
    }

    // 프로필 기록 수정 화면에서 돌아오면 내 기록만 새로 받는다. (한마디 등 입력은 그대로 둔다)
    fun onResume() {
        viewModelScope.launch {
            val me = userRepository.getCurrentUser().getOrNull() ?: return@launch
            _state.update { it.copy(me = me) }
        }
    }

    // 한마디는 200자까지만 받고, 다시 쓰기 시작하면 실패 띠는 지운다.
    fun onMessageChange(text: String) = _state.update { it.copy(message = limitMessage(text), failed = false) }

    // 신청하기: 한마디가 비어 있으면 한 번 확인하고, 있으면 바로 보낸다. (5장)
    fun onSubmit() {
        val s = _state.value
        if (s.submitting || s.loaded == null) return
        if (messageToSend(s.message) == null) _state.update { it.copy(confirmEmpty = true) } else send()
    }

    fun onConfirmEmpty() {
        _state.update { it.copy(confirmEmpty = false) }
        send()
    }

    fun onDismissConfirmEmpty() = _state.update { it.copy(confirmEmpty = false) }

    private fun send() {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, failed = false) }
        viewModelScope.launch {
            repository.apply(meetingId, messageToSend(_state.value.message))
                .onSuccess { _state.update { it.copy(submitting = false, done = true) } }
                .onFailure { _state.update { it.copy(submitting = false, failed = true) } }
        }
    }
}
