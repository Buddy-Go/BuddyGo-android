package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import java.util.Calendar
import java.util.TimeZone

// 검색 조건 (service-rules 3장). 아무것도 정하지 않은 상태(기본값)가 '전체'다.
// 규칙 판정은 모두 이 파일의 순수 함수에서 하고, 화면은 값을 바꾸고 보여 주기만 한다.

// 시간대 4구간. 모임 시작 시각이 [startHour, endHour)에 들어가면 맞다.
enum class TimeSlot(val startHour: Int, val endHour: Int) {
    DAWN(0, 6), MORNING(6, 12), AFTERNOON(12, 18), EVENING(18, 24);

    val label: String get() = "%02d–%02d".format(startHour, endHour)
}

// 하루 안의 시간 범위(분 단위, 시작·끝 포함). 직접 고른 시간 범위(4f)다.
data class TimeOfDayRange(val startMinute: Int, val endMinute: Int) {
    init {
        require(startMinute in 0..MAX_MINUTE && endMinute in 0..MAX_MINUTE) { "하루를 벗어난 시각" }
        require(startMinute <= endMinute) { "시작이 끝보다 늦음" }
    }

    operator fun contains(minuteOfDay: Int) = minuteOfDay in startMinute..endMinute

    // 시작을 바꿨는데 끝보다 늦으면 끝도 같이 옮겨서 범위가 뒤집히지 않게 한다.
    fun withStart(minute: Int): TimeOfDayRange {
        val start = minute.coerceIn(0, MAX_MINUTE)
        return TimeOfDayRange(start, maxOf(start, endMinute))
    }

    // 끝을 바꿨는데 시작보다 이르면 시작도 같이 옮긴다.
    fun withEnd(minute: Int): TimeOfDayRange {
        val end = minute.coerceIn(0, MAX_MINUTE)
        return TimeOfDayRange(minOf(startMinute, end), end)
    }

    companion object {
        const val MAX_MINUTE = 24 * 60 - 1
        const val STEP_MINUTE = 10 // 시간 범위 시트에서 분은 10분 단위로 고른다

        // 시트를 처음 열 때 보이는 값 (디자인 4f 예시)
        val DEFAULT = TimeOfDayRange(7 * 60, 11 * 60)
    }
}

// 헬스 기준. 부위는 AND, 수준 범위는 '기준 설정'을 켰을 때만 쓴다.
data class HealthFilter(
    val parts: Set<BodyPart> = emptySet(),
    val criteriaOn: Boolean = false,
    // 가슴·하체·어깨는 kg, 등은 턱걸이 횟수. 범위가 없는 부위는 직접 정하기 전까지 비교하지 않는다.
    val ranges: Map<BodyPart, IntRange> = emptyMap(),
    val armLevel: SkillLevel? = null,
)

// 러닝 기준: 켜면 페이스(초/km) 범위로 거른다.
data class RunningFilter(val criteriaOn: Boolean = false, val paceRangeSec: IntRange? = null)

// 등산 기준: 켜면 선택한 성향(취미/전문)으로 거른다.
data class HikingFilter(val criteriaOn: Boolean = false, val style: HikingStyle? = null)

