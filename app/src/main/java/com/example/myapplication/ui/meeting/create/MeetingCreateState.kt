package com.example.myapplication.ui.meeting.create

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.SkillLevel
import java.util.Calendar
import java.util.TimeZone


// 모임 개설 화면(10a, 10a-2)의 상태.
// 2026-10-30 범위 변경: 헬스 모임만 개설한다. 종목 선택·러닝·등산(10b, 10c)은 없다 (service-rules 4장).
// 값의 정의와 화면에 보일 글자(계산 속성)만 둔다. 값을 바꾸는 코드는 MeetingCreateViewModel에 있다.
// 개설 요청을 보낼 때 이 값들을 공용 Meeting 모델로 옮겨 담는다 (MeetingRepository에 개설 호출이 생기면).

// 운동 부위는 공용 모델 BodyPart를 쓴다. 화면 글자와 칩 순서만 여기서 정한다.
// 칩 순서는 10d 시트 기준: 가슴, 등, 팔, 하체, 어깨
val HEALTH_PART_ORDER = listOf(BodyPart.CHEST, BodyPart.BACK, BodyPart.ARM, BodyPart.LEGS, BodyPart.SHOULDER)

val BodyPart.label: String
    get() = when (this) {
        BodyPart.CHEST -> "가슴"
        BodyPart.BACK -> "등"
        BodyPart.ARM -> "팔"
        BodyPart.LEGS -> "하체"
        BodyPart.SHOULDER -> "어깨"
    }

// 프로필 기록 항목 이름 (service-rules 1장)
val BodyPart.recordLabel: String
    get() = when (this) {
        BodyPart.CHEST -> "벤치프레스 1RM"
        BodyPart.BACK -> "맨몸 턱걸이"
        BodyPart.ARM -> "숙련도"
        BodyPart.LEGS -> "스쿼트 1RM"
        BodyPart.SHOULDER -> "오버헤드 프레스 1RM"
    }

val SkillLevel.label: String
    get() = when (this) {
        SkillLevel.BEGINNER -> "초급"
        SkillLevel.INTERMEDIATE -> "중급"
        SkillLevel.ADVANCED -> "고급"
    }

// 부위별 기록을 화면 글자로. 기록이 없으면 null (service-rules 1장: null은 '미설정')
fun ExerciseRecord.textFor(part: BodyPart): String? = when (part) {
    BodyPart.CHEST -> benchPressKg?.let { "${it}kg" }
    BodyPart.BACK -> pullUpCount?.let { "${it}회" }
    BodyPart.LEGS -> squatKg?.let { "${it}kg" }
    BodyPart.SHOULDER -> overheadPressKg?.let { "${it}kg" }
    BodyPart.ARM -> armLevel?.label
}

val GenderLimit.label: String
    get() = when (this) {
        GenderLimit.ANYONE -> "누구나"
        GenderLimit.FEMALE_ONLY -> "여성만"
        GenderLimit.MALE_ONLY -> "남성만"
    }

// 모임 개설 화면 위에 뜨는 시트
enum class CreateSheet {
    HEALTH_CRITERIA, // 10d
    DESCRIPTION, // 10e
    TIME, // 10f
    DETAIL_ADDRESS, // 수정 모드에서 상세주소만 고친다 (장소 검색 화면 10g가 생기기 전까지 유일한 장소 수정)
}

// 상세주소 최대 글자 수. 정해진 값이 없어 임시로 둔다.
const val DETAIL_ADDRESS_MAX_LENGTH = 50

// 설명 최대 글자 수. service-rules 4장의 임시값이라 바뀌면 여기만 고친다.
const val DESCRIPTION_MAX_LENGTH = 300

// 헬스 기준: 운동 부위 1개 이상 + '내 기록 표시' (service-rules 4장)
data class HealthCriteria(
    val parts: List<BodyPart> = emptyList(),
    val showMyRecord: Boolean = true,
    // 개설 시점 내 프로필 기록 (UserRepository.getCurrentUser). 기록 없는 부위는 표시되지 않는다.
    val myRecord: ExerciseRecord = ExerciseRecord(),
) {
    val isFilled: Boolean get() = parts.isNotEmpty()

    val hasAnyRecord: Boolean get() = HEALTH_PART_ORDER.any { myRecord.textFor(it) != null }

    // 10a "가슴 80kg · 팔 중급" / 10a-2 "가슴 · 팔"
    val summary: String
        get() = HEALTH_PART_ORDER.filter { it in parts }.joinToString(" · ") { part ->
            val record = myRecord.textFor(part)
            if (showMyRecord && record != null) "${part.label} $record" else part.label
        }
}

