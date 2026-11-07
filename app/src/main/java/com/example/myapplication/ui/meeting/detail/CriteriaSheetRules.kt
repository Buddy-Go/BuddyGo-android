package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.ui.explore.filter.CriteriaDefaults
import com.example.myapplication.ui.explore.filter.myValue
import com.example.myapplication.ui.explore.filter.weightRangeAround
import com.example.myapplication.ui.explore.filter.paceText

// 기준 시트(5f 등산, 5h 헬스)에 보여 주는 '내 기록과의 비교' 규칙. 참고용이라 신청을 막지 않는다. (service-rules 8장)
// 모임 상세의 전체 판정(맞음/일부만/안 맞음)은 서버가 계산하고, 여기서는 시트 안의 부위별 안내만 만든다.

enum class PartVerdict { INSIDE, ABOVE, BELOW, NO_MY_RECORD, HOST_HIDDEN }

// 호스트 기록 기준의 범위: 가슴·하체·어깨 ±10kg, 등 ±2회. 팔은 숙련도라 범위가 없다.
// 호스트가 기록을 숨겼으면 null이다.
fun hostRangeOf(part: BodyPart, hostRecord: ExerciseRecord?): IntRange? {
    val host = myValue(part, hostRecord) ?: return null
    if (part != BodyPart.BACK) return weightRangeAround(host)
    return (host - CriteriaDefaults.PULL_UP_MARGIN).coerceAtLeast(0)..(host + CriteriaDefaults.PULL_UP_MARGIN)
}

fun unitOf(part: BodyPart): String = if (part == BodyPart.BACK) "회" else "kg"

fun verdictOf(part: BodyPart, hostRecord: ExerciseRecord?, myRecord: ExerciseRecord?): PartVerdict {
    val range = hostRangeOf(part, hostRecord) ?: return PartVerdict.HOST_HIDDEN
    val mine = myValue(part, myRecord) ?: return PartVerdict.NO_MY_RECORD
    return when {
        mine > range.last -> PartVerdict.ABOVE
        mine < range.first -> PartVerdict.BELOW
        else -> PartVerdict.INSIDE
    }
}

// "내 기록 95kg · 범위보다 높아요". 내 기록이 없거나 호스트가 숨겼으면 그 사실을 알려 준다.
fun partNote(part: BodyPart, hostRecord: ExerciseRecord?, myRecord: ExerciseRecord?): String {
    val mine = myValue(part, myRecord)
    val mineText = mine?.let { "내 기록 $it${unitOf(part)}" }
    return when (verdictOf(part, hostRecord, myRecord)) {
        PartVerdict.INSIDE -> "$mineText · 범위 안이에요"
        PartVerdict.ABOVE -> "$mineText · 범위보다 높아요"
        PartVerdict.BELOW -> "$mineText · 범위보다 낮아요"
        PartVerdict.NO_MY_RECORD -> "내 기록을 아직 안 적었어요"
        PartVerdict.HOST_HIDDEN -> "호스트가 기록을 공개하지 않았어요"
    }
}

// 팔 숙련도: 같으면 true. 호스트나 내 기록이 없으면 null(비교할 수 없음)
fun armMatches(hostRecord: ExerciseRecord?, myRecord: ExerciseRecord?): Boolean? {
    val host = hostRecord?.armLevel ?: return null
    val mine = myRecord?.armLevel ?: return null
    return host == mine
}

fun armNote(hostRecord: ExerciseRecord?, myRecord: ExerciseRecord?): String {
    val mine = myRecord?.armLevel ?: return "내 기록을 아직 안 적었어요"
    val label = armLevelLabel(mine)
    val base = "내 숙련도는 $label${copula(label)}"
    return when (armMatches(hostRecord, myRecord)) {
        true -> "$base · 기준 충족"
        false -> "$base · 기준과 달라요"
        null -> base
    }
}

private fun armLevelLabel(level: SkillLevel) = when (level) {
    SkillLevel.BEGINNER -> "초급"
    SkillLevel.INTERMEDIATE -> "중급"
    SkillLevel.ADVANCED -> "고급"
}

// 팔 숙련도별 설명 (5h는 중급만 보이지만 세 가지 모두 같은 틀의 문구를 쓴다)
fun armLevelDescription(level: SkillLevel): String = when (level) {
    SkillLevel.BEGINNER -> "가벼운 무게로 자세를 익히는 중인 수준"
    SkillLevel.INTERMEDIATE -> "덤벨컬·케이블 푸시다운 등 기본 팔 운동을 혼자 진행할 수 있는 수준"
    SkillLevel.ADVANCED -> "다양한 팔 운동을 스스로 구성해 고중량으로 진행할 수 있는 수준"
}

// 등산 성향 설명 (5f)
fun hikingStyleDescription(style: HikingStyle): String = when (style) {
    HikingStyle.HOBBY -> "풍경 보며 쉬어가는 속도로 걸어요. 둘레길·근교 산 위주"
    HikingStyle.PRO -> "정해진 페이스로 오르고 장거리 종주도 함께해요"
}

// "내 등산 성향은 취미예요 · 이 모임과 맞아요". 내 성향이 없으면 null이다.
fun hikingNote(host: HikingStyle, myRecord: ExerciseRecord?): String? {
    val mine = myRecord?.hikingStyle ?: return null
    val label = if (mine == HikingStyle.HOBBY) "취미" else "전문"
    return "내 등산 성향은 ${label}${copula(label)} · ${if (mine == host) "이 모임과 맞아요" else "이 모임과 달라요"}"
}

// 러닝: 호스트 페이스 ±30초. "내 페이스 5'30" · 범위 안이에요" (디자인 프레임 없음)
fun runningNote(hostPaceSec: Int, myRecord: ExerciseRecord?): String {
    val mine = myRecord?.runningPaceSec ?: return "내 기록을 아직 안 적었어요"
    val margin = CriteriaDefaults.PACE_MARGIN_SEC
    val text = paceText(mine)
    return when {
        mine < hostPaceSec - margin -> "내 페이스 $text · 범위보다 빨라요"
        mine > hostPaceSec + margin -> "내 페이스 $text · 범위보다 느려요"
        else -> "내 페이스 $text · 범위 안이에요"
    }
}

// 읽기 전용 막대의 눈금 범위. 호스트 범위와 내 기록이 모두 보이도록 양쪽에 여유를 두고 10 단위로 맞춘다.
data class BarScale(val min: Int, val max: Int)

fun barScaleOf(range: IntRange, mine: Int?, part: BodyPart): BarScale {
    val pad = if (part == BodyPart.BACK) 3 else 20
    val unit = if (part == BodyPart.BACK) 1 else 10
    val low = minOf(range.first, mine ?: range.first) - pad
    val high = maxOf(range.last, mine ?: range.last) + pad
    val min = (low.coerceAtLeast(0) / unit) * unit
    val max = ((high + unit - 1) / unit) * unit
    return BarScale(min, max)
}

// 서술격 조사: 받침이 있으면 "이에요", 없으면 "예요" (취미예요, 전문이에요)
fun copula(word: String): String {
    val last = word.lastOrNull() ?: return "이에요"
    if (last !in '가'..'힣') return "이에요"
    return if ((last - '가') % 28 != 0) "이에요" else "예요"
}
