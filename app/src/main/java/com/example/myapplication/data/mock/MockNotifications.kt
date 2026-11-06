package com.example.myapplication.data.mock

import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType

// 읽은 알림과 읽지 않은 알림, 오늘과 어제 알림, 빈 목록을 모두 담는다.
// 시각은 MockMeetings와 같은 기준 시각(BASE)에서 더하고 뺀 고정값이다. 화면은 '지금'을 따로 받아 오늘/이전을 나눈다.
object MockNotifications {
    private const val MINUTE = 60_000L
    private const val HOUR = 3_600_000L
    private const val BASE = 1_798_761_600_000L

    // 시작 10분 전 · 체크인 열림 (진행중인 내 모임 m4)
    val unreadCheckIn = Notification(
        id = "n0", type = NotificationType.CHECKIN_OPENED, subject = "북한산 가볍게",
        message = "모임이 곧 시작해요. 지금 체크인할 수 있어요",
        meetingId = "m4", createdAtMillis = BASE - 5 * MINUTE, isRead = false,
    )
    val unreadAccepted = Notification(
        id = "n1", type = NotificationType.JOIN_ACCEPTED, subject = "가슴·팔 같이 하실 분",
        message = "신청이 수락됐어요",
        meetingId = "m1", createdAtMillis = BASE - 30 * MINUTE, isRead = false,
    )
    val unreadNotice = Notification(
        id = "n2", type = NotificationType.NOTICE_WRITTEN, subject = "하체 데이",
        message = "호스트가 공지를 올렸어요 · 스쿼트 랙 예약했어요",
        meetingId = "m3", noticeId = "n3", // FakeNoticeRepository의 예시 공지 createdAtMillis = BASE - HOUR, isRead = false,
    )
    val readMate = Notification(
        id = "n3", type = NotificationType.MATE_REQUESTED, subject = "서연",
        message = "님이 메이트 신청을 보냈어요",
        userId = "u2", createdAtMillis = BASE - 24 * HOUR, isRead = true,
    )

    val all = listOf(unreadCheckIn, unreadAccepted, unreadNotice, readMate)

    // 모두 읽은 상태
    val allRead = all.map { it.copy(isRead = true) }

    // 알림이 하나도 없는 상태 (3-6-2)
    val empty: List<Notification> = emptyList()
}
