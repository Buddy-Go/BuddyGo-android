package com.example.myapplication.ui.mate

import com.example.myapplication.data.local.BlockedUsersStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Sport
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
            val mates = repository.getMates()
            val workedOut = repository.getWorkedOut()
            val requests = repository.getRequests()
            state = state.copy(
                mates = mates.getOrDefault(state.mates).filterNot { BlockedUsersStore.isBlocked(it.userId) },
                workedOut = workedOut.getOrDefault(state.workedOut).filterNot { BlockedUsersStore.isBlocked(it.userId) },
                pendingRequestCount = requests.map { it.size }.getOrDefault(state.pendingRequestCount),
                loadFailed = mates.isFailure || workedOut.isFailure || requests.isFailure,
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

    // 11b-2 종목 칩
    fun onSportFilterSelect(sport: Sport?) {
        state = state.copy(sportFilter = sport)
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
        load()
    }

    fun load() {
        viewModelScope.launch {
            val result = repository.getRequests()
            state = state.copy(requests = result.getOrDefault(state.requests), loadFailed = result.isFailure, isLoading = false)
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

    var state by mutableStateOf(MateProfileState(canReport = repository.supportsReport))
        private set

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val result = repository.getProfile(userId)
            state = state.copy(profile = result.getOrNull() ?: state.profile, loadFailed = result.isFailure, now = System.currentTimeMillis())
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

    // 신고하기: 사유를 골라 보낸다. 서버 신고 API가 없어 지금은 접수 안내만 보여 준다. (정의서: 초기 범위 밖이라 임시)
    fun onReport(reason: String) {
        if (!state.canReport) return // 서버가 받지 않는 신고를 '접수됐다'고 하지 않는다
        state = state.copy(reportedMessage = "신고가 접수됐어요")
    }

    fun onReportedMessageShown() {
        state = state.copy(reportedMessage = null)
    }

    // 차단하기: 차단 목록에 넣고 화면을 닫는다.
    fun onBlock() {
        val p = state.profile ?: return
        BlockedUsersStore.block(p.userId, p.nickname)
        state = state.copy(isBlocked = true)
    }

    // 메이트 끊기. 확인 창은 화면(MateProfileScreen)이 공용 ConfirmDialog로 먼저 띄운다.
    fun onRemoveMate() {
        viewModelScope.launch {
            if (repository.removeMate(userId).isSuccess) state = state.copy(isRemoved = true)
        }
    }
}
