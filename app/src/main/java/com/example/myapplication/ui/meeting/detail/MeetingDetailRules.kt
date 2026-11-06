package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.CriteriaJudgement
import com.example.myapplication.data.model.CriteriaMatchResult
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.CriteriaMatch
import com.example.myapplication.ui.explore.filter.CriteriaDefaults
import com.example.myapplication.ui.explore.filter.filterPartOrder
import com.example.myapplication.ui.explore.filter.paceText
import com.example.myapplication.ui.explore.formatStartTime
import com.example.myapplication.ui.record.bodyPartLabel
import com.example.myapplication.ui.record.hikingStyleLabel
import com.example.myapplication.ui.record.skillLevelLabel
import java.util.Calendar
import java.util.TimeZone

// 모임 상세(5번 구간)가 보여 주는 문구와 버튼 규칙. 화면은 여기서 만든 값을 그리기만 한다.

// ---- 시각 ----

private const val MINUTE = 60_000L
private val weekdayNames = listOf("일", "월", "화", "수", "목", "금", "토")

// 데드라인 = 시작 1시간 30분 전. 이때까지 정원이 안 차면 자동 취소된다. (service-rules 5장)
const val DEADLINE_BEFORE_START_MINUTES = 90
fun deadlineMillis(meeting: Meeting): Long = meeting.startAtMillis - DEADLINE_BEFORE_START_MINUTES * MINUTE

// 체크인은 시작 10분 전부터 시작 후 10분까지 열린다. (7장, 기준값은 바뀔 수 있다)
const val CHECK_IN_OPENS_BEFORE_MINUTES = 10
const val CHECK_IN_CLOSES_AFTER_MINUTES = 10

// "9월 8일 (월) 오전 7:00 · 4시간 진행"
fun formatDetailDateTime(startAtMillis: Long, durationMinutes: Int, timeZone: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(timeZone).apply { timeInMillis = startAtMillis }
    val hour24 = c.get(Calendar.HOUR_OF_DAY)
    val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
    val period = if (hour24 < 12) "오전" else "오후"
    val date = "${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.DAY_OF_MONTH)}일 (${weekdayNames[c.get(Calendar.DAY_OF_WEEK) - 1]})"
    val time = "$period $hour12:${c.get(Calendar.MINUTE).toString().padStart(2, '0')}"
    val duration = formatDuration(durationMinutes)
    return if (duration.isEmpty()) "$date $time" else "$date $time · $duration 진행"
}

// 90분 → "1시간 30분", 60분 → "1시간", 45분 → "45분", 0분 → ""
fun formatDuration(minutes: Int): String {
    if (minutes <= 0) return ""
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}분"
        m == 0 -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}

// "마감까지 5시간". 하루 이상이면 일, 한 시간 미만이면 분 단위로 쓰고, 이미 지났으면 null이다.
fun formatUntilDeadline(meeting: Meeting, nowMillis: Long): String? {
    val left = deadlineMillis(meeting) - nowMillis
    if (left <= 0) return null
    val minutes = (left / MINUTE).toInt().coerceAtLeast(1)
    val text = when {
        minutes >= 24 * 60 -> "${minutes / (24 * 60)}일"
        minutes >= 60 -> "${minutes / 60}시간"
        else -> "${minutes}분"
    }
    return "마감까지 $text"
}

// ---- 상태 칩 ----

enum class ChipTone { Lime, Dark, Gray, Outlined }

data class StatusChip(val text: String, val tone: ChipTone)

fun statusChipsOf(meeting: Meeting, role: MeetingRole): List<StatusChip> {
    val main = when (meeting.status) {
        MeetingStatus.RECRUITING -> StatusChip("모집중 ${meeting.confirmedCount}/${meeting.capacity}", ChipTone.Lime)
        MeetingStatus.CLOSED -> StatusChip("마감 ${meeting.confirmedCount}/${meeting.capacity}", ChipTone.Dark)
        MeetingStatus.IN_PROGRESS -> StatusChip("진행중 · ${meeting.confirmedCount}명", ChipTone.Lime)
        MeetingStatus.ENDED -> StatusChip("종료", ChipTone.Gray)
        MeetingStatus.CANCELED -> StatusChip("취소됨", ChipTone.Gray)
    }
    // 내가 신청해서 호스트가 검토 중인 모임
    val pending = if (role == MeetingRole.PENDING && meeting.status == MeetingStatus.RECRUITING)
        listOf(StatusChip("신청 검토중", ChipTone.Outlined)) else emptyList()
    return listOf(main) + pending
}

