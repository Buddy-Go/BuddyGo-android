package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingStatus

// 목데이터로 응답한다. 서버 규칙은 신청·취소, 수락 시 자동 마감, 개설·취소·삭제 정도만 메모리에서 흉내 낸다.
class FakeMeetingRepository(
    initial: List<Meeting> = MockMeetings.all,
    private val currentUserId: String = MockUsers.me.id,
) : MeetingRepository {
    private val meetings = initial.toMutableList()

    override suspend fun getMeetings() = Result.success(meetings.toList())

    override suspend fun getMeeting(meetingId: String): Result<Meeting> =
        meetings.firstOrNull { it.id == meetingId }
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException(meetingId))

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

    // 수락으로 정원이 차면 바로 마감되고 남은 대기 신청은 모두 거절된다 (service-rules 5장)
    override suspend fun accept(meetingId: String, requestId: String): Result<Unit> {
        val meeting = meetings.firstOrNull { it.id == meetingId }
            ?: return Result.failure(NoSuchElementException(meetingId))
        if (meeting.status != MeetingStatus.RECRUITING || meeting.isFull) {
            return Result.failure(IllegalStateException("모집중이 아니에요"))
        }
        return update(meetingId) {
            val applicantId = requestId.substringAfterLast('-')
            val accepted = it.copy(
                pendingIds = it.pendingIds - applicantId,
                confirmedIds = it.confirmedIds + applicantId,
                confirmedCount = it.confirmedCount + 1,
            )
            if (accepted.isFull) accepted.copy(status = MeetingStatus.CLOSED, pendingIds = emptyList()) else accepted
        }
    }

    override suspend fun reject(meetingId: String, requestId: String): Result<Unit> =
        update(meetingId) { it.copy(pendingIds = it.pendingIds - requestId.substringAfterLast('-')) }

    override suspend fun createMeeting(meeting: Meeting): Result<String> {
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
