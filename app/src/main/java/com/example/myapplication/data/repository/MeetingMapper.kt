package com.example.myapplication.data.repository

import com.example.myapplication.data.api.CardDto
import com.example.myapplication.data.api.CheckInStatusDto
import com.example.myapplication.data.api.GatheringDetailDto
import com.example.myapplication.data.api.GymCriteriaDto
import com.example.myapplication.data.api.IsoTime
import com.example.myapplication.data.api.MyActivityDto
import com.example.myapplication.data.api.PlaceDto
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.CriteriaJudgement
import com.example.myapplication.data.model.CriteriaMatchResult
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingMember
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.ui.explore.filter.CriteriaDefaults

// 서버 모임 응답 → 앱 Meeting. 서버는 헬스 모임만 있어서 종목은 항상 헬스다.

internal fun String.toMeetingStatus(): MeetingStatus = when (this) {
    "RECRUITING" -> MeetingStatus.RECRUITING
    "CLOSED" -> MeetingStatus.CLOSED
    "IN_PROGRESS" -> MeetingStatus.IN_PROGRESS
    "FINISHED" -> MeetingStatus.ENDED
    "CANCELED" -> MeetingStatus.CANCELED
    else -> MeetingStatus.RECRUITING
}

internal fun String.toGenderLimit(): GenderLimit = when (this) {
    "FEMALE_ONLY" -> GenderLimit.FEMALE_ONLY
    "MALE_ONLY" -> GenderLimit.MALE_ONLY
    else -> GenderLimit.ANYONE
}

internal fun GenderLimit.toServer(): String = when (this) {
    GenderLimit.ANYONE -> "ANY"
    GenderLimit.FEMALE_ONLY -> "FEMALE_ONLY"
    GenderLimit.MALE_ONLY -> "MALE_ONLY"
}

internal fun String.toBodyPartOrNull(): BodyPart? = BodyPart.values().firstOrNull { it.name == this }

internal fun PlaceDto.toMeetingPlace(): MeetingPlace {
    val lot = jibunAddress.orEmpty()
    return MeetingPlace(
        name = name,
        address = roadAddress?.takeIf { it.isNotBlank() } ?: lot,
        lotAddress = lot,
        detailAddress = detailAddress?.takeIf { it.isNotBlank() },
        latitude = latitude,
        longitude = longitude,
    )
}

// 서버는 호스트 기록을 값이 아니라 범위(기록 ± 허용치)로 준다. 앱은 값을 기준으로 범위를 다시 그리므로,
// 상한에서 허용치를 빼 원래 값을 되돌린다. 허용치는 앱이 쓰는 값(중량 ±10kg, 턱걸이 ±2회)이 서버 설정과 같다.
internal fun GymCriteriaDto.hostRecordOrNull(): ExerciseRecord? {
    fun center(part: String, margin: Int) =
        parts.firstOrNull { it.bodyPart == part && it.recordShown }?.maxValue?.let { it.toInt() - margin }
    val arm = parts.firstOrNull { it.bodyPart == "ARM" && it.recordShown }?.armLevel?.toSkillLevelOrNull()
    val record = ExerciseRecord(
        benchPressKg = center("CHEST", CriteriaDefaults.WEIGHT_MARGIN_KG),
        pullUpCount = center("BACK", CriteriaDefaults.PULL_UP_MARGIN),
        squatKg = center("LEGS", CriteriaDefaults.WEIGHT_MARGIN_KG),
        overheadPressKg = center("SHOULDER", CriteriaDefaults.WEIGHT_MARGIN_KG),
        armLevel = arm,
    )
    return if (record == ExerciseRecord()) null else record // 호스트가 기록을 숨겼으면 null
}

internal fun GymCriteriaDto?.toCriteria(): MeetingCriteria.Health = MeetingCriteria.Health(
    parts = this?.parts?.mapNotNull { it.bodyPart.toBodyPartOrNull() }?.toSet().orEmpty(),
    hostRecord = this?.hostRecordOrNull(),
)

internal fun GymCriteriaDto?.toJudgement(): CriteriaJudgement? {
    val match = this?.match ?: return null
    val result = when (match.result) {
        "MATCH" -> CriteriaMatchResult.MATCH
        "PARTIAL" -> CriteriaMatchResult.PARTIAL
        "MISMATCH" -> CriteriaMatchResult.MISMATCH
        else -> return null // 비교할 부위가 없다
    }
    return CriteriaJudgement(result, match.mismatchedParts.mapNotNull { it.toBodyPartOrNull() })
}

