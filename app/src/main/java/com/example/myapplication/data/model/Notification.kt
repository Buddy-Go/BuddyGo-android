package com.example.myapplication.data.model

// 서비스 알림 18종 (service-rules 12장)
enum class NotificationType {
    JOIN_REQUESTED,        // 참가자가 모임 신청 → 호스트
    JOIN_ACCEPTED,         // 신청 수락 → 신청자
    JOIN_REJECTED,         // 신청 거절 (호스트 거절, 자동 거절) → 신청자
    MEETING_CLOSED,        // 모임 마감 → 호스트+확정
    MEMBER_LEFT_BEFORE_DEADLINE, // 참가자 나감, 마감 풀림 → 호스트+남은 확정
    MEMBER_LEFT_AFTER_DEADLINE,  // 참가자 나감, 방폭 → 호스트+남은 확정
    MEETING_CANCELED_UNDERFILLED, // 인원 미달 취소 → 호스트+확정
    MEETING_CANCELED_BY_HOST,     // 호스트 취소(사유 포함) → 확정
    CHECKIN_OPENED,        // 시작 10분 전 → 호스트+확정
    MEETING_STARTED,       // 운동 시작 → 미체크인자
    MEETING_ENDED,         // 종료·평가 가능 → 체크인한 사람
    NOTICE_WRITTEN,        // 공지 작성·수정 → 확정
    NOTICE_COMMENTED,      // 내 공지에 댓글 → 호스트
    NOTICE_REPLIED,        // 내가 댓글 단 공지에 새 댓글 → 참가자
    MATE_REQUESTED,        // 메이트 신청 받음
    MATE_ACCEPTED,         // 메이트 수락
    MATE_MEETING_CREATED,  // 메이트가 모임 개설
    MATE_MEETING_JOINED,   // 메이트가 모임 신청
}

// 알림을 누르면 관련 화면으로 가므로, 이동에 필요한 id만 담는다. (화면 사이에는 id만 넘긴다)
data class Notification(
    val id: String = "",
    val type: NotificationType = NotificationType.JOIN_REQUESTED,
    val message: String = "",
    val meetingId: String? = null,
    val noticeId: String? = null,
    val userId: String? = null, // 메이트 신청 등 상대 사용자
    val createdAtMillis: Long = 0L,
    val isRead: Boolean = false,
)
