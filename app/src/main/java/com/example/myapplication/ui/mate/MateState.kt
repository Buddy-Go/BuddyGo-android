package com.example.myapplication.ui.mate

import com.example.myapplication.data.model.MateActivity
import com.example.myapplication.data.model.MateActivityKind
import com.example.myapplication.data.model.MateProfile
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.data.model.MateRequest
import com.example.myapplication.data.model.MateSummary
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.sportLabel
import com.example.myapplication.data.model.WorkedOutPerson
import java.util.Calendar
import java.util.TimeZone

// 메이트 화면들의 상태 (11a·11b 탭 화면, 11c 받은 신청, 15 프로필).
// 15의 '자주 하는 운동'은 만들지 않는다. 11b의 종목 칩(11b-2)과 필터 결과 빈 상태(11b-3)는 모든 종목으로 만든다.

enum class MateTab { MATES, WORKED_OUT }

// 11a·11b
data class MateState(
    val selectedTab: MateTab = MateTab.MATES,
    val mates: List<MateSummary> = emptyList(),
    val workedOut: List<WorkedOutPerson> = emptyList(),
    val pendingRequestCount: Int = 0,
    val searchQuery: String = "",
    val sportFilter: Sport? = null, // 11b-2 종목 칩. null이면 '전체'
    val loadFailed: Boolean = false, // 목록을 불러오지 못함 → 16g 오류 화면
    val isLoading: Boolean = true,
    val now: Long = 0L,
) {
    // 받은 신청이 있을 때만 위에 배너 (11a-2에는 없음)
    val bannerText: String? get() = if (pendingRequestCount > 0) "메이트 신청 ${pendingRequestCount}명이 기다리고 있어요" else null

    // 11b-2 칩 숫자: 이름 찾기와 상관없이 그 종목을 함께한 사람 수
    fun sportCount(sport: Sport?): Int = if (sport == null) workedOut.size else workedOut.count { sport in it.sports }

    // 11b 이름으로 찾기 + 11b-2 종목 칩 (둘 다 맞는 사람만)
    val filteredWorkedOut: List<WorkedOutPerson>
        get() {
            val q = searchQuery.trim()
            return workedOut.filter { (sportFilter == null || sportFilter in it.sports) && (q.isEmpty() || it.nickname.contains(q)) }
        }

    // 11b·11b-3 빈 문구. 종목 칩 문구는 디자인(11b-3), 나머지는 임시
    val workedOutEmptyText: String
        get() = when {
            workedOut.isEmpty() -> "아직 함께 운동한 사람이 없어요"
            sportFilter != null && sportCount(sportFilter) == 0 -> "${sportLabel(sportFilter)}을 함께한 사람이 아직 없어요"
            else -> "'${searchQuery.trim()}' 이름의 사람이 없어요"
        }
}

// 11c
data class MateRequestState(
    val requests: List<MateRequest> = emptyList(),
    val loadFailed: Boolean = false, // 신청 목록을 불러오지 못함 → 16g 오류 화면
    val processingRequestId: String? = null,
    val isLoading: Boolean = true,
) {
    val title: String get() = if (requests.isEmpty()) "메이트 신청" else "메이트 신청 ${requests.size}"
}

// 15a 메이트 / 15b 메이트 아님 / 15c 신청 보냄
enum class ProfileButton { REQUEST, REQUESTED, NONE }

