package com.example.myapplication.ui.record

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport

// 종목 기록 화면(2c~2e)의 값. 값의 정의와 값에서 계산되는 것만 담고, 바꾸는 코드는 ViewModel에 둔다.
// 한 화면이 종목(sport)에 따라 헬스(2c)·러닝(2d)·등산(2e)을 보여 준다.
data class RecordState(
    val sport: Sport = Sport.HEALTH,
    // ---- 헬스 (2c) ----
    val parts: Set<BodyPart> = emptySet(),
    val benchPressKg: Int = DEFAULT_BENCH_KG,
    val pullUpCount: Int = DEFAULT_PULL_UP,
    val squatKg: Int = DEFAULT_SQUAT_KG,
    val overheadPressKg: Int = DEFAULT_OVERHEAD_KG,
    val armLevel: SkillLevel? = null,
    // ---- 러닝 (2d) ----
    val runningPaceSec: Int? = null, // 초/km. 고르지 않았으면 null
    // ---- 등산 (2e) ----
    val hikingStyle: HikingStyle? = null,
    val saving: Boolean = false, // 프로필에 저장하는 중
    val saveFailed: Boolean = false, // 저장하지 못했다. 한 번 안내하고 지운다
    val saved: Boolean = false, // 저장이 끝났는지. 화면이 보고 이전 화면으로 돌아간다.
) {
    // 가슴 · 등 · 하체 · 어깨 · 팔 순서로 고정
    val selectedParts: List<BodyPart> get() = BodyPart.entries.filter { it in parts }

    // 1RM 기준 부위(가슴·하체·어깨). 그중 첫 번째에만 '1RM 기준' 제목과 안내 문구가 붙는다.
    val firstOneRmPart: BodyPart? get() = selectedParts.firstOrNull { it in ONE_RM_PARTS }

    // 입력값을 담은 기록. 이 종목의 값만 바꾸고 다른 종목 기록은 그대로 둔다.
    // 선택하지 않은 헬스 부위는 비운다.
    fun toRecord(base: ExerciseRecord): ExerciseRecord = when (sport) {
        Sport.HEALTH -> base.copy(
            benchPressKg = if (BodyPart.CHEST in parts) benchPressKg else null,
            pullUpCount = if (BodyPart.BACK in parts) pullUpCount else null,
            squatKg = if (BodyPart.LEGS in parts) squatKg else null,
            overheadPressKg = if (BodyPart.SHOULDER in parts) overheadPressKg else null,
            armLevel = if (BodyPart.ARM in parts) armLevel else null,
        )
        Sport.RUNNING -> base.copy(runningPaceSec = runningPaceSec)
        Sport.HIKING -> base.copy(hikingStyle = hikingStyle)
    }

    companion object {
        const val WEIGHT_STEP = 5      // 무게 −/+ 한 번에 바뀌는 kg
        const val MAX_WEIGHT = 400
        const val MAX_PULL_UP = 100
        const val DEFAULT_BENCH_KG = 40
        const val DEFAULT_PULL_UP = 0
        const val DEFAULT_SQUAT_KG = 60
        const val DEFAULT_OVERHEAD_KG = 30
        val ONE_RM_PARTS = setOf(BodyPart.CHEST, BodyPart.LEGS, BodyPart.SHOULDER)

        // 러닝 페이스 선택지 (초/km). 처음 값은 '~4'30"', 마지막 값은 '7'00"+'로 보인다.
        val PACE_OPTIONS = listOf(270, 300, 330, 360, 390, 420)

        fun paceLabel(sec: Int): String {
            val text = "${sec / 60}'${(sec % 60).toString().padStart(2, '0')}\""
            return when (sec) {
                PACE_OPTIONS.first() -> "~$text"
                PACE_OPTIONS.last() -> "$text+"
                else -> text
            }
        }

        // 저장해 둔 기록에서 화면 값을 복원한다. 기록이 있는 부위는 선택된 상태가 된다.
        fun from(record: ExerciseRecord, sport: Sport = Sport.HEALTH) = RecordState(
            sport = sport,
            parts = buildSet {
                if (record.benchPressKg != null) add(BodyPart.CHEST)
                if (record.pullUpCount != null) add(BodyPart.BACK)
                if (record.squatKg != null) add(BodyPart.LEGS)
                if (record.overheadPressKg != null) add(BodyPart.SHOULDER)
                if (record.armLevel != null) add(BodyPart.ARM)
            },
            benchPressKg = record.benchPressKg ?: DEFAULT_BENCH_KG,
            pullUpCount = record.pullUpCount ?: DEFAULT_PULL_UP,
            squatKg = record.squatKg ?: DEFAULT_SQUAT_KG,
            overheadPressKg = record.overheadPressKg ?: DEFAULT_OVERHEAD_KG,
            armLevel = record.armLevel,
            runningPaceSec = record.runningPaceSec,
            hikingStyle = record.hikingStyle,
        )
    }
}
