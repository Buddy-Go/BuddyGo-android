package com.example.myapplication.ui.meeting.manage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.launch

// 참가자 관리 (7a~7d). 화면 사이에는 meetingId만 넘기고, 명단은 여기서 불러온다 (project-rules 7장).
// 수락·거절 후 마감 여부와 남은 신청은 서버(MeetingRepository) 응답을 다시 받아 보여준다.
class ParticipantManageViewModel(
    private val meetingId: String,
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    var state by mutableStateOf(ParticipantManageState(meetingId = meetingId))
        private set

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val meeting = meetingRepository.getMeeting(meetingId).getOrNull() ?: return@launch
            // TODO(임시): 실패하면 16f·16g 오류 화면 (FullScreenError 공용 부품이 정해지면)
            val requests = meetingRepository.getJoinRequests(meetingId).getOrDefault(emptyList())
            val ids = (listOf(meeting.hostId) + meeting.confirmedIds).distinct()
            val users = ids.mapNotNull { userRepository.getUser(it).getOrNull() }.associateBy(User::id)
            state = buildParticipantState(meeting, requests, users, state.selectedTab)
        }
    }

    fun onTabSelect(tab: ManageTab) {
        state = state.copy(selectedTab = tab)
    }

    fun onAccept(applicationId: String) = handle(applicationId) { meetingRepository.accept(meetingId, it) }

    fun onReject(applicationId: String) = handle(applicationId) { meetingRepository.reject(meetingId, it) }

    private fun handle(applicationId: String, action: suspend (String) -> Result<Unit>) {
        if (state.processingApplicationId != null) return
        state = state.copy(processingApplicationId = applicationId)
        viewModelScope.launch {
            action(applicationId)
            // TODO(임시): 실패하면 16i 인라인 오류 (ActionButton 공용 부품이 정해지면)
            state = state.copy(processingApplicationId = null)
            reload()
        }
    }
}

// ---------- 미리보기용 예시 (7a·7b 디자인과 비슷하게, 헬스 정원 3명) ----------

internal fun sampleState(meetingId: String) = ParticipantManageState(
    meetingId = meetingId,
    capacity = 3,
    confirmed = listOf(
        ConfirmedMember("u1", "민준", 41.5, isHost = true),
        ConfirmedMember("u2", "수아", 39.8),
    ),
    applicants = listOf(
        Applicant("a1", "u4", "서연", 44.1, "가슴 80kg · 팔 중급", "퇴근하고 바로 갈 수 있어요. 벤치 스팟 봐드릴게요!"),
        Applicant("a2", "u5", "태윤", 37.2, null, null),
    ),
)

// 7d 마감 후: 확정 3명 = 정원
internal fun sampleClosedState(meetingId: String) = sampleState(meetingId).copy(
    confirmed = sampleState(meetingId).confirmed + ConfirmedMember("u3", "지호", 38.4),
    applicants = emptyList(),
    isClosed = true,
)