data class MateProfileState(
    val profile: MateProfile? = null,
    val loadFailed: Boolean = false, // 프로필을 불러오지 못함 → 16g 오류 화면
    val isSending: Boolean = false,
    val isRemoved: Boolean = false,
    val isBlocked: Boolean = false, // 차단하면 화면을 닫는다
    val reportedMessage: String? = null, // 신고를 보내면 잠깐 알린다
    val now: Long = 0L,
) {
    val isMate: Boolean get() = profile?.relation == MateRelation.MATE

    // 신청 버튼은 함께 운동한 적이 있을 때만 (service-rules 10장, 13장 '15b 수정 필요')
    val bottomButton: ProfileButton
        get() {
            val p = profile ?: return ProfileButton.NONE
            return when (p.relation) {
                MateRelation.MATE -> ProfileButton.NONE
                MateRelation.REQUESTED -> ProfileButton.REQUESTED
                MateRelation.NONE -> if (p.workedOutTogether) ProfileButton.REQUEST else ProfileButton.NONE
            }
        }

    // ⋯ 메뉴: 메이트일 때 알림 켜기·끄기와 메이트 끊기, 누구에게나 신고하기·차단하기. 화면 디자인이 없어 Material 기본 메뉴
    val showMenu: Boolean get() = profile != null

    val notificationMenuLabel: String
        get() = if (profile?.notificationEnabled == false) "이 메이트 알림 켜기" else "이 메이트 알림 끄기"

    val reviewHeader: String get() = "후기 ${profile?.reviewCount ?: 0}"
}

// ---------- 화면 글자 만들기 ----------

// "매너온도 42.5°C"
fun mannerText(temperature: Float): String {
    val tenths = Math.round(temperature * 10)
    return "매너온도 ${tenths / 10}.${Math.abs(tenths % 10)}°C"
}

// 11c "매너온도 42.5°C · 참여 18회"
fun MateRequest.infoText(): String = "${mannerText(mannerTemperature)} · 참여 ${joinedCount}회"

// "9/14"
fun monthDayText(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
    return "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
}

// 11b "마지막 운동 9/14"
fun WorkedOutPerson.lastText(timeZone: TimeZone = TimeZone.getDefault()): String =
    "마지막 운동 ${monthDayText(lastWorkedOutAtMillis, timeZone)}"

// "오늘 오후 8:00" / "내일 오전 6:30" / "토요일 오후 2:00" / 일주일 넘으면 "9/20 오후 2:00"
fun whenText(now: Long, target: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val t = Calendar.getInstance(timeZone).apply { timeInMillis = target }
    val hour = t.get(Calendar.HOUR_OF_DAY)
    val clock = "${if (hour < 12) "오전" else "오후"} ${if (hour % 12 == 0) 12 else hour % 12}:${t.get(Calendar.MINUTE).toString().padStart(2, '0')}"
    val days = calendarDaysBetween(now, target, timeZone)
    val day = when {
        days == 0 -> "오늘"
        days == 1 -> "내일"
        days in 2..6 -> listOf("일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일")[t.get(Calendar.DAY_OF_WEEK) - 1]
        else -> monthDayText(target, timeZone)
    }
    return "$day $clock"
}

fun calendarDaysBetween(from: Long, to: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
    fun dayStart(x: Long) = Calendar.getInstance(timeZone).apply {
        timeInMillis = x
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return Math.round((dayStart(to) - dayStart(from)) / 86_400_000.0).toInt()
}

// 11a 메이트 줄 아래 글자. 지금 운동 중이면 초록 점과 함께
data class MateLine(val text: String, val isLive: Boolean, val sport: Sport? = null)

fun MateSummary.line(now: Long, timeZone: TimeZone = TimeZone.getDefault()): MateLine {
    val a = nextActivity ?: return MateLine("예정된 모임 없어요", isLive = false)
    return if (a.kind == MateActivityKind.EXERCISING_NOW) {
        MateLine("지금 운동 중 · ${sportLabel(a.sport)}", isLive = true)
    } else {
        MateLine("${a.title} · ${whenText(now, a.startAtMillis, timeZone)}", isLive = false, sport = a.sport)
    }
}

// 15a 현재 활동 카드 아래줄 "에이블짐 망원점 · 내일 오전 7:00"
fun MateActivity.cardMeta(now: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
    if (kind == MateActivityKind.EXERCISING_NOW) "$placeName · 지금 운동 중" else "$placeName · ${whenText(now, startAtMillis, timeZone)}"