data class PlaceSelection(
    val name: String,
    val detail: String? = null, // 상세주소(선택)
) {
    val summary: String get() = if (detail.isNullOrBlank()) name else "$name · $detail"
}

// 날짜 칩 하나. 10a "오늘 9/8", "화 9/9"
data class DateOption(
    val year: Int,
    val month: Int, // 1~12
    val day: Int,
    val topLabel: String, // "오늘" 또는 요일
) {
    val dateLabel: String get() = "$month/$day"
}

data class MeetingCreateState(
    val title: String = "",
    val dateOptions: List<DateOption> = emptyList(),
    val selectedDateIndex: Int? = null,
    val startMinutes: Int? = null, // 자정부터 분. 19:00 → 1140
    val durationMinutes: Int? = null,
    val healthCriteria: HealthCriteria = HealthCriteria(),
    val place: PlaceSelection? = null,
    val capacity: Int? = null, // 호스트 포함. 헬스 2~3명 (서버 설정값)
    val gender: GenderLimit = GenderLimit.ANYONE,
    val description: String = "",
    val openSheet: CreateSheet? = null,
    val isSubmitting: Boolean = false,
    val createdMeetingId: String? = null, // 개설되면 채워지고, 화면이 모임 상세로 넘어간다
    val errorMessage: String? = null,
    // ---- 수정 모드 (service-rules 4장): 기존 모임을 불러와 같은 화면으로 고친다 ----
    val editMeetingId: String? = null,
    val original: Meeting? = null,         // 수정 중인 모임 원본. 장소 좌표 같은 건 이걸 그대로 쓴다
    val lockedCore: Boolean = false,       // 확정 참가자(호스트 제외)가 있어 제목·설명·상세주소만 고칠 수 있다
    val updated: Boolean = false,          // 수정이 끝났다: 화면을 닫는다
) {
    val isEditMode: Boolean get() = editMeetingId != null
    val isCriteriaFilled: Boolean get() = healthCriteria.isFilled

    // 비어 있으면 null → 화면에서 '미입력'으로 보인다.
    val criteriaSummary: String? get() = if (isCriteriaFilled) healthCriteria.summary else null

    val placeSummary: String? get() = place?.summary

    // 10a "2명 · 누구나"
    val capacitySummary: String? get() = capacity?.let { "${it}명 · ${gender.label}" }

    // 한 줄로 보여 주고 넘치면 화면에서 '…'으로 자른다.
    val descriptionSummary: String?
        get() = description.lineSequence().firstOrNull { it.isNotBlank() }?.trim()

    // 10a "오후 7:00 – 8:30 · 1시간 30분"
    val timeSummary: String?
        get() {
            val start = startMinutes ?: return null
            val duration = durationMinutes ?: return null
            val end = start + duration
            val sameHalf = (start / 60 % 24 < 12) == (end / 60 % 24 < 12)
            val endText = if (sameHalf) clock(end) else "${half(end)} ${clock(end)}"
            return "${half(start)} ${clock(start)} – $endText · ${durationText(duration)}"
        }

    // 수정 요청에 담을 Meeting: 원본에 화면에서 바꾼 값을 덮어쓴다. 장소 이름·좌표는 그대로 두고 상세주소만 바뀐다.
    // 잠긴 항목은 서버가 무시하므로 같이 보내도 상관없다. 필수 값이 비어 있으면 null.
    fun toUpdatedMeeting(timeZone: TimeZone = TimeZone.getDefault()): Meeting? {
        val base = original ?: return null
        val edited = toMeeting(timeZone) ?: return null
        return base.copy(
            title = edited.title,
            criteria = edited.criteria,
            place = base.place.copy(detailAddress = place?.detail?.takeIf { it.isNotBlank() }),
            startAtMillis = edited.startAtMillis,
            durationMinutes = edited.durationMinutes,
            capacity = edited.capacity,
            genderLimit = edited.genderLimit,
            description = edited.description,
        )
    }

    // 필수: 제목, 날짜, 시간, 기준, 장소, 정원 (service-rules 4장). 설명은 선택.
    val canSubmit: Boolean
        get() = title.isNotBlank() &&
            selectedDateIndex != null &&
            startMinutes != null &&
            durationMinutes != null &&
            isCriteriaFilled &&
            place != null &&
            capacity != null

    // 개설 요청에 담을 공용 Meeting. 필수 값이 비어 있으면 null.
    // id·호스트·확정 인원·상태는 서버가 정한다.
    fun toMeeting(timeZone: TimeZone = TimeZone.getDefault()): Meeting? {
        if (!canSubmit) return null
        val date = dateOptions.getOrNull(selectedDateIndex ?: return null) ?: return null
        val start = startMinutes ?: return null
        val startAt = Calendar.getInstance(timeZone).apply {
            clear()
            set(date.year, date.month - 1, date.day, 0, 0, 0)
        }.timeInMillis + start * 60_000L
        val record = healthCriteria.myRecord.takeIf { healthCriteria.showMyRecord && healthCriteria.hasAnyRecord }
        return Meeting(
            title = title.trim(),
            criteria = MeetingCriteria.Health(parts = healthCriteria.parts.toSet(), hostRecord = record),
            place = MeetingPlace(name = place?.name.orEmpty(), detailAddress = place?.detail),
            startAtMillis = startAt,
            durationMinutes = durationMinutes ?: 0,
            capacity = capacity ?: 0,
            genderLimit = gender,
            description = description.trim(),
        )
    }

    private fun half(minutes: Int): String = if (minutes / 60 % 24 < 12) "오전" else "오후"

    private fun clock(minutes: Int): String {
        val hour24 = minutes / 60 % 24
        val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val minute = (minutes % 60).toString().padStart(2, '0')
        return "$hour12:$minute"
    }
}

