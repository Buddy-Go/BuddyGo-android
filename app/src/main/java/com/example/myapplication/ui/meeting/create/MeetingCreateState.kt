package com.example.myapplication.ui.meeting.create

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.SkillLevel

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
}

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
) {
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

    // 필수: 제목, 날짜, 시간, 기준, 장소, 정원 (service-rules 4장). 설명은 선택.
    val canSubmit: Boolean
        get() = title.isNotBlank() &&
            selectedDateIndex != null &&
            startMinutes != null &&
            durationMinutes != null &&
            isCriteriaFilled &&
            place != null &&
            capacity != null

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
