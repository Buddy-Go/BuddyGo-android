package com.example.myapplication.data.model

// 모집중 → 마감 → 진행중 → 종료, 취소는 어느 단계에서든 가능 (service-rules 6장)
// 데드라인은 모임 시작 이 시간(분) 전이다. (service-rules 5장, 기준값은 바뀔 수 있다)
const val DEADLINE_BEFORE_START_MINUTES = 90

enum class MeetingStatus { RECRUITING, CLOSED, IN_PROGRESS, ENDED, CANCELED }

enum class GenderLimit { ANYONE, FEMALE_ONLY, MALE_ONLY }

// 모임에서 본 나의 역할. 판단은 Meeting.roleOf 한 곳에서만 한다.
enum class MeetingRole { HOST, CONFIRMED, PENDING, NONE }

enum class BodyPart { CHEST, BACK, LEGS, SHOULDER, ARM }

// 서버가 계산해서 모임 상세에 담아 주는 기준 일치 판정 (service-rules 8장)
enum class CriteriaMatchResult { MATCH, PARTIAL, MISMATCH }

// result가 MATCH면 모두 맞음, PARTIAL이면 outOfRangeParts에 범위 밖인 부위가 담긴다. (헬스만 해당)
data class CriteriaJudgement(
    val result: CriteriaMatchResult,
    val outOfRangeParts: List<BodyPart> = emptyList(),
)

// 모임에 확정된 참가자 한 명. 체크인했는지는 진행중부터 의미가 있다. (7장)
data class MeetingMember(
    val userId: String,
    val nickname: String,
    val checkedIn: Boolean = false,
    val checkedInAtMillis: Long? = null, // 체크인한 시각 (9c '체크인 완료 · 오전 7:02')
)

data class MeetingPlace(
    val name: String = "",
    val address: String = "",
    val lotAddress: String = "",       // 지번. 같은 지번이면 지도 핀 하나로 묶는다 (2장)
    val detailAddress: String? = null, // 호스트가 적는 상세주소 (선택)
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val kakaoPlaceId: String? = null, // 카카오 장소 id. 있으면 개설·수정 요청에 실어 서버가 같은 장소를 합치게 한다
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
    val hostReviewCount: Int = 0,    // 호스트가 받은 후기 수 (모임 상세의 '후기 32')
    // 판정할 수 있는 부위가 없거나 화면에 표시하지 않기로 했으면 null (8장)
    val criteriaJudgement: CriteriaJudgement? = null,
    // 확정 참가자 명단. 참가자에게 공개되지 않는 시점에는 서버가 비워서 내려준다. (5장 '참가자 명단 공개')
    val members: List<MeetingMember> = emptyList(),
) {
    val sport: Sport
        get() = when (criteria) {
            is MeetingCriteria.Health -> Sport.HEALTH
            is MeetingCriteria.Running -> Sport.RUNNING
            is MeetingCriteria.Hiking -> Sport.HIKING
        }

    // 데드라인 = 시작 1시간 30분 전. 이때까지 정원이 안 차면 자동 취소되고, 마감 상태에서 이후에 누가 나가면 모임이 취소된다. (service-rules 5장)
    val deadlineAtMillis: Long get() = startAtMillis - DEADLINE_BEFORE_START_MINUTES * 60_000L

    // 종료 시각 = 시작 + 진행 시간
    val endAtMillis: Long get() = startAtMillis + durationMinutes * 60_000L

    // 체크인한 참가자 수 (진행중 화면의 '체크인 5')
    val checkedInCount: Int get() = members.count { it.checkedIn }

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
