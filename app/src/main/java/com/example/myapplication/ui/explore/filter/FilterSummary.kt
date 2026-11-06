package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.explore.sportLabel
import com.example.myapplication.ui.record.bodyPartLabel
import com.example.myapplication.ui.record.hikingStyleLabel
import com.example.myapplication.ui.record.skillLevelLabel
import java.util.Calendar
import java.util.TimeZone

// 조건 설정 화면이 보여 주는 문구와 날짜 선택지를 만드는 순수 함수.

// 날짜 한 칸: 오늘이면 "오늘", 아니면 요일, 그 아래 "9/9"
data class DateOption(val startOfDayMillis: Long, val weekdayLabel: String, val dateLabel: String)

private val weekdays = listOf("일", "월", "화", "수", "목", "금", "토") // Calendar.DAY_OF_WEEK는 일요일이 1

// 오늘부터 [count]일. 날짜는 그날 0시(해당 시간대)로 정한다.
// 달력으로 고른 날짜([pickedStartOfDay])가 이 범위 밖이면 그 날짜부터 [count]일을 보여 줘서, 고른 날이 줄에서 보이게 한다.
fun dateOptions(
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
    count: Int = 5,
    pickedStartOfDay: Long? = null,
): List<DateOption> {
    val today = startOfDay(nowMillis, timeZone)
    val windowEnd = addDays(today, count - 1, timeZone)
    val start = if (pickedStartOfDay != null && pickedStartOfDay > windowEnd) pickedStartOfDay else today
    return (0 until count).map { offset ->
        val day = Calendar.getInstance(timeZone).apply { timeInMillis = addDays(start, offset, timeZone) }
        DateOption(
            startOfDayMillis = day.timeInMillis,
            weekdayLabel = if (day.timeInMillis == today) "오늘" else weekdays[day.get(Calendar.DAY_OF_WEEK) - 1],
            dateLabel = "${day.get(Calendar.MONTH) + 1}/${day.get(Calendar.DAY_OF_MONTH)}",
        )
    }
}

private fun startOfDay(millis: Long, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addDays(startOfDay: Long, days: Int, timeZone: TimeZone): Long =
    Calendar.getInstance(timeZone).apply { timeInMillis = startOfDay; add(Calendar.DAY_OF_YEAR, days) }.timeInMillis

// 날짜 선택 창은 고른 날을 'UTC 그날 0시'로 돌려준다. 우리가 쓰는 '그 시간대의 그날 0시'로 바꾼다.
fun localStartOfDay(utcMidnightMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMidnightMillis }
    return Calendar.getInstance(timeZone).apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}

// 반대로, 우리 날짜(그 시간대의 어느 시각)를 날짜 선택 창이 쓰는 'UTC 그날 0시'로 바꾼다.
fun utcMidnightOf(localMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
    val local = Calendar.getInstance(timeZone).apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}

// 분(하루 안)을 "07:00"으로
fun formatMinuteOfDay(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

// 4a-2 시간 범위 알약: "07:00 – 11:00 시작"
fun timeRangeLabel(range: TimeOfDayRange): String =
    "${formatMinuteOfDay(range.startMinute)} – ${formatMinuteOfDay(range.endMinute)} 시작"

// 페이스(초/km)를 5'30" 형태로. 기록 화면의 선택지 문구(~4'30", 7'00"+)와 달리 값 그대로 쓴다.
fun paceText(sec: Int): String = "${sec / 60}'${(sec % 60).toString().padStart(2, '0')}\""

private const val NONE = "관계없음"

// '기준' 줄 요약. 종목을 고르지 않았으면 null이다. (기준은 종목을 골랐을 때만 설정할 수 있다)
fun criteriaSummary(filter: MeetingFilter): String? = when (filter.sport) {
    null -> null
    Sport.HEALTH -> healthSummary(filter.health)
    Sport.RUNNING ->
        if (filter.running.criteriaOn && filter.running.paceRangeSec != null)
            "${paceText(filter.running.paceRangeSec.first)}–${paceText(filter.running.paceRangeSec.last)}"
        else NONE
    Sport.HIKING ->
        if (filter.hiking.criteriaOn && filter.hiking.style != null) hikingStyleLabel(filter.hiking.style) else NONE
}

private fun healthSummary(health: HealthFilter): String {
    if (health.parts.isEmpty()) return NONE
    val ordered = filterPartOrder.filter { it in health.parts }
    // 기준 설정이 꺼져 있으면 부위만 보여 주고 수준은 관계없다고 쓴다. (4a-3)
    if (!health.criteriaOn) return ordered.joinToString(" · ") { bodyPartLabel(it) } + " · 수준 $NONE"
    return ordered.joinToString(" · ") { part ->
        val detail = when (part) {
            BodyPart.ARM -> health.armLevel?.let(::skillLevelLabel)
            BodyPart.BACK -> health.ranges[part]?.let { "${it.first}–${it.last}회" }
            else -> health.ranges[part]?.let { "${it.first}–${it.last}kg" }
        }
        if (detail == null) bodyPartLabel(part) else "${bodyPartLabel(part)} $detail"
    }
}

fun locationSummary(filter: MeetingFilter): String = filter.district?.takeIf { it.isNotBlank() } ?: "지역 전체"

// '상세 조건' 줄 요약: "누구나 · 매너 30°C+" (성별을 정하지 않으면 '누구나', 매너온도를 정하지 않으면 '관계없음')
fun detailSummary(filter: MeetingFilter): String {
    val gender = genderLabel(filter.gender ?: GenderLimit.ANYONE)
    val manner = filter.minMannerTemperature?.let { "매너 ${it}°C+" } ?: "매너 $NONE"
    return "$gender · $manner"
}

// 탐색 검색창 첫 줄: "오늘 · 헬스". 날짜를 고르지 않았으면 날짜는 빼고, 종목을 고르지 않았으면 "모든 운동"이다.
fun searchTitle(filter: MeetingFilter, nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val date = filter.dateMillis?.let { picked ->
        val day = Calendar.getInstance(timeZone).apply { timeInMillis = picked }
        val today = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        if (day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        ) "오늘" else "${day.get(Calendar.MONTH) + 1}/${day.get(Calendar.DAY_OF_MONTH)}"
    }
    return listOfNotNull(date, filter.sport?.let(::sportLabel) ?: "모든 운동").joinToString(" · ")
}
