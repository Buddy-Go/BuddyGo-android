package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.CHECK_IN_RADIUS_METERS
import com.example.myapplication.data.model.CheckInOutOfRangeException
import com.example.myapplication.data.model.CheckInResult
import com.example.myapplication.data.model.GeoPoint
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingMember
import com.example.myapplication.data.model.distanceMeters

// 목데이터로 응답한다. 서버 규칙은 흉내 내지 않고, 신청·취소 정도만 메모리에서 반영한다.
class FakeMeetingRepository(
    initial: List<Meeting> = MockMeetings.all,
    private val currentUserId: String = MockUsers.me.id,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : MeetingRepository {
    private val meetings = initial.toMutableList()

    override suspend fun getMeetings() = Result.success(meetings.toList())

    override suspend fun getMeeting(meetingId: String): Result<Meeting> =
        meetings.firstOrNull { it.id == meetingId }
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException(meetingId))

    override suspend fun setFavorite(meetingId: String, favorite: Boolean): Result<Unit> =
        update(meetingId) { it.copy(isFavorite = favorite) }

    override suspend fun apply(meetingId: String, message: String?): Result<Unit> =
        update(meetingId) { it.copy(pendingIds = it.pendingIds + currentUserId) }

    override suspend fun cancelApplication(meetingId: String): Result<Unit> =
        update(meetingId) { it.copy(pendingIds = it.pendingIds - currentUserId) }

    override suspend fun leave(meetingId: String): Result<Unit> =
        update(meetingId) {
            it.copy(
                confirmedIds = it.confirmedIds - currentUserId,
                confirmedCount = (it.confirmedCount - 1).coerceAtLeast(0),
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
                )
            }
        }
        return Result.success(requests)
    }

    override suspend fun accept(meetingId: String, requestId: String): Result<Unit> =
        update(meetingId) {
            val applicantId = requestId.substringAfterLast('-')
            it.copy(
                pendingIds = it.pendingIds - applicantId,
                confirmedIds = it.confirmedIds + applicantId,
                confirmedCount = it.confirmedCount + 1,
            )
        }

    override suspend fun reject(meetingId: String, requestId: String): Result<Unit> =
        update(meetingId) { it.copy(pendingIds = it.pendingIds - requestId.substringAfterLast('-')) }

    private fun update(meetingId: String, change: (Meeting) -> Meeting): Result<Unit> {
        val index = meetings.indexOfFirst { it.id == meetingId }
        if (index < 0) return Result.failure(NoSuchElementException(meetingId))
        meetings[index] = change(meetings[index])
        return Result.success(Unit)
    }
}
