package com.example.myapplication.ui.mate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.MateRepository
import kotlinx.coroutines.launch

// 메이트 화면들의 ViewModel. 화면 사이에는 userId만 넘긴다 (project-rules 7장).

// 11a·11b 메이트 탭 화면
class MateViewModel(private val repository: MateRepository) : ViewModel() {

    var state by mutableStateOf(MateState())
        private set

    init {
        refresh()
    }

    // 신청 화면·프로필에서 돌아왔을 때도 부른다.
    fun refresh() {
        viewModelScope.launch {
            state = state.copy(
                // TODO(임시): 실패하면 16f·16g 오류 화면 (공용 부품이 정해지면)
                mates = repository.getMates().getOrDefault(emptyList()),
                workedOut = repository.getWorkedOut().getOrDefault(emptyList()),
                pendingRequestCount = repository.getRequests().getOrDefault(emptyList()).size,
                isLoading = false,
                now = System.currentTimeMillis(),
            )
        }
    }

    fun onTabSelect(tab: MateTab) {
        state = state.copy(selectedTab = tab)
    }

    fun onSearchChange(query: String) {
        state = state.copy(searchQuery = query)
    }

    // 11b '메이트 신청'
    fun onSendRequest(userId: String) {
        viewModelScope.launch {
            repository.sendRequest(userId)
            state = state.copy(workedOut = repository.getWorkedOut().getOrDefault(state.workedOut))
        }
    }
}

// 11c 받은 메이트 신청
class MateRequestViewModel(private val repository: MateRepository) : ViewModel() {

    var state by mutableStateOf(MateRequestState())
        private set

    init {
        viewModelScope.launch {
            state = state.copy(requests = repository.getRequests().getOrDefault(emptyList()), isLoading = false)
        }
    }

    fun onAccept(requestId: String) = handle(requestId) { repository.acceptRequest(it) }

    fun onReject(requestId: String) = handle(requestId) { repository.rejectRequest(it) }

    private fun handle(requestId: String, action: suspend (String) -> Result<Unit>) {
        if (state.processingRequestId != null) return
        state = state.copy(processingRequestId = requestId)
        viewModelScope.launch {
            action(requestId)
            state = state.copy(requests = repository.getRequests().getOrDefault(state.requests), processingRequestId = null)
        }
    }
}

// 15 메이트 프로필
class MateProfileViewModel(
    private val userId: String,
    private val repository: MateRepository,
) : ViewModel() {

    var state by mutableStateOf(MateProfileState())
        private set

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            state = state.copy(profile = repository.getProfile(userId).getOrNull(), now = System.currentTimeMillis())
        }
    }

    // 15b → 15c
    fun onSendRequest() {
        if (state.bottomButton != ProfileButton.REQUEST || state.isSending) return
        state = state.copy(isSending = true)
        viewModelScope.launch {
            repository.sendRequest(userId)
            state = state.copy(profile = repository.getProfile(userId).getOrNull() ?: state.profile, isSending = false)
        }
    }

    fun onToggleNotification() {
        val enabled = state.profile?.notificationEnabled ?: return
        viewModelScope.launch {
            repository.setMateNotification(userId, !enabled)
            state = state.copy(profile = repository.getProfile(userId).getOrNull() ?: state.profile)
        }
    }

    // 메이트 끊기. TODO(임시): 확인 창 디자인이 없음 (팀에 질문)
    fun onRemoveMate() {
        viewModelScope.launch {
            if (repository.removeMate(userId).isSuccess) state = state.copy(isRemoved = true)
        }
    }
}
