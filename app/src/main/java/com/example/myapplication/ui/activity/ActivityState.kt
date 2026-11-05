package com.example.myapplication.ui.activity

import java.util.Calendar
import java.util.TimeZone

// 활동 내역 화면(13-H, 13-H2, 13-P, 13-P2, 13-R)의 상태.
// 2026-10-30 범위 변경(헬스만): 디자인의 종목 필터(13-H)와 카드의 종목 칩은 만들지 않는다.
// 시각은 모두 epoch millis. 마감·평가 기한 같은 정책 시각은 서버가 계산해서 준다 (service-rules 0장).

enum class ActivityTab { HOST, JOINED }

// 내가 개설한 모임의 상태 (service-rules 6장)
enum class HostedStatus { RECRUITING, CLOSED, IN_PROGRESS, ENDED, CANCELLED }

data class HostedMeetingItem(
    val meetingId: String,
    val title: String,
    val startAt: Long,
    val confirmedCount: Int,
    val capacity: Int,
    val status: HostedStatus,
    val pendingApplicantCount: Int = 0,
    val deadlineAt: Long? = null, // 모집중일 때 '마감까지'
) {
    val isUpcoming: Boolean get() = status in setOf(HostedStatus.RECRUITING, HostedStatus.CLOSED, HostedStatus.IN_PROGRESS)
}

// 내가 신청·참여한 모임에서 나의 상태
enum class JoinedStatus(val label: String) {
    PENDING("신청 대기"), // (추정) 디자인에 없음
    CONFIRMED("참여 확정"),
    ATTENDED("참여 완료"),
    NO_SHOW("노쇼"),
    CANCELLED("모임 취소"), // (추정) 디자인에 없음
}

data class JoinedMeetingItem(
    val meetingId: String,
    val title: String,
    val startAt: Long,
    val placeName: String,
    val distanceKm: Double? = null, // 내 위치에서의 거리. 위치를 모르면 null
    val status: JoinedStatus,
) {
    val isUpcoming: Boolean get() = status == JoinedStatus.PENDING || status == JoinedStatus.CONFIRMED
}

// 13-R 평가 대기 카드 하나
data class ReviewPendingItem(
    val meetingId: String,
    val title: String,
    val endedAt: Long,
    val reviewDueAt: Long, // 평가 기한. 서버가 '종료 + 평가 기간'으로 계산
)

data class ActivityState(
    val joinedTotal: Int = 0, // 지금까지 참여한 모임 수 '참여 27'
    val hostedTotal: Int = 0, // 지금까지 개설한 모임 수 '개설 9'
    val reviewPending: List<ReviewPendingItem> = emptyList(),
    val selectedTab: ActivityTab = ActivityTab.HOST,
    val hosted: List<HostedMeetingItem> = emptyList(),
    val joined: List<JoinedMeetingItem> = emptyList(),
    val now: Long = 0L, // 남은 시간 계산 기준. ViewModel이 화면을 열 때 넣는다
) {
    val statsText: String get() = "참여 $joinedTotal · 개설 $hostedTotal"
    val reviewHeader: String get() = "평가 대기 ${reviewPending.size}"

    // (추정) 디자인의 탭 숫자가 목록 수와 맞지 않아 목록 전체 수로 둔다
    val hostTabLabel: String get() = "호스트 ${hosted.size}"
    val joinedTabLabel: String get() = "참여 ${joined.size}"

    val hostedUpcoming: List<HostedMeetingItem> get() = hosted.filter { it.isUpcoming }.sortedBy { it.startAt }
    val hostedPast: List<HostedMeetingItem> get() = hosted.filterNot { it.isUpcoming }.sortedByDescending { it.startAt }
    val joinedUpcoming: List<JoinedMeetingItem> get() = joined.filter { it.isUpcoming }.sortedBy { it.startAt }
    val joinedPast: List<JoinedMeetingItem> get() = joined.filterNot { it.isUpcoming }.sortedByDescending { it.startAt }
}

// ---------- 화면 글자 만들기 ----------

data class DateBox(val monthText: String, val dayText: String) // "9월", "8"

fun dateBox(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): DateBox {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
    return DateBox("${cal.get(Calendar.MONTH) + 1}월", cal.get(Calendar.DAY_OF_MONTH).toString())
}

// "오전 7:00", "오후 8:00"
fun clockText(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val half = if (hour < 12) "오전" else "오후"
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return "$half $hour12:${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}"
}

// 남은 시간 "3일", "5시간", "20분". 이미 지났으면 null
fun remainingText(now: Long, target: Long): String? {
    val diff = target - now
    if (diff <= 0) return null
    val minutes = diff / 60_000
    return when {
        minutes >= 24 * 60 -> "${minutes / (24 * 60)}일"
        minutes >= 60 -> "${minutes / 60}시간"
        else -> "${minutes.coerceAtLeast(1)}분"
    }
}

// 달력 날짜 차이 (오늘 = 0, 내일 = 1)
fun calendarDaysBetween(from: Long, to: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
    fun dayStart(t: Long) = Calendar.getInstance(timeZone).apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return Math.round((dayStart(to) - dayStart(from)) / 86_400_000.0).toInt()
}

// 13-H 카드 아래줄
fun HostedMeetingItem.metaText(now: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val time = clockText(startAt, timeZone)
    return when (status) {
        HostedStatus.RECRUITING -> deadlineAt?.let { remainingText(now, it) }?.let { "$time · 마감까지 $it" } ?: time
        HostedStatus.CLOSED -> remainingText(now, startAt)?.let { "$time · 시작까지 $it" } ?: time
        HostedStatus.IN_PROGRESS -> "$time · 진행중"
        HostedStatus.ENDED -> "$time 종료"
        HostedStatus.CANCELLED -> "$time · 취소됨"
    }
}

// 13-P 카드 아래줄 "망원한강공원 · 오전 6:30 · 0.8km"
fun JoinedMeetingItem.metaText(timeZone: TimeZone = TimeZone.getDefault()): String =
    listOfNotNull(placeName, clockText(startAt, timeZone), distanceKm?.let { distanceText(it) }).joinToString(" · ")

// 0.8 → "0.8km", 3.0 → "3km" (소수 첫째 자리)
fun distanceText(km: Double): String {
    val tenths = Math.round(km * 10)
    return if (tenths % 10 == 0L) "${tenths / 10}km" else "${tenths / 10}.${tenths % 10}km"
}

// 13-R "9/5 종료 · 5일 남음" / 마감 임박 "9/5 종료 · 오늘까지"
data class ReviewDueText(val text: String, val isUrgent: Boolean)

fun ReviewPendingItem.dueText(now: Long, timeZone: TimeZone = TimeZone.getDefault()): ReviewDueText {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = endedAt }
    val ended = "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)} 종료"
    val days = calendarDaysBetween(now, reviewDueAt, timeZone)
    return if (days <= 0) ReviewDueText("$ended · 오늘까지", isUrgent = true) else ReviewDueText("$ended · ${days}일 남음", isUrgent = false)
}
