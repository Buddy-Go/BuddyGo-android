package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.ui.common.UiState

// 모임 상세 위에 뜨는 시트 (5e 장소, 5f·5h 기준)
enum class DetailSheet { PLACE, CRITERIA }

data class MeetingDetailState(
    val meeting: UiState<Meeting> = UiState.Loading,
    val myUserId: String = "",
    val nowMillis: Long = 0L,
    val submitting: Boolean = false,       // 신청 취소 같은 요청을 보내는 중
    val message: String? = null,           // 사용자에게 한 번 보여 줄 안내(실패 등)
    val openSheet: DetailSheet? = null,
) {
    val loaded: Meeting? get() = (meeting as? UiState.Success)?.data

    // 모임에서 본 나의 역할. 판단은 Meeting.roleOf 한 곳에서만 한다.
    val role: MeetingRole get() = loaded?.roleOf(myUserId) ?: MeetingRole.NONE

    // 지금 하단에 보여 줄 큰 버튼 (호스트이거나 모임을 못 불러왔으면 null)
    val bottomAction: BottomAction?
        get() = loaded?.let { bottomActionOf(it, role, myUserId, nowMillis) }

    fun withFavorite(favorite: Boolean): MeetingDetailState {
        val m = loaded ?: return this
        return copy(meeting = UiState.Success(m.copy(isFavorite = favorite)))
    }
}
