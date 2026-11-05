package com.example.myapplication.data.model

// 모집중 → 마감 → 진행중 → 종료, 취소는 어느 단계에서든 가능 (service-rules 6장)
enum class MeetingStatus { RECRUITING, CLOSED, IN_PROGRESS, ENDED, CANCELED }

enum class GenderLimit { ANYONE, FEMALE_ONLY, MALE_ONLY }

// 모임에서 본 나의 역할. 판단은 Meeting.roleOf 한 곳에서만 한다.
enum class MeetingRole { HOST, CONFIRMED, PENDING, NONE }

enum class BodyPart { CHEST, BACK, LEGS, SHOULDER, ARM }

data class MeetingPlace(
    val name: String = "",
    val address: String = "",
    val lotAddress: String = "",       // 지번. 같은 지번이면 지도 핀 하나로 묶는다 (2장)
    val detailAddress: String? = null, // 호스트가 적는 상세주소 (선택)
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

// 종목별 모임 기준. 종목은 기준의 종류로 정해진다.
sealed interface MeetingCriteria {
    // hostRecord는 '내 기록 표시'가 켜졌을 때 개설 시점 기록의 복사본이다. 숨기면 null.
    data class Health(
        val parts: Set<BodyPart>,
        val hostRecord: ExerciseRecord? = null,
    ) : MeetingCriteria

    data class Running(
        val distanceKm: Float,
        val hostPaceSec: Int, // 초/km
    ) : MeetingCriteria

    data class Hiking(val style: HikingStyle) : MeetingCriteria
}

data class Meeting(
    val id: String = "",
    val title: String = "",
    val criteria: MeetingCriteria = MeetingCriteria.Hiking(HikingStyle.HOBBY),
    val hostId: String = "",
    val hostNickname: String = "",
    val hostMannerTemperature: Float = 0f,
    val place: MeetingPlace = MeetingPlace(),
    val startAtMillis: Long = 0L,
    val durationMinutes: Int = 0,
    val capacity: Int = 0, // 호스트 포함
    // 확정 참가자 id (호스트 포함). 참가자에게 공개되지 않는 시점에는 서버가 비워서 내려준다. (5장)
    val confirmedIds: List<String> = emptyList(),
    val confirmedCount: Int = 0,
    // 대기 신청자 id. 호스트에게는 전체, 참가자에게는 본인 것만 내려온다.
    val pendingIds: List<String> = emptyList(),
    val genderLimit: GenderLimit = GenderLimit.ANYONE,
    val description: String = "",
    val status: MeetingStatus = MeetingStatus.RECRUITING,
    val distanceMeters: Int? = null, // 내 위치에서의 거리 (서버 계산)
    val isFavorite: Boolean = false, // 내가 찜했는지
) {
    val sport: Sport
        get() = when (criteria) {
            is MeetingCriteria.Health -> Sport.HEALTH
            is MeetingCriteria.Running -> Sport.RUNNING
            is MeetingCriteria.Hiking -> Sport.HIKING
        }

    // 종료 시각 = 시작 + 진행 시간
    val endAtMillis: Long get() = startAtMillis + durationMinutes * 60_000L

    // 마감 = 확정이 정원과 같음
    val isFull: Boolean get() = capacity > 0 && confirmedCount >= capacity

    // 호스트/확정/대기/미신청 판단은 여기서만 한다.
    fun roleOf(userId: String): MeetingRole = when {
        userId == hostId -> MeetingRole.HOST
        userId in confirmedIds -> MeetingRole.CONFIRMED
        userId in pendingIds -> MeetingRole.PENDING
        else -> MeetingRole.NONE
    }
}
