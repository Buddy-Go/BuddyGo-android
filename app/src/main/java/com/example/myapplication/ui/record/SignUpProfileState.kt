package com.example.myapplication.ui.record

import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Sport

// 종목별 기록을 지운 복사본. 종목 선택을 해제하면 그 종목의 기록도 함께 지운다.
fun ExerciseRecord.clearedFor(sport: Sport): ExerciseRecord = when (sport) {
    Sport.HEALTH -> copy(benchPressKg = null, pullUpCount = null, squatKg = null, overheadPressKg = null, armLevel = null)
    Sport.RUNNING -> copy(runningPaceSec = null)
    Sport.HIKING -> copy(hikingStyle = null)
}

// 해당 종목의 기록이 하나라도 입력됐는지
fun ExerciseRecord.hasRecordFor(sport: Sport): Boolean = when (sport) {
    Sport.HEALTH -> benchPressKg != null || pullUpCount != null || squatKg != null || overheadPressKg != null || armLevel != null
    Sport.RUNNING -> runningPaceSec != null
    Sport.HIKING -> hikingStyle != null
}

// 2b 프로필 설정 화면의 값. 선택·기록은 회원가입 입력값 저장소에서 읽어 온 값이다.
data class SignUpProfileState(
    val sports: Set<Sport> = emptySet(),
    val record: ExerciseRecord = ExerciseRecord(),
    val submitting: Boolean = false,
    val error: Boolean = false,
    val completed: Boolean = false, // 가입 요청이 성공했는지. 화면이 보고 다음 화면으로 이동한다.
) {
    // 헬스 · 러닝 · 등산 순서로 고정
    val selectedSports: List<Sport> get() = Sport.entries.filter { it in sports }

    fun isRecordDone(sport: Sport): Boolean = record.hasRecordFor(sport)

    // 기록 줄의 회색 글자. 등산의 '레벨 미입력'은 디자인 문구이고, 헬스·러닝의 미입력 문구는 같은 형식으로 정한 것이다.
    fun recordStatusText(sport: Sport): String = when {
        isRecordDone(sport) -> "입력 완료"
        sport == Sport.HEALTH -> "기록 미입력"
        sport == Sport.RUNNING -> "페이스 미입력"
        else -> "레벨 미입력"
    }
}
