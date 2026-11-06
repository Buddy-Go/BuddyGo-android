package com.example.myapplication.ui.meeting.manage

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.repository.FakeMeetingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 가짜 저장소가 흉내 내는 서버 규칙: 자동 마감(5장), 개설·취소·삭제(4장)
class FakeMeetingRepositoryTest {

    private val hosted = MockMeetings.hosted // 정원 3, 확정 1(나), 대기 2

    @Test
    fun acceptBelowCapacityKeepsRecruiting() = runBlocking {
        val repo = FakeMeetingRepository()
        val first = repo.getJoinRequests(hosted.id).getOrThrow().first()
        repo.accept(hosted.id, first.id).getOrThrow()
        val m = repo.getMeeting(hosted.id).getOrThrow()
        assertEquals(MeetingStatus.RECRUITING, m.status)
        assertEquals(2, m.confirmedCount)
        assertEquals(1, m.pendingIds.size)
    }

    @Test
    fun acceptToFullClosesAndRejectsRest() = runBlocking {
        val repo = FakeMeetingRepository(listOf(hosted.copy(capacity = 2)))
        val first = repo.getJoinRequests(hosted.id).getOrThrow().first()
        repo.accept(hosted.id, first.id).getOrThrow()
        val m = repo.getMeeting(hosted.id).getOrThrow()
        assertEquals(MeetingStatus.CLOSED, m.status)
        assertTrue(m.pendingIds.isEmpty())
        // 마감 뒤에는 더 수락할 수 없다
        assertTrue(repo.accept(hosted.id, "r-${hosted.id}-u3").isFailure)
    }

    @Test
    fun createMakesMeHostAndConfirmed() = runBlocking {
        val now = 1_000_000_000_000L
        val repo = FakeMeetingRepository(emptyList(), nowMillis = { now })
        // 시작은 지금부터 1시간 30분 이후여야 개설된다. (service-rules 4장)
        val id = repo.createMeeting(Meeting(title = "새 모임", capacity = 3, startAtMillis = now + 2 * 3_600_000L)).getOrThrow()
        val m = repo.getMeeting(id).getOrThrow()
        assertEquals("u1", m.hostId)
        assertEquals(listOf("u1"), m.confirmedIds)
        assertEquals(MeetingStatus.RECRUITING, m.status)
    }

    @Test
    fun createFailsWhenStartIsWithinDeadline() = runBlocking {
        val now = 1_000_000_000_000L
        val repo = FakeMeetingRepository(emptyList(), nowMillis = { now })
        assertTrue(repo.createMeeting(Meeting(title = "새 모임", capacity = 3, startAtMillis = now + 60 * 60_000L)).isFailure)
        assertTrue(repo.getMeetings().getOrThrow().isEmpty())
    }

    @Test
    fun deleteOnlyWithoutOtherConfirmed() = runBlocking {
        val repo = FakeMeetingRepository()
        assertTrue(repo.deleteMeeting(hosted.id).isSuccess)
        assertTrue(repo.getMeeting(hosted.id).isFailure)

        val full = FakeMeetingRepository(listOf(hosted.copy(confirmedIds = listOf("u1", "u2"), confirmedCount = 2)))
        assertTrue(full.deleteMeeting(hosted.id).isFailure)
    }

    @Test
    fun cancelSetsCanceled() = runBlocking {
        val repo = FakeMeetingRepository()
        repo.cancelMeeting(hosted.id, "일정이 생겼어요").getOrThrow()
        val m = repo.getMeeting(hosted.id).getOrThrow()
        assertEquals(MeetingStatus.CANCELED, m.status)
        assertTrue(m.pendingIds.isEmpty())
    }
}
