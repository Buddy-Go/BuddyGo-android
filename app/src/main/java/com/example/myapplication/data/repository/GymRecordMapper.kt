package com.example.myapplication.data.repository

import com.example.myapplication.data.api.GymRecordDto
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.SkillLevel

// 서버 헬스 기록(부위별)과 앱 ExerciseRecord(종목별 한 덩어리) 사이 변환.
// 서버 부위: CHEST 가슴(벤치 1RM kg), BACK 등(턱걸이 횟수), LEGS 하체(스쿼트 1RM kg), SHOULDER 어깨(오버헤드 1RM kg), ARM 팔(숙련도)
// 러닝 페이스·등산 성향은 서버에 저장할 곳이 없어 여기서 다루지 않는다.

fun List<GymRecordDto>.toExerciseRecord(): ExerciseRecord {
    fun value(part: String) = firstOrNull { it.bodyPart == part && it.recorded }?.recordValue?.toInt()
    return ExerciseRecord(
        benchPressKg = value("CHEST"),
        pullUpCount = value("BACK"),
        squatKg = value("LEGS"),
        overheadPressKg = value("SHOULDER"),
        armLevel = firstOrNull { it.bodyPart == "ARM" && it.recorded }?.armLevel?.toSkillLevelOrNull(),
    )
}

// 입력한 부위만 담는다. 서버는 보낸 부위가 내 기록 전체가 된다.
fun ExerciseRecord.toGymRecordDtos(): List<GymRecordDto> = buildList {
    benchPressKg?.let { add(GymRecordDto("CHEST", true, it.toDouble(), null)) }
    pullUpCount?.let { add(GymRecordDto("BACK", true, it.toDouble(), null)) }
    squatKg?.let { add(GymRecordDto("LEGS", true, it.toDouble(), null)) }
    overheadPressKg?.let { add(GymRecordDto("SHOULDER", true, it.toDouble(), null)) }
    armLevel?.let { add(GymRecordDto("ARM", true, null, it.name)) }
}

internal fun String.toSkillLevelOrNull(): SkillLevel? = SkillLevel.values().firstOrNull { it.name == this }
