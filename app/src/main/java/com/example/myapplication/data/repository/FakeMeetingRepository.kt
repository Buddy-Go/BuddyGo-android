package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.CHECK_IN_RADIUS_METERS
import com.example.myapplication.data.model.CheckInOutOfRangeException
import com.example.myapplication.data.model.CheckInResult
import com.example.myapplication.data.model.DEADLINE_BEFORE_START_MINUTES
import com.example.myapplication.data.model.GeoPoint
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.MapBounds
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingMember
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.distanceMeters

// 목데이터로 응답한다. 서버 규칙은 신청·취소, 수락 시 자동 마감, 개설·취소·삭제 정도만 메모리에서 흉내 낸다.
class FakeMeetingRepository(
    initial: List<Meeting> = MockMeetings.all,
    private val currentUserId: String = MockUsers.me.id,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    initialMessages: Map<String, String> = MockMeetings.applicationMessages,
) : MeetingRepository {
    private val meetings = initial.toMutableList()

    // 신청할 때 쓴 한마디와 신청 시각. 호스트의 신청 목록(getJoinRequests)에 그대로 보인다. 키는 "모임 id/신청자 id".
    private val applicationMessages = initialMessages.toMutableMap<String, String?>()
    private val applicationTimes = mutableMapOf<String, Long>()
    private fun key(meetingId: String, userId: String) = "$meetingId/$userId"

    // 영역이 있으면 장소 좌표가 그 안에 있는 모임만 준다. 좌표가 없는 모임(0, 0)은 지도에 못 올라 영역 검색에서 뺀다.
    override suspend fun getMeetings(area: MapBounds?): Result<List<Meeting>> = Result.success(
        meetings.filter { m -> area == null || (m.place.hasCoordinates() && area.contains(m.place.latitude, m.place.longitude)) },
    )

    private fun MeetingPlace.hasCoordinates() = latitude != 0.0 || longitude != 0.0

    override suspend fun getMeeting(meetingId: String): Result<Meeting> =
        meetings.firstOrNull { it.id == meetingId }
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException(meetingId))

    override suspend fun setFavorite(meetingId: String, favorite: Boolean): Result<Unit> =
        update(meetingId) { it.copy(isFavorite = favorite) }

    // 모집중인 모임에 아직 신청·확정하지 않았을 때만 된다. 한마디는 비어 있으면 없는 것으로 본다.
    override suspend fun apply(meetingId: String, message: String?): Result<Unit> {
        val meeting = meetings.firstOrNull { it.id == meetingId }
            ?: return Result.failure(NoSuchElementException(meetingId))
        if (meeting.status != MeetingStatus.RECRUITING) return Result.failure(IllegalStateException("모집중이 아니에요"))
        if (currentUserId in meeting.pendingIds || currentUserId in meeting.confirmedIds) {
            return Result.failure(IllegalStateException("이미 신청했거나 참여 중이에요"))
        }
        applicationMessages[key(meetingId, currentUserId)] = message?.trim()?.takeIf { it.isNotEmpty() }
        applicationTimes[key(meetingId, currentUserId)] = nowMillis()
        return update(meetingId) { it.copy(pendingIds = it.pendingIds + currentUserId) }
    }

    override suspend fun cancelApplication(meetingId: String): Result<Unit> {
        applicationMessages.remove(key(meetingId, currentUserId))
        applicationTimes.remove(key(meetingId, currentUserId))
        return update(meetingId) { it.copy(pendingIds = it.pendingIds - currentUserId) }
    }

    override suspend fun leave(meetingId: String): Result<Unit> =
        update(meetingId) {
            it.copy(
                confirmedIds = it.confirmedIds - currentUserId,
                members = it.members.filterNot { member -> member.userId == currentUserId },
                confirmedCount = (it.confirmedCount - 1).coerceAtLeast(0),
                // 마감 상태에서 나가면(service-rules 5장) 데드라인 전에는 마감이 풀려 모집중으로, 후에는 모임이 취소된다.
                status = when {
                    it.status != MeetingStatus.CLOSED -> it.status
                    nowMillis() >= it.deadlineAtMillis -> MeetingStatus.CANCELED
                    else -> MeetingStatus.RECRUITING
                },
            )
        }

    // 서버처럼 거리만 재서 범위 안이면 체크인을 기록한다. (시간대·모임 상태 검사는 하지 않는다)
    override suspend fun checkIn(meetingId: String, latitude: Double, longitude: Double): Result<CheckInResult> {
        val meeting = meetings.firstOrNull { it.id == meetingId } ?: return Result.failure(NoSuchElementException(meetingId))
        if (meeting.members.any { it.userId == currentUserId && it.checkedIn }) {
            return Result.failure(IllegalStateException("이미 체크인했어요")) // 체크인은 한 번만 (7장)
        }
        val meters = distanceMeters(GeoPoint(latitude, longitude), meeting.place.latitude, meeting.place.longitude)
        if (meters > CHECK_IN_RADIUS_METERS) return Result.failure(CheckInOutOfRangeException(meters))
        val at = nowMillis()
        update(meetingId) { m ->
            val others = m.members.filterNot { it.userId == currentUserId }
            val nickname = m.members.firstOrNull { it.userId == currentUserId }?.nickname ?: MockUsers.me.nickname
            m.copy(members = others + MeetingMember(currentUserId, nickname, checkedIn = true, checkedInAtMillis = at))
        }
        return Result.success(CheckInResult(at))
    }

    override suspend fun getJoinRequests(meetingId: String): Result<List<JoinRequest>> {
        val meeting = meetings.firstOrNull { it.id == meetingId }
            ?: return Result.failure(NoSuchElementException(meetingId))
        val requests = meeting.pendingIds.mapNotNull { id ->
            MockUsers.all.firstOrNull { it.id == id }?.let { user ->
                JoinRequest(
                    id = "r-$meetingId-$id", meetingId = meetingId, applicantId = user.id,
                    applicantNickname = user.nickname, applicantMannerTemperature = user.mannerTemperature,
                    applicantRecord = user.record,
                    message = applicationMessages[key(meetingId, id)],
                    requestedAtMillis = applicationTimes[key(meetingId, id)] ?: 0L,
                )
            }
        }
        return Result.success(requests)
    }

    // 수락으로 정원이 차면 바로 마감되고 남은 대기 신청은 모두 거절된다 (service-rules 5장)
    override suspend fun accept(meetingId: String, requestId: String): Result<Unit> {
        val meeting = meetings.firstOrNull { it.id == meetingId }
            ?: return Result.failure(NoSuchElementException(meetingId))
        if (meeting.status != MeetingStatus.RECRUITING || meeting.isFull) {
            return Result.failure(IllegalStateException("모집중이 아니에요"))
        }
        return update(meetingId) {
            val applicantId = requestId.substringAfterLast('-')
            val nickname = MockUsers.all.firstOrNull { user -> user.id == applicantId }?.nickname.orEmpty()
            val accepted = it.copy(
                pendingIds = it.pendingIds - applicantId,
                confirmedIds = it.confirmedIds + applicantId,
                confirmedCount = it.confirmedCount + 1,
                // 명단이 공개된 모임이면 명단에도 넣는다. 비어 있으면 서버가 명단을 숨기는 모임이라 그대로 둔다.
                members = if (it.members.isEmpty()) it.members else it.members + MeetingMember(applicantId, nickname),
            )
            if (accepted.isFull) accepted.copy(status = MeetingStatus.CLOSED, pendingIds = emptyList()) else accepted
        }
    }

    override suspend fun reject(meetingId: String, requestId: String): Result<Unit> {
        val applicantId = requestId.substringAfterLast('-')
        applicationMessages.remove(key(meetingId, applicantId))
        applicationTimes.remove(key(meetingId, applicantId))
        return update(meetingId) { it.copy(pendingIds = it.pendingIds - applicantId) }
    }

    // 서버처럼 시작이 지금부터 데드라인(1시간 30분) 이후여야 개설할 수 있다. (service-rules 4장)
    override suspend fun createMeeting(meeting: Meeting): Result<String> {
        if (meeting.startAtMillis - nowMillis() < DEADLINE_BEFORE_START_MINUTES * 60_000L) {
            return Result.failure(IllegalArgumentException("시작은 지금부터 1시간 30분 이후만 고를 수 있어요"))
        }
        val host = MockUsers.all.firstOrNull { it.id == currentUserId }
        val id = "new-${meetings.size + 1}"
        meetings += meeting.copy(
            id = id,
            hostId = currentUserId,
            hostNickname = host?.nickname.orEmpty(),
            hostMannerTemperature = host?.mannerTemperature ?: 0f,
            confirmedIds = listOf(currentUserId),
            confirmedCount = 1,
            pendingIds = emptyList(),
            status = MeetingStatus.RECRUITING,
        )
        return Result.success(id)
    }

    // 서버처럼 규칙을 판정한다 (4장): 진행중이 되기 전까지만, 확정 참가자(호스트 제외)가 있으면 제목·설명·상세주소만 바뀐다.
    // 잠기지 않았을 때 일시를 바꾸면 시작이 지금부터 데드라인(90분) 이후여야 하고, 정원은 확정 인원보다 작을 수 없다.
    override suspend fun updateMeeting(meetingId: String, meeting: Meeting): Result<Unit> {
        val current = meetings.firstOrNull { it.id == meetingId }
            ?: return Result.failure(NoSuchElementException(meetingId))
        if (current.status != MeetingStatus.RECRUITING && current.status != MeetingStatus.CLOSED) {
            return Result.failure(IllegalStateException("진행중이 되면 수정할 수 없어요"))
        }
        val locked = current.confirmedCount > 1
        val place = current.place.copy(detailAddress = meeting.place.detailAddress)
        if (locked) {
            return update(meetingId) { it.copy(title = meeting.title, description = meeting.description, place = place) }
        }
        if (meeting.startAtMillis != current.startAtMillis &&
            meeting.startAtMillis - nowMillis() < DEADLINE_BEFORE_START_MINUTES * 60_000L
        ) {
            return Result.failure(IllegalArgumentException("시작은 지금부터 1시간 30분 이후만 고를 수 있어요"))
        }
        if (meeting.capacity < current.confirmedCount) {
            return Result.failure(IllegalArgumentException("정원은 확정 인원보다 작을 수 없어요"))
        }
        return update(meetingId) {
            it.copy(
                title = meeting.title,
                description = meeting.description,
                criteria = meeting.criteria,
                place = meeting.place,
                startAtMillis = meeting.startAtMillis,
                durationMinutes = meeting.durationMinutes,
                capacity = meeting.capacity,
                genderLimit = meeting.genderLimit,
            )
        }
    }

    // 취소하면 대기 신청도 모두 거절된다. 사유는 서버가 확정자에게 알림으로 보낸다 (4장)
    override suspend fun cancelMeeting(meetingId: String, reason: String): Result<Unit> =
        update(meetingId) { it.copy(status = MeetingStatus.CANCELED, pendingIds = emptyList()) }

    // 삭제는 호스트 말고 확정된 사람이 없을 때만 된다 (5a-X3)
    override suspend fun deleteMeeting(meetingId: String): Result<Unit> {
        val meeting = meetings.firstOrNull { it.id == meetingId }
            ?: return Result.failure(NoSuchElementException(meetingId))
        if (meeting.confirmedCount > 1) return Result.failure(IllegalStateException("확정된 메이트가 있어요"))
        meetings.remove(meeting)
        return Result.success(Unit)
    }

    private fun update(meetingId: String, change: (Meeting) -> Meeting): Result<Unit> {
        val index = meetings.indexOfFirst { it.id == meetingId }
        if (index < 0) return Result.failure(NoSuchElementException(meetingId))
        meetings[index] = change(meetings[index])
        return Result.success(Unit)
    }
}
