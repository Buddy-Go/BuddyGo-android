package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.Meeting

// 목데이터로 응답한다. 서버 규칙은 흉내 내지 않고, 신청·취소 정도만 메모리에서 반영한다.
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