data class MeetingFilter(
    val sport: Sport? = null,
    val dateMillis: Long? = null,               // 고른 날짜(그날 어느 시각이든 상관없음). null이면 날짜 무관
    val timeSlots: Set<TimeSlot> = emptySet(),  // 복수 선택 가능
    val customRange: TimeOfDayRange? = null,    // 있으면 시간대 선택보다 우선한다
    val health: HealthFilter = HealthFilter(),
    val running: RunningFilter = RunningFilter(),
    val hiking: HikingFilter = HikingFilter(),
    val district: String? = null,               // 구·동 하나 (예: "마포구", "서교동")
    val gender: GenderLimit? = null,            // null이면 성별 무관. 모임의 성별 조건과 같은 것만 남긴다
    val minMannerTemperature: Int? = null,      // 호스트 매너온도 하한 (30/50/70)
) {
    // 시간 조건이 걸려 있는지 (4a-2 '07:00 – 11:00 시작' 표시)
    val hasTimeCondition: Boolean get() = customRange != null || timeSlots.isNotEmpty()

    // 기준은 종목을 골랐을 때만 설정할 수 있다. 종목을 바꾸면 이전 종목의 기준은 지운다.
    fun withSport(newSport: Sport?): MeetingFilter =
        if (newSport == sport) this
        else copy(sport = newSport, health = HealthFilter(), running = RunningFilter(), hiking = HikingFilter())

    // 직접 고른 시간 범위와 시간대 선택은 함께 쓰지 않는다. 하나를 고르면 다른 하나는 지운다.
    fun withTimeSlots(slots: Set<TimeSlot>): MeetingFilter = copy(timeSlots = slots, customRange = null)
    fun withCustomRange(range: TimeOfDayRange?): MeetingFilter =
        copy(customRange = range, timeSlots = if (range != null) emptySet() else timeSlots)

    // 이 모임이 조건에 맞는지. 모든 조건은 AND다.
    fun matches(meeting: Meeting, timeZone: TimeZone = TimeZone.getDefault()): Boolean =
        matchesSport(meeting) &&
            matchesTime(meeting.startAtMillis, timeZone) &&
            matchesCriteria(meeting) &&
            matchesDistrict(meeting) &&
            (gender == null || meeting.genderLimit == gender) &&
            (minMannerTemperature == null || meeting.hostMannerTemperature >= minMannerTemperature)

    private fun matchesSport(meeting: Meeting) = sport == null || meeting.sport == sport

    private fun matchesTime(startAtMillis: Long, timeZone: TimeZone): Boolean {
        val start = Calendar.getInstance(timeZone).apply { timeInMillis = startAtMillis }
        if (dateMillis != null) {
            val day = Calendar.getInstance(timeZone).apply { timeInMillis = dateMillis }
            val sameDay = start.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                start.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
            if (!sameDay) return false
        }
        val minuteOfDay = start.get(Calendar.HOUR_OF_DAY) * 60 + start.get(Calendar.MINUTE)
        return when {
            customRange != null -> minuteOfDay in customRange
            timeSlots.isNotEmpty() -> timeSlots.any { minuteOfDay in it.startHour * 60 until it.endHour * 60 }
            else -> true
        }
    }

    // 종목 기준. 종목을 고르지 않았으면 기준은 보지 않는다.
    private fun matchesCriteria(meeting: Meeting): Boolean = when (val c = meeting.criteria) {
        is MeetingCriteria.Health -> sport != Sport.HEALTH || matchesHealth(c)
        is MeetingCriteria.Running -> sport != Sport.RUNNING || !running.criteriaOn ||
            running.paceRangeSec == null || c.hostPaceSec in running.paceRangeSec
        is MeetingCriteria.Hiking -> sport != Sport.HIKING || !hiking.criteriaOn ||
            hiking.style == null || c.style == hiking.style
    }

    private fun matchesHealth(c: MeetingCriteria.Health): Boolean {
        // 부위: 고른 부위를 모두 포함하는 모임만 (모임에 다른 부위가 더 있어도 된다)
        if (!c.parts.containsAll(health.parts)) return false
        if (!health.criteriaOn) return true
        // 기준 설정: 범위를 정한 부위 중 하나라도 호스트 기록이 범위 안이면 포함한다.
        val checkable = health.parts.filter { it == BodyPart.ARM && health.armLevel != null || health.ranges[it] != null }
        if (checkable.isEmpty()) return true
        // 호스트가 기록을 숨겼으면(hostRecord == null) 비교할 수 없으므로 범위 안으로 치지 않는다.
        val record = c.hostRecord ?: return false
        return checkable.any { isInRange(it, record) }
    }

    private fun isInRange(part: BodyPart, record: ExerciseRecord): Boolean = when (part) {
        BodyPart.ARM -> record.armLevel != null && record.armLevel == health.armLevel
        else -> {
            val value = when (part) {
                BodyPart.CHEST -> record.benchPressKg
                BodyPart.BACK -> record.pullUpCount
                BodyPart.LEGS -> record.squatKg
                else -> record.overheadPressKg
            }
            value != null && health.ranges[part]?.contains(value) == true
        }
    }

    // 구·동은 도로명 주소나 지번에 그 지역의 이름이 모두 들어 있으면 맞다. (임시: 서버가 행정구역 코드를 주면 코드로 비교한다)
    // 조건은 "대구 달서구 도원동"처럼 전체 이름이라서 "달서구"와 "도원동"이 둘 다 있어야 한다. 이름만 같은 다른 도시의 동은 걸러진다.
    // 시도가 붙은 조건은 시도도 맞아야 한다("서울 중구"에 "대구 중구"가 섞이지 않게). 서버 주소와 쓰는 말이 다를 수 있어
    // ("대구" / "대구광역시") 시도는 앞 두 글자로만 비교한다. "마포구"처럼 이름 하나만 저장된 조건은 시도 없이 쓴다.
    private fun matchesDistrict(meeting: Meeting): Boolean {
        val name = district ?: return true
        val parts = districtParts(name)
        if (parts.isEmpty()) return true
        val sido = name.trim().split(Regex("\\s+")).takeIf { it.size >= 2 }?.first()?.take(2)
        fun inAddress(address: String) = (sido == null || sido in address) && parts.all { it in address }
        return inAddress(meeting.place.address) || inAddress(meeting.place.lotAddress)
    }
}

// 지역 이름을 시도를 뺀 조각으로 나눈다. "대구 달서구 도원동" → [달서구, 도원동], "서울 마포구" → [마포구], "마포구" → [마포구], "서울" → [서울].
internal fun districtParts(district: String): List<String> {
    val parts = district.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return if (parts.size >= 2) parts.drop(1) else parts
}