// "1시간 30분", "1시간", "30분" (10a 시간 행, 10f 진행 시간)
fun durationText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}시간 ${m}분"
        h > 0 -> "${h}시간"
        else -> "${m}분"
    }
}

// 24시간 표기 "19:00" (10f). 자정을 넘기면 다음 날 시각으로 돌린다.
fun clock24(minutes: Int): String {
    val hour = minutes / 60 % 24
    val minute = minutes % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

// ---- 수정 모드 ----

// 오늘부터 count일의 날짜 칩. 고칠 모임의 날짜가 그 안에 없으면 그 날짜 칩을 맨 뒤에 하나 더 둔다.
fun buildDateOptions(
    nowMillis: Long,
    extraMillis: Long? = null,
    timeZone: TimeZone = TimeZone.getDefault(),
    count: Int = 5,
): List<DateOption> {
    val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")
    val today = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
    fun option(c: Calendar, isToday: Boolean) = DateOption(
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        if (isToday) "오늘" else weekdays[c.get(Calendar.DAY_OF_WEEK) - 1],
    )
    val cal = today.clone() as Calendar
    val options = List(count) { i -> option(cal, i == 0).also { cal.add(Calendar.DAY_OF_MONTH, 1) } }
    val extra = extraMillis?.let { Calendar.getInstance(timeZone).apply { timeInMillis = it } } ?: return options
    val extraOption = option(extra, false)
    val sameDay = { a: DateOption -> a.year == extraOption.year && a.month == extraOption.month && a.day == extraOption.day }
    return if (options.any(sameDay)) options else options + extraOption
}

// 고칠 모임을 화면 상태로 바꾼다. myRecord는 '내 기록 표시'를 다시 켤 때 보여 줄 현재 프로필 기록이다.
// 헬스 모임만 고칠 수 있다. 다른 종목이면 기준이 비어 있어 제출할 수 없고 안내를 띄운다.
fun editStateOf(
    meeting: Meeting,
    myRecord: ExerciseRecord,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): MeetingCreateState {
    val options = buildDateOptions(nowMillis, meeting.startAtMillis, timeZone)
    val start = Calendar.getInstance(timeZone).apply { timeInMillis = meeting.startAtMillis }
    val dateIndex = options.indexOfFirst {
        it.year == start.get(Calendar.YEAR) && it.month == start.get(Calendar.MONTH) + 1 && it.day == start.get(Calendar.DAY_OF_MONTH)
    }.takeIf { it >= 0 }
    val health = meeting.criteria as? MeetingCriteria.Health
    return MeetingCreateState(
        title = meeting.title,
        dateOptions = options,
        selectedDateIndex = dateIndex,
        startMinutes = start.get(Calendar.HOUR_OF_DAY) * 60 + start.get(Calendar.MINUTE),
        durationMinutes = meeting.durationMinutes,
        healthCriteria = HealthCriteria(
            parts = health?.parts?.sortedBy { HEALTH_PART_ORDER.indexOf(it) }.orEmpty(),
            showMyRecord = health?.hostRecord != null,
            myRecord = health?.hostRecord ?: myRecord,
        ),
        place = PlaceSelection(meeting.place.name, meeting.place.detailAddress),
        capacity = meeting.capacity,
        gender = meeting.genderLimit,
        description = meeting.description,
        editMeetingId = meeting.id,
        original = meeting,
        lockedCore = meeting.confirmedCount > 1,
        errorMessage = if (health == null) "이 종목은 아직 수정할 수 없어요" else null,
    )
}