// ---- 참가자 영역 ----

// 진행중이 되면 서버가 체크인 정보를 함께 내려 주므로, 명단이 있을 때만 체크인 수를 붙인다.
fun participantsTitle(meeting: Meeting): String = when (meeting.status) {
    MeetingStatus.IN_PROGRESS -> {
        val base = "참여자 ${meeting.confirmedCount} / ${meeting.capacity}"
        if (meeting.members.isEmpty()) base else "$base · 체크인 ${meeting.checkedInCount}"
    }
    MeetingStatus.ENDED ->
        "참여한 메이트 ${if (meeting.members.isEmpty()) meeting.confirmedCount else meeting.checkedInCount} / ${meeting.capacity}"
    else -> "참여자 ${meeting.confirmedCount} / ${meeting.capacity}"
}

// 명단이 공개되지 않는 사람에게는 숫자만 보인다. (5장)
const val MEMBERS_HIDDEN_NOTE = "참여자 목록은 마감 후 참여한 메이트에게만 보여요"
const val NOTICE_NOTE = "모임 안내는 공지로 전달돼요"

// 아바타 줄: 앞의 [shown]명 + "+나머지"
data class AvatarRow(val initials: List<String>, val extra: Int)

fun avatarRowOf(meeting: Meeting, shown: Int = 3): AvatarRow? {
    if (meeting.members.isEmpty()) return null
    val names = meeting.members.map { it.nickname.take(1) }
    return AvatarRow(names.take(shown), (names.size - shown).coerceAtLeast(0))
}

