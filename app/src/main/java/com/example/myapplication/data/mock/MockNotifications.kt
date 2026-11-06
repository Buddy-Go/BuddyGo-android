package com.example.myapplication.data.mock

import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType

// 읽은 알림과 읽지 않은 알림, 빈 목록을 모두 담는다. 시각은 MockMeetings와 같은 기준 시각에서 더하고 뺀 고정값이다.
object MockNotifications {
    private const val HOUR = 3_600_000L
    private const val BASE = 1_798_761_600_000L

    val unreadAccepted = Notification(
        id = "n1", type = NotificationType.JOIN_ACCEPTED, message = "'가슴·팔 같이 하실 분' 신청이 수락됐어요",
        meetingId = "m1", createdAtMillis = BASE - HOUR, isRead = false,
    )
    val unreadNotice = Notification(
        id = "n2", type = NotificationType.NOTICE_WRITTEN, message = "'북한산 둘레길 같이 가요'에 새 공지가 올라왔어요",
        meetingId = "m4", noticeId = "notice1", createdAtMillis = BASE - 3 * HOUR, isRead = false,
    )
    val readMate = Notification(
        id = "n3", type = NotificationType.MATE_REQUESTED, message = "지훈님이 메이트를 신청했어요",
        userId = "u3", createdAtMillis = BASE - 24 * HOUR, isRead = true,
    )

    val all = listOf(unreadAccepted, unreadNotice, readMate)

    // 모두 읽은 상태
    val allRead = all.map { it.copy(isRead = true) }

    // 알림이 하나도 없는 상태 (3-6-2)
    val empty: List<Notification> = emptyList()
}
