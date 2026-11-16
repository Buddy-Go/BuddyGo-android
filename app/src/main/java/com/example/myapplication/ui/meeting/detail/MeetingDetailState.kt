package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.GeoPoint
import com.example.myapplication.ui.explore.meetingDistanceMeters

// 모임 상세 위에 뜨는 시트 (5e 장소, 5f·5h 기준)
enum class DetailSheet { PLACE, CRITERIA }

// 호스트가 ⋯ 메뉴에서 연 창 (5a-X2 취소 시트, 5a-X3 삭제 확인)
enum class HostDialog { CANCEL, DELETE }

data class MeetingDetailState(
    val meeting: UiState<Meeting> = UiState.Loading,
    val myUserId: String = "",
    val myGender: Gender? = null,
    val myLocation: GeoPoint? = null,        // 탐색에서 알아낸 내 위치. 장소 시트의 거리 계산에 쓴다
    val myRecord: ExerciseRecord? = null,   // 기준 시트에서 내 기록과 비교할 때 쓴다
    val nowMillis: Long = 0L,
    val submitting: Boolean = false,       // 신청 취소 같은 요청을 보내는 중
    val message: String? = null,           // 사용자에게 한 번 보여 줄 안내(실패 등)
    val openSheet: DetailSheet? = null,
    val notice: String? = null,              // 한 번 보여 줄 성공 안내(체크인 등)
    val leaveDialog: LeavePlan? = null,      // 나가기 확인창. null이면 닫혀 있다
    val checkIn: CheckInPrompt? = null,      // 체크인 확인창(9a, 9b). null이면 닫혀 있다
    val checkInPoint: GeoPoint? = null,      // 체크인 확인창에서 잰 내 위치. 체크인을 보낼 때 쓴다
    val hostSection: HostSectionState? = null, // 내가 호스트일 때만 채운다 (5a-H 계열)
    val participantNotices: ParticipantNotices? = null, // 내가 확정 참가자일 때만 채운다 (공지는 호스트·확정 참가자만 읽는다, service-rules 9장)
    val hostDialog: HostDialog? = null,      // 취소 시트·삭제 확인창. null이면 닫혀 있다
    val deleted: Boolean = false,            // 모임을 삭제했다: 화면을 닫는다
) {
    val loaded: Meeting? get() = (meeting as? UiState.Success)?.data

    // 모임에서 본 나의 역할. 판단은 Meeting.roleOf 한 곳에서만 한다.
    val role: MeetingRole get() = loaded?.roleOf(myUserId) ?: MeetingRole.NONE

    // 지금 하단에 보여 줄 큰 버튼 (호스트이거나 모임을 못 불러왔으면 null)
    val bottomAction: BottomAction?
        get() = loaded?.let { bottomActionOf(it, role, myUserId, nowMillis, myGender) }

    // 장소 시트의 '내 위치에서 N'에 쓰는 거리. 탐색 카드와 같은 값이다.
    val distanceMeters: Int? get() = loaded?.let { meetingDistanceMeters(it, myLocation) }

    // 지금 이 사람에게 ⋮ 메뉴에 보여 줄 '나가기'가 있는지와 그 확인창 내용 (없으면 null)
    val leavePlan: LeavePlan? get() = loaded?.let { leavePlanOf(it, role, nowMillis) }

    fun withFavorite(favorite: Boolean): MeetingDetailState {
        val m = loaded ?: return this
        return copy(meeting = UiState.Success(m.copy(isFavorite = favorite)))
    }
}

// 확정 참가자가 보는 공지 영역. 공지가 하나도 없으면 latest가 null이다.
data class ParticipantNotices(val latest: HostNoticePreview?)