// 진행중·종료일 때 아바타 아래에 나오는 안내 상자 (제목, 보조 문구)
fun statusNoteOf(meeting: Meeting, nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Pair<String, String>? = when (meeting.status) {
    MeetingStatus.IN_PROGRESS -> {
        val start = formatStartTime(meeting.startAtMillis, nowMillis, timeZone).substringAfter(' ')
        "지금 진행 중이에요" to "$start 시작"
    }
    MeetingStatus.ENDED ->
        "${formatStartTime(meeting.endAtMillis, nowMillis, timeZone)}에 종료됐어요" to "함께한 메이트를 평가하면 매너온도가 올라가요"
    else -> null
}

// ---- 정보 카드 ----

// "합정 스포애니 · 마포구". 구는 주소에서 찾고, 없으면 장소 이름만 쓴다.
fun placeLine(meeting: Meeting): String {
    val district = Regex("""\S+구""").find(meeting.place.address)?.value
    return listOfNotNull(meeting.place.name.takeIf { it.isNotBlank() }, district).joinToString(" · ")
}

// 장소 시트와 주소 복사에 쓰는 주소. 도로명 주소 뒤에 호스트가 적은 상세주소를 붙인다.
fun fullAddress(meeting: Meeting): String =
    listOfNotNull(meeting.place.address.takeIf { it.isNotBlank() }, meeting.place.detailAddress?.takeIf { it.isNotBlank() }).joinToString(" ")

// 길찾기: 카카오맵 길찾기 링크. 앱이 있으면 앱이, 없으면 브라우저가 열린다.
fun routeUrl(meeting: Meeting): String =
    "https://map.kakao.com/link/to/${java.net.URLEncoder.encode(meeting.place.name, "UTF-8").replace("+", "%20")}," +
        "${meeting.place.latitude},${meeting.place.longitude}"

fun sportCriteriaTitle(sport: Sport): String = when (sport) {
    Sport.HEALTH -> "헬스 기준"
    Sport.RUNNING -> "러닝 기준"
    Sport.HIKING -> "등산 기준"
}

// 모임 기준 한 줄 요약. 호스트 기록에서 범위를 계산한다. (헬스 가슴·하체·어깨 ±10kg, 등 ±2회, 팔은 숙련도 그대로, 러닝 ±30초)
fun criteriaSummaryOf(meeting: Meeting): String = when (val c = meeting.criteria) {
    is MeetingCriteria.Health -> filterPartOrder.filter { it in c.parts }.joinToString(" · ") { part ->
        val detail = healthPartDetail(part, c.hostRecord)
        if (detail == null) bodyPartLabel(part) else "${bodyPartLabel(part)} $detail"
    }
    is MeetingCriteria.Running -> {
        val low = c.hostPaceSec - CriteriaDefaults.PACE_MARGIN_SEC
        val high = c.hostPaceSec + CriteriaDefaults.PACE_MARGIN_SEC
        "${formatKm(c.distanceKm)}km · ${paceText(low)}–${paceText(high)}"
    }
    is MeetingCriteria.Hiking -> hikingStyleLabel(c.style)
}

private fun formatKm(km: Float) = km.toString().removeSuffix(".0")

// 호스트가 기록을 숨긴 부위(기록이 없으면 null)는 이름만 보인다.
private fun healthPartDetail(part: BodyPart, record: ExerciseRecord?): String? = when (part) {
    BodyPart.ARM -> record?.armLevel?.let(::skillLevelLabel)
    BodyPart.BACK -> record?.pullUpCount?.let {
        "${(it - CriteriaDefaults.PULL_UP_MARGIN).coerceAtLeast(0)}–${it + CriteriaDefaults.PULL_UP_MARGIN}회"
    }
    else -> {
        val value = when (part) {
            BodyPart.CHEST -> record?.benchPressKg
            BodyPart.LEGS -> record?.squatKg
            else -> record?.overheadPressKg
        }
        value?.let { "${(it - CriteriaDefaults.WEIGHT_MARGIN_KG).coerceAtLeast(0)}–${it + CriteriaDefaults.WEIGHT_MARGIN_KG}kg" }
    }
}

// 서버 판정을 5i 행의 표시로 바꾼다. 판정이 없으면 null이라 판정 줄을 숨긴다.
data class JudgementView(val match: CriteriaMatch, val statusText: String)

fun judgementViewOf(meeting: Meeting): JudgementView? {
    val j = meeting.criteriaJudgement ?: return null
    val match = when (j.result) {
        CriteriaMatchResult.MATCH -> CriteriaMatch.Match
        CriteriaMatchResult.PARTIAL -> CriteriaMatch.Partial
        CriteriaMatchResult.MISMATCH -> CriteriaMatch.Mismatch
    }
    return JudgementView(match, judgementText(meeting.sport, j))
}

private fun judgementText(sport: Sport, j: CriteriaJudgement): String = when (j.result) {
    CriteriaMatchResult.MATCH -> when (sport) {
        Sport.HEALTH -> "내 기록 범위 안"
        Sport.RUNNING -> "내 페이스 범위 안"
        Sport.HIKING -> "내 성향과 맞음"
    }
    CriteriaMatchResult.PARTIAL ->
        if (j.outOfRangeParts.isEmpty()) "일부만 맞아요"
        else j.outOfRangeParts.sortedBy { filterPartOrder.indexOf(it) }.joinToString(", ") { bodyPartLabel(it) }.let { "${it}${topicParticle(it)} 범위 밖이에요" }
    CriteriaMatchResult.MISMATCH -> when (sport) {
        Sport.HIKING -> "내 성향과 맞지 않아요"
        Sport.RUNNING -> "내 페이스와 맞지 않아요"
        Sport.HEALTH -> "내 기록과 맞지 않아요"
    }
}

// 받침이 있으면 "은", 없으면 "는" (한글 글자가 아니면 "은")
fun topicParticle(word: String): String {
    val last = word.lastOrNull() ?: return "은"
    if (last !in '가'..'힣') return "은"
    return if ((last - '가') % 28 != 0) "은" else "는"
}

// ---- 하단 버튼 ----

enum class DetailAction { APPLY, CANCEL_APPLICATION, CHECK_IN, REVIEW, NONE }

// outlined는 보조 동작(신청 취소) 모양이다. enabled가 false면 눌리지 않는 회색 버튼이다.
data class BottomAction(
    val action: DetailAction,
    val title: String,
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val outlined: Boolean = false,
)

// 지금 이 사람에게 보여 줄 하단 큰 버튼. 호스트는 호스트 영역(HostSection)이 맡으므로 null이다.
fun bottomActionOf(
    meeting: Meeting,
    role: MeetingRole,
    myUserId: String,
    nowMillis: Long,
    myGender: com.example.myapplication.data.model.Gender? = null,
): BottomAction? {
    if (role == MeetingRole.HOST) return null
    val myMember = meeting.members.firstOrNull { it.userId == myUserId }
    val iCheckedIn = myMember?.checkedIn == true
    val myCheckInAt = myMember?.checkedInAtMillis
    return when (meeting.status) {
        MeetingStatus.RECRUITING -> when (role) {
            // 성별 조건이 안 맞으면 서버가 막는다. 미리 알려 줘서 헛걸음을 줄인다. (5장)
            MeetingRole.NONE -> if (!genderAllowed(meeting.genderLimit, myGender))
                BottomAction(DetailAction.NONE, "신청할 수 없어요", "성별 조건이 맞지 않아요", enabled = false)
            else BottomAction(
                DetailAction.APPLY, "참여 신청하기",
                listOfNotNull(formatUntilDeadline(meeting, nowMillis), "남은 자리 ${(meeting.capacity - meeting.confirmedCount).coerceAtLeast(0)}").joinToString(" · "),
            )
            MeetingRole.PENDING -> BottomAction(
                DetailAction.CANCEL_APPLICATION, "신청 취소하기", "호스트가 확인하면 알림을 보내드릴게요", outlined = true,
            )
            else -> BottomAction(DetailAction.NONE, "참여 확정", formatUntilDeadline(meeting, nowMillis), enabled = false)
        }
        MeetingStatus.CLOSED ->
            if (role == MeetingRole.CONFIRMED && nowMillis >= meeting.startAtMillis - CHECK_IN_OPENS_BEFORE_MINUTES * MINUTE)
                checkInAction(meeting, iCheckedIn, nowMillis, myCheckInAt)
            else BottomAction(DetailAction.NONE, "모임 마감", enabled = false)
        MeetingStatus.IN_PROGRESS ->
            if (role == MeetingRole.CONFIRMED) checkInAction(meeting, iCheckedIn, nowMillis, myCheckInAt)
            else BottomAction(DetailAction.NONE, "진행중인 모임", enabled = false)
        MeetingStatus.ENDED ->
            if (role == MeetingRole.CONFIRMED && iCheckedIn)
                BottomAction(DetailAction.REVIEW, "메이트 평가하기", "${meeting.confirmedCount}명 중 ${meeting.checkedInCount}명이 참여 완료")
            else BottomAction(DetailAction.NONE, "종료된 모임", enabled = false)
        MeetingStatus.CANCELED -> BottomAction(DetailAction.NONE, "취소된 모임", enabled = false)
    }
}

// 체크인은 한 번만 할 수 있고, 시작 10분 전부터 시작 후 10분까지만 열린다. (7장)
private fun checkInAction(meeting: Meeting, alreadyCheckedIn: Boolean, nowMillis: Long, checkedInAtMillis: Long?): BottomAction {
    val opens = meeting.startAtMillis - CHECK_IN_OPENS_BEFORE_MINUTES * MINUTE
    val closes = meeting.startAtMillis + CHECK_IN_CLOSES_AFTER_MINUTES * MINUTE
    return when {
        alreadyCheckedIn -> BottomAction(DetailAction.NONE, "체크인 완료", checkedInAtMillis?.let { formatCheckInTime(it) }, enabled = false)
        nowMillis < opens -> BottomAction(DetailAction.NONE, "체크인하기", "시작 10분 전부터 할 수 있어요", enabled = false)
        nowMillis > closes -> BottomAction(DetailAction.NONE, "체크인하기", "체크인 시간이 지났어요", enabled = false)
        else -> BottomAction(DetailAction.CHECK_IN, "체크인하기")
    }
}
