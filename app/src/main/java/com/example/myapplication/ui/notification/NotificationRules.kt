package com.example.myapplication.ui.notification

import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType
import com.example.myapplication.ui.navigation.Routes
import java.util.Calendar
import java.util.TimeZone

// 알림 목록 화면의 계산 규칙. 화면 없이 테스트할 수 있게 순수 함수로 둔다.

// 3-6 '오늘' / '이전' 두 묶음. 비어 있는 묶음은 제목도 보이지 않는다.
data class NotificationSections(val today: List<Notification>, val earlier: List<Notification>) {
    val isEmpty: Boolean get() = today.isEmpty() && earlier.isEmpty()
}

fun groupNotifications(
    list: List<Notification>,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): NotificationSections {
    // 기기 시계가 서버보다 느려 '미래'로 찍힌 알림도 오늘로 본다.
    val sorted = list.sortedByDescending { it.createdAtMillis }
    val (today, earlier) = sorted.partition { dayIndex(it.createdAtMillis, timeZone) >= dayIndex(nowMillis, timeZone) }
    return NotificationSections(today, earlier)
}

// "방금 전", "5분 전", "1시간 전", "어제", "3일 전", 일주일이 넘으면 "11월 2일"
fun formatRelativeTime(
    createdAtMillis: Long,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    // 미래로 찍힌 시각은 '지금'으로 본다.
    val created = createdAtMillis.coerceAtMost(nowMillis)
    val minutes = (nowMillis - created) / 60_000
    val days = dayIndex(nowMillis, timeZone) - dayIndex(created, timeZone)
    return when {
        days == 0L && minutes < 1 -> "방금 전"
        days == 0L && minutes < 60 -> "${minutes}분 전"
        days == 0L -> "${minutes / 60}시간 전"
        days == 1L -> "어제"
        days < 7 -> "${days}일 전"
        else -> Calendar.getInstance(timeZone).apply { timeInMillis = created }
            .let { "${it.get(Calendar.MONTH) + 1}월 ${it.get(Calendar.DAY_OF_MONTH)}일" }
    }
}

private fun dayIndex(millis: Long, timeZone: TimeZone): Long =
    (millis + timeZone.getOffset(millis)) / 86_400_000L

// 알림 종류별 아이콘 모양
enum class NotificationIcon { PIN_CHECK, MEGAPHONE, PEOPLE, CHECK, CLOSE }

fun NotificationType.icon(): NotificationIcon = when (this) {
    NotificationType.CHECKIN_OPENED, NotificationType.MEETING_STARTED -> NotificationIcon.PIN_CHECK
    NotificationType.NOTICE_WRITTEN, NotificationType.NOTICE_COMMENTED, NotificationType.NOTICE_REPLIED -> NotificationIcon.MEGAPHONE
    NotificationType.MATE_REQUESTED, NotificationType.MATE_ACCEPTED,
    NotificationType.MATE_MEETING_CREATED, NotificationType.MATE_MEETING_JOINED -> NotificationIcon.PEOPLE
    NotificationType.JOIN_REJECTED, NotificationType.MEETING_CANCELED_UNDERFILLED,
    NotificationType.MEETING_CANCELED_BY_HOST -> NotificationIcon.CLOSE
    else -> NotificationIcon.CHECK
}

// 알림을 눌렀을 때 가는 곳 (정의서 12장). 필요한 값이 없으면 갈 곳이 없다(null).
fun Notification.destination(): String? = when (type) {
    NotificationType.NOTICE_WRITTEN, NotificationType.NOTICE_COMMENTED, NotificationType.NOTICE_REPLIED ->
        if (meetingId != null && noticeId != null) Routes.noticeDetail(meetingId, noticeId) else meetingId?.let(Routes::meetingDetail)
    NotificationType.MATE_REQUESTED -> Routes.MATE_REQUEST
    NotificationType.MATE_ACCEPTED, NotificationType.MATE_MEETING_CREATED, NotificationType.MATE_MEETING_JOINED ->
        meetingId?.let(Routes::meetingDetail) ?: userId?.let(Routes::mateProfile)
    NotificationType.MEETING_ENDED -> meetingId?.let(Routes::reviewList)
    else -> meetingId?.let(Routes::meetingDetail)
}

// 굵은 앞부분과 뒤 문장을 잇는다. 뒤가 '님'으로 시작하면(서연님이 …) 붙이고, 아니면 한 칸 띄운다.
fun joinSubject(subject: String?, message: String): String = when {
    subject.isNullOrEmpty() -> message
    message.startsWith("님") -> subject + message
    else -> "$subject $message"
}

// 스크린 리더가 읽는 문장: 읽지 않은 알림이면 앞에 알려 준다.
fun Notification.spokenText(timeText: String): String =
    (if (isRead) "" else "읽지 않음, ") + joinSubject(subject, message) + ", " + timeText
