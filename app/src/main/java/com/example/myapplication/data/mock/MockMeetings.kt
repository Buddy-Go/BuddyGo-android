package com.example.myapplication.data.mock

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.CriteriaJudgement
import com.example.myapplication.data.model.CriteriaMatchResult
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingMember
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.MeetingStatus

// 상태별 사례(모집중, 마감, 진행중, 종료, 취소, 빈 목록)를 모두 담는다.
// 시각은 기준 시각(BASE)에서 더하고 뺀 고정값이다. 실제 현재 시각과는 무관하다.
object MockMeetings {
    private const val HOUR = 3_600_000L
    private const val BASE = 1_798_761_600_000L // 임의의 기준 시각

    private val gym = MeetingPlace(
        name = "에이블짐 홍대점", address = "서울 마포구 양화로 100", lotAddress = "서교동 123-4",
        detailAddress = "3층", latitude = 37.5563, longitude = 126.9236,
    )
    private val park = MeetingPlace(
        name = "한강공원 망원지구", address = "서울 마포구 마포나루길 467", lotAddress = "망원동 436-1",
        latitude = 37.5551, longitude = 126.8978,
    )
    private val mountain = MeetingPlace(
        name = "북한산 입구", address = "서울 강북구 우이동", lotAddress = "우이동 1-1",
        latitude = 37.6586, longitude = 127.0114,
    )

    // 모집중 · 내 역할 없음 (호스트 u3)
    val recruiting = Meeting(
        id = "m1", title = "가슴·팔 같이 하실 분", hostId = "u3", hostNickname = "지훈", hostMannerTemperature = 41.0f, hostReviewCount = 18,
        criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST, BodyPart.ARM), MockUsers.hostA.record),
        criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.PARTIAL, listOf(BodyPart.CHEST)),
        place = gym, startAtMillis = BASE + 24 * HOUR, durationMinutes = 90, capacity = 3,
        confirmedIds = emptyList(), confirmedCount = 1, description = "벤치 위주로 해요.",
        status = MeetingStatus.RECRUITING, distanceMeters = 450,
    )

    // 모집중 · 내가 신청해서 대기 중
    val pending = Meeting(
        id = "m2", title = "저녁 한강 러닝", hostId = "u4", hostNickname = "유나", hostMannerTemperature = 37.0f, hostReviewCount = 7,
        criteria = MeetingCriteria.Running(distanceKm = 5f, hostPaceSec = 345),
        criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.MATCH),
        place = park, startAtMillis = BASE + 30 * HOUR, durationMinutes = 60, capacity = 4,
        confirmedCount = 2, pendingIds = listOf("u1"),
        status = MeetingStatus.RECRUITING, distanceMeters = 1200, isFavorite = true,
    )

    // 마감 · 내가 확정
    val closed = Meeting(
        id = "m3", title = "하체 데이", hostId = "u3", hostNickname = "지훈", hostMannerTemperature = 41.0f,
        criteria = MeetingCriteria.Health(setOf(BodyPart.LEGS), MockUsers.hostA.record),
        place = gym, startAtMillis = BASE + 48 * HOUR, durationMinutes = 120, capacity = 2,
        confirmedIds = listOf("u3", "u1"), confirmedCount = 2,
        hostReviewCount = 18, criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.MISMATCH),
        members = listOf(MeetingMember("u3", "지훈"), MeetingMember("u1", "민준")),
        genderLimit = GenderLimit.MALE_ONLY, status = MeetingStatus.CLOSED, distanceMeters = 450,
    )

    // 진행중 · 내가 확정
    val inProgress = Meeting(
        id = "m4", title = "북한산 가볍게", hostId = "u4", hostNickname = "유나", hostMannerTemperature = 37.0f,
        criteria = MeetingCriteria.Hiking(HikingStyle.HOBBY),
        place = mountain, startAtMillis = BASE - HOUR, durationMinutes = 240, capacity = 3,
        confirmedIds = listOf("u4", "u1"), confirmedCount = 2,
        hostReviewCount = 7, criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.MATCH),
        members = listOf(MeetingMember("u4", "유나", checkedIn = true), MeetingMember("u1", "민준")),
        status = MeetingStatus.IN_PROGRESS, distanceMeters = 9000,
    )

    // 종료
    val ended = Meeting(
        id = "m5", title = "아침 러닝", hostId = "u4", hostNickname = "유나", hostMannerTemperature = 37.0f,
        criteria = MeetingCriteria.Running(distanceKm = 3f, hostPaceSec = 345),
        place = park, startAtMillis = BASE - 72 * HOUR, durationMinutes = 60, capacity = 3,
        confirmedIds = listOf("u4", "u1", "u2"), confirmedCount = 3,
        hostReviewCount = 7, criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.MATCH),
        members = listOf(MeetingMember("u4", "유나", true), MeetingMember("u1", "민준", true), MeetingMember("u2", "서연", false)),
        status = MeetingStatus.ENDED,
    )

    // 취소
    val canceled = Meeting(
        id = "m6", title = "어깨 운동", hostId = "u3", hostNickname = "지훈", hostMannerTemperature = 41.0f,
        criteria = MeetingCriteria.Health(setOf(BodyPart.SHOULDER), null), // 기록 숨김
        place = gym, startAtMillis = BASE + 10 * HOUR, durationMinutes = 60, capacity = 3,
        confirmedCount = 1, status = MeetingStatus.CANCELED,
    )

    // 내가 호스트 · 모집중 · 신청 대기 2명
    val hosted = Meeting(
        id = "m7", title = "벤치 같이 해요", hostId = "u1", hostNickname = "민준", hostMannerTemperature = 36.5f,
        criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST), MockUsers.me.record),
        place = gym, startAtMillis = BASE + 26 * HOUR, durationMinutes = 90, capacity = 3,
        confirmedIds = listOf("u1"), confirmedCount = 1, pendingIds = listOf("u2", "u3"),
        status = MeetingStatus.RECRUITING, distanceMeters = 0,
    )

    val all = listOf(recruiting, pending, closed, inProgress, ended, canceled, hosted)

    // 대기 신청의 한마디. 키는 "모임 id/신청자 id". 호스트의 신청 목록(7)에 보인다.
    val applicationMessages = mapOf(
        "m7/u2" to "벤치 처음인데 같이 해도 될까요?",
        "m7/u3" to "자세 봐 드릴 수 있어요. 3대 400 정도 합니다.",
    )

    // 빈 목록
    val empty: List<Meeting> = emptyList()
}