// 상세 응답 → Meeting. 내가 누구인지(meId)는 viewer.role로 confirmedIds·pendingIds에 반영한다.
// pendingIds: 호스트는 신청자 목록을 따로 받아 넘기고, 신청자는 본인 id만 담는다.
internal fun GatheringDetailDto.toMeeting(
    meId: String?,
    pendingApplicantIds: List<String> = emptyList(),
    checkIns: CheckInStatusDto? = null,
): Meeting {
    val hostId = host.userId.toString()
    val memberIds = members?.map { it.userId.toString() }.orEmpty()
    val role = viewer.role
    val confirmed = buildList {
        addAll(memberIds)
        if (role == "PARTICIPANT" && meId != null && meId !in this) add(meId)
    }
    val pending = when (role) {
        "APPLICANT" -> listOfNotNull(meId)
        "HOST" -> pendingApplicantIds
        else -> emptyList()
    }
    val checkInByUser = checkIns?.members.orEmpty().associateBy { it.userId.toString() }
    val meCheckIn = checkIns?.me
    return Meeting(
        id = gatheringId.toString(),
        title = title,
        criteria = gym.toCriteria(),
        hostId = hostId,
        hostNickname = host.nickname,
        hostMannerTemperature = host.mannerTemperature.toFloat(),
        place = place.toMeetingPlace(),
        startAtMillis = IsoTime.parse(startAt),
        durationMinutes = durationMinutes.toInt(),
        capacity = capacity,
        confirmedIds = confirmed,
        confirmedCount = confirmedCount.toInt(),
        pendingIds = pending,
        genderLimit = genderRule.toGenderLimit(),
        description = description.orEmpty(),
        status = status.toMeetingStatus(),
        isFavorite = viewer.liked,
        hostReviewCount = host.reviewCount.toInt(),
        criteriaJudgement = gym.toJudgement(),
        members = members.orEmpty().map { m ->
            val userId = m.userId.toString()
            val checkedAt = (checkInByUser[userId]?.checkedInAt ?: if (userId == meId) meCheckIn?.checkedInAt else null)
            MeetingMember(
                userId = userId,
                nickname = m.nickname,
                checkedIn = checkedAt != null,
                checkedInAtMillis = checkedAt?.let(IsoTime::parse),
            )
        },
    )
}

// 목록 카드 → Meeting. 카드에는 기준·설명·상태가 없어서 상세를 받지 못했을 때만 쓴다. (모집중인 모임만 검색에 나온다)
internal fun CardDto.toMeeting(): Meeting {
    val start = IsoTime.parse(startAt)
    return Meeting(
        id = gatheringId.toString(),
        title = title,
        criteria = MeetingCriteria.Health(parts = emptySet()),
        hostId = host.userId.toString(),
        hostNickname = host.nickname,
        hostMannerTemperature = host.mannerTemperature.toFloat(),
        place = place.toMeetingPlace(),
        startAtMillis = start,
        durationMinutes = ((IsoTime.parse(endAt) - start) / 60_000L).toInt(),
        capacity = capacity,
        confirmedCount = confirmedCount.toInt(),
        status = MeetingStatus.RECRUITING,
        distanceMeters = distanceMeters?.toInt(),
        isFavorite = liked,
    )
}

// 내 활동 응답 → Meeting. 내가 호스트가 아니면 참여 상태(PENDING, CONFIRMED)를 pendingIds·confirmedIds에 반영한다.
internal fun MyActivityDto.toMeeting(meId: String?): Meeting {
    val start = IsoTime.parse(startAt)
    val host = hostId.toString()
    return Meeting(
        id = gatheringId.toString(),
        title = title,
        criteria = MeetingCriteria.Health(parts = emptySet()),
        hostId = host,
        hostNickname = hostNickname,
        place = MeetingPlace(name = placeName, address = address.orEmpty(), lotAddress = address.orEmpty(), detailAddress = detailAddress?.takeIf { it.isNotBlank() }),
        startAtMillis = start,
        durationMinutes = ((IsoTime.parse(endAt) - start) / 60_000L).toInt(),
        capacity = capacity,
        confirmedIds = if (participationStatus == "CONFIRMED" && meId != null) listOf(meId) else emptyList(),
        confirmedCount = confirmedCount.toInt(),
        pendingIds = if (participationStatus == "PENDING" && meId != null) listOf(meId) else emptyList(),
        status = gatheringStatus.toMeetingStatus(),
    )
}

