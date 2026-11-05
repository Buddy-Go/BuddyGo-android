package com.example.myapplication.data.model

enum class Sport { HEALTH, RUNNING, HIKING }

enum class Gender { MALE, FEMALE }

// 헬스 팔 숙련도
enum class SkillLevel { BEGINNER, INTERMEDIATE, ADVANCED }

// 등산 성향
enum class HikingStyle { HOBBY, PRO }

// 운동 수준 기록. 전부 선택 입력이라 null은 '미설정'이다. (service-rules 1장)
data class ExerciseRecord(
    val benchPressKg: Int? = null,     // 헬스 가슴
    val pullUpCount: Int? = null,      // 헬스 등 (맨몸 턱걸이 횟수, 0회 가능)
    val squatKg: Int? = null,          // 헬스 하체
    val overheadPressKg: Int? = null,  // 헬스 어깨
    val armLevel: SkillLevel? = null,  // 헬스 팔
    val runningPaceSec: Int? = null,   // 러닝 평균 페이스 (초/km)
    val hikingStyle: HikingStyle? = null, // 등산 성향
)

// 비밀번호는 모델에 두지 않는다. 인증은 서버가 처리한다.
data class User(
    val id: String = "",
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val gender: Gender = Gender.MALE,
    val mannerTemperature: Float = 0f, // 시작값은 서버가 정한다 (정책 숫자는 코드에 쓰지 않음)
    val preferredSports: List<Sport> = emptyList(),
    val record: ExerciseRecord = ExerciseRecord(),
)
