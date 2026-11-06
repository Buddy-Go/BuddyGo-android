package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel

// 조건 설정의 '기준' 규칙 (service-rules 3장). 화면은 이 함수들만 부르고 규칙은 여기서만 정한다.

object CriteriaDefaults {
    const val WEIGHT_MARGIN_KG = 10     // 헬스 중량: 내 기록 ±10kg
    const val PULL_UP_MARGIN = 2        // 헬스 턱걸이: 내 기록 ±2회
    const val PACE_MARGIN_SEC = 30      // 러닝: 내 페이스 ±30초

    // 슬라이더 범위
    const val SLIDER_MAX_WEIGHT = 200  // 기록 입력 상한(400kg)보다 낮춰 슬라이더에서 구간이 너무 짧아지지 않게 한다
    const val SLIDER_MAX_PULL_UP = 30
    const val PACE_MIN_SEC = 240
    const val PACE_MAX_SEC = 480
    const val PACE_STEP_SEC = 10
}

// 조건 설정에서 부위를 보여 주는 순서 (디자인 4d: 가슴·등·팔·하체·어깨)
val filterPartOrder: List<BodyPart> =
    listOf(BodyPart.CHEST, BodyPart.BACK, BodyPart.ARM, BodyPart.LEGS, BodyPart.SHOULDER)

// 이 부위의 슬라이더 최대값 (등은 횟수, 나머지는 kg)
fun sliderMax(part: BodyPart): Int =
    if (part == BodyPart.BACK) CriteriaDefaults.SLIDER_MAX_PULL_UP else CriteriaDefaults.SLIDER_MAX_WEIGHT

// 내 기록에서 이 부위의 값 (팔은 숙련도라 값이 아니므로 null)
fun myValue(part: BodyPart, record: ExerciseRecord?): Int? = when (part) {
    BodyPart.CHEST -> record?.benchPressKg
    BodyPart.BACK -> record?.pullUpCount
    BodyPart.LEGS -> record?.squatKg
    BodyPart.SHOULDER -> record?.overheadPressKg
    BodyPart.ARM -> null
}

// 내 기록 기준의 초기 범위: 중량 ±10kg, 턱걸이 ±2회. 슬라이더 범위 밖으로 나가지 않는다.
private fun initialRange(part: BodyPart, value: Int): IntRange {
    val margin = if (part == BodyPart.BACK) CriteriaDefaults.PULL_UP_MARGIN else CriteriaDefaults.WEIGHT_MARGIN_KG
    return (value - margin).coerceAtLeast(0)..(value + margin).coerceAtMost(sliderMax(part))
}

// ---- 헬스 ----

// 부위를 켜고 끈다. 끈 부위의 수준은 지우고, 부위가 하나도 없으면 기준 설정도 끈다.
fun HealthFilter.withPartToggled(part: BodyPart): HealthFilter {
    val newParts = if (part in parts) parts - part else parts + part
    val next = copy(
        parts = newParts,
        ranges = ranges.filterKeys { it in newParts },
        armLevel = if (BodyPart.ARM in newParts) armLevel else null,
    )
    return if (newParts.isEmpty()) next.copy(criteriaOn = false) else next
}

// 내 기록으로 수준을 채운다. 기록이 없는 부위는 비워 두고 사용자가 직접 정한다. ('내 기준으로 설정')
fun HealthFilter.withMyCriteria(record: ExerciseRecord?): HealthFilter = copy(
    ranges = parts.mapNotNull { part -> myValue(part, record)?.let { part to initialRange(part, it) } }.toMap(),
    armLevel = if (BodyPart.ARM in parts) record?.armLevel else null,
)

// 기준 설정 스위치. 부위를 먼저 골라야 켤 수 있고, 처음 켜면 내 기록으로 초기값을 채운다.
fun HealthFilter.withCriteriaOn(on: Boolean, record: ExerciseRecord?): HealthFilter = when {
    !on -> copy(criteriaOn = false)
    parts.isEmpty() -> this
    ranges.isEmpty() && armLevel == null -> copy(criteriaOn = true).withMyCriteria(record)
    else -> copy(criteriaOn = true)
}

fun HealthFilter.withRange(part: BodyPart, range: IntRange): HealthFilter = copy(ranges = ranges + (part to range))

fun HealthFilter.withArmLevel(level: SkillLevel): HealthFilter = copy(armLevel = level)

// ---- 러닝 ----

// 처음 켜면 내 페이스 ±30초가 초기값이다. 기록이 없으면 비워 두고 직접 정한다.
fun RunningFilter.withCriteriaOn(on: Boolean, record: ExerciseRecord?): RunningFilter =
    if (!on) copy(criteriaOn = false)
    else copy(
        criteriaOn = true,
        paceRangeSec = paceRangeSec ?: record?.runningPaceSec?.let {
            (it - CriteriaDefaults.PACE_MARGIN_SEC).coerceAtLeast(CriteriaDefaults.PACE_MIN_SEC)..
                (it + CriteriaDefaults.PACE_MARGIN_SEC).coerceAtMost(CriteriaDefaults.PACE_MAX_SEC)
        },
    )

fun RunningFilter.withPaceRange(range: IntRange): RunningFilter = copy(paceRangeSec = range)

// ---- 등산 ----

// 처음 켜면 내 성향이 초기값이다.
fun HikingFilter.withCriteriaOn(on: Boolean, record: ExerciseRecord?): HikingFilter =
    if (!on) copy(criteriaOn = false) else copy(criteriaOn = true, style = style ?: record?.hikingStyle)

fun HikingFilter.withStyle(style: HikingStyle): HikingFilter = copy(style = style)
