package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.CHECK_IN_RADIUS_METERS
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.CheckInOutOfRangeException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMeetingRepositoryTest {
    private val id = MockMeetings.recruiting.id

    private fun isFavorite(repo: FakeMeetingRepository) =
        runBlocking { repo.getMeeting(id).getOrThrow().isFavorite }

    @Test
    fun 찜을_켜면_다시_불러와도_켜져_있다() {
        val repo = FakeMeetingRepository()
        assertFalse(isFavorite(repo))
        assertTrue(runBlocking { repo.setFavorite(id, true) }.isSuccess)
        assertTrue(isFavorite(repo))
    }

    @Test
    fun 같은_값으로_두_번_보내도_결과는_같다() {
        val repo = FakeMeetingRepository()
        runBlocking { repo.setFavorite(id, true); repo.setFavorite(id, true) }
        assertTrue(isFavorite(repo))
        runBlocking { repo.setFavorite(id, false); repo.setFavorite(id, false) }
        assertFalse(isFavorite(repo))
    }

    @Test
    fun 없는_모임이면_실패한다() {
        val result = runBlocking { FakeMeetingRepository().setFavorite("없는 id", true) }
        assertTrue(result.isFailure)
        assertEquals(NoSuchElementException::class, result.exceptionOrNull()!!::class)
    }

    // ---- 체크인 ----

    private val mountain = MockMeetings.inProgress // 북한산 입구 37.6586, 127.0114

    @Test
    fun 모임_장소_근처에서_체크인하면_성공하고_시각이_남는다() {
        val repo = FakeMeetingRepository(nowMillis = { 12_345L })
        val result = runBlocking { repo.checkIn(mountain.id, 37.6586, 127.0114) }
        assertEquals(12_345L, result.getOrThrow().checkedInAtMillis)
        val me = runBlocking { repo.getMeeting(mountain.id) }.getOrThrow().members.first { it.userId == "u1" }
        assertTrue(me.checkedIn)
        assertEquals(12_345L, me.checkedInAtMillis)
    }

    @Test
    fun 체크인하면_체크인_수가_늘고_다른_참가자는_그대로다() {
        val repo = FakeMeetingRepository()
        val before = runBlocking { repo.getMeeting(mountain.id) }.getOrThrow().checkedInCount
        runBlocking { repo.checkIn(mountain.id, 37.6586, 127.0114) }
        val after = runBlocking { repo.getMeeting(mountain.id) }.getOrThrow()
        assertEquals(before + 1, after.checkedInCount)
        assertTrue(after.members.first { it.userId == "u4" }.checkedIn)
    }

    @Test
    fun 범위_밖이면_현재_거리와_함께_실패하고_기록하지_않는다() {
        val repo = FakeMeetingRepository()
        // 위도 0.003도 ≈ 330m
        val error = runBlocking { repo.checkIn(mountain.id, 37.6586 + 0.003, 127.0114) }.exceptionOrNull()
        assertTrue(error is CheckInOutOfRangeException)
        assertTrue((error as CheckInOutOfRangeException).distanceMeters in 300..360)
        val me = runBlocking { repo.getMeeting(mountain.id) }.getOrThrow().members.first { it.userId == "u1" }
        assertFalse(me.checkedIn)
    }

    @Test
    fun 범위_경계는_오십_미터이고_넘으면_실패한다() {
        assertEquals(50, CHECK_IN_RADIUS_METERS)
        val repo = FakeMeetingRepository()
        // 위도 0.00036도 ≈ 40m: 성공, 0.0006도 ≈ 67m: 실패
        assertTrue(runBlocking { repo.checkIn(mountain.id, 37.6586 + 0.00036, 127.0114) }.isSuccess)
        val repo2 = FakeMeetingRepository()
        assertTrue(runBlocking { repo2.checkIn(mountain.id, 37.6586 + 0.0006, 127.0114) }.isFailure)
    }

    @Test
    fun 체크인은_한_번만_할_수_있다() {
        val repo = FakeMeetingRepository()
        assertTrue(runBlocking { repo.checkIn(mountain.id, 37.6586, 127.0114) }.isSuccess)
        assertTrue(runBlocking { repo.checkIn(mountain.id, 37.6586, 127.0114) }.isFailure)
    }

    @Test
    fun 없는_모임이면_체크인에_실패한다() {
        assertTrue(runBlocking { FakeMeetingRepository().checkIn("없는 id", 37.0, 127.0) }.isFailure)
    }

    // ---- 나가기 ----

    @Test
    fun 나가면_확정_명단과_인원과_참가자_명단에서_빠진다() {
        val repo = FakeMeetingRepository()
        val closed = MockMeetings.closed
        assertTrue(runBlocking { repo.leave(closed.id) }.isSuccess)
        val after = runBlocking { repo.getMeeting(closed.id) }.getOrThrow()
        assertFalse("u1" in after.confirmedIds)
        assertEquals(closed.confirmedCount - 1, after.confirmedCount)
        assertFalse(after.members.any { it.userId == "u1" })
        assertTrue("다른 참가자는 그대로", after.members.any { it.userId == "u3" })
    }

    @Test
    fun 데드라인_전에_마감_모임에서_나가면_마감이_풀려_모집중이_된다() {
        val closed = MockMeetings.closed
        val repo = FakeMeetingRepository(nowMillis = { closed.deadlineAtMillis - 1 })
        runBlocking { repo.leave(closed.id) }
        assertEquals(MeetingStatus.RECRUITING, runBlocking { repo.getMeeting(closed.id) }.getOrThrow().status)
    }

    @Test
    fun 데드라인_후에_마감_모임에서_나가면_모임이_취소된다() {
        val closed = MockMeetings.closed
        val repo = FakeMeetingRepository(nowMillis = { closed.deadlineAtMillis })
        runBlocking { repo.leave(closed.id) }
        assertEquals(MeetingStatus.CANCELED, runBlocking { repo.getMeeting(closed.id) }.getOrThrow().status)
    }

    @Test
    fun 모집중_모임에서_나가도_상태는_그대로다() {
        val repo = FakeMeetingRepository(initial = listOf(MockMeetings.recruiting.copy(confirmedIds = listOf("u1"), confirmedCount = 2)))
        runBlocking { repo.leave(MockMeetings.recruiting.id) }
        assertEquals(MeetingStatus.RECRUITING, runBlocking { repo.getMeeting(MockMeetings.recruiting.id) }.getOrThrow().status)
    }

    @Test
    fun 없는_모임에서_나가려면_실패한다() {
        assertTrue(runBlocking { FakeMeetingRepository().leave("없는 id") }.isFailure)
    }
}

// 모임 수정 규칙 (service-rules 4장)
class FakeMeetingRepositoryUpdateTest {
    private val hostedId = MockMeetings.hosted.id // 호스트 u1, 확정 1명(호스트만), 모집중
    private val now = 1_798_761_600_000L - 24 * 3_600_000L // 목데이터 기준 시각의 하루 전

    private fun repo(vararg meetings: com.example.myapplication.data.model.Meeting) =
        FakeMeetingRepository(meetings.toList(), nowMillis = { now })

    private fun get(r: FakeMeetingRepository, id: String) = runBlocking { r.getMeeting(id).getOrThrow() }

    @Test
    fun 확정_참가자가_없으면_모든_항목을_바꿀_수_있다() {
        val r = repo(MockMeetings.hosted)
        val m = MockMeetings.hosted
        val edited = m.copy(
            title = "새 제목", description = "새 설명", capacity = 2,
            startAtMillis = m.startAtMillis + 3_600_000L, durationMinutes = 120,
            place = m.place.copy(detailAddress = "3층"),
        )
        assertTrue(runBlocking { r.updateMeeting(hostedId, edited) }.isSuccess)
        val saved = get(r, hostedId)
        assertEquals("새 제목", saved.title)
        assertEquals(2, saved.capacity)
        assertEquals(120, saved.durationMinutes)
        assertEquals(m.startAtMillis + 3_600_000L, saved.startAtMillis)
        assertEquals("3층", saved.place.detailAddress)
        assertEquals("확정·신청 정보는 그대로", m.pendingIds, saved.pendingIds)
    }

    @Test
    fun 확정_참가자가_있으면_제목_설명_상세주소만_바뀐다() {
        val locked = MockMeetings.hosted.copy(confirmedCount = 2, confirmedIds = listOf("u1", "u2"))
        val r = repo(locked)
        val edited = locked.copy(
            title = "새 제목", description = "새 설명", capacity = 3, durationMinutes = 30,
            startAtMillis = locked.startAtMillis + 3_600_000L, place = locked.place.copy(detailAddress = "후문"),
        )
        assertTrue(runBlocking { r.updateMeeting(hostedId, edited) }.isSuccess)
        val saved = get(r, hostedId)
        assertEquals("새 제목", saved.title)
        assertEquals("새 설명", saved.description)
        assertEquals("후문", saved.place.detailAddress)
        assertEquals("일시는 잠긴다", locked.startAtMillis, saved.startAtMillis)
        assertEquals(locked.durationMinutes, saved.durationMinutes)
        assertEquals(locked.capacity, saved.capacity)
    }

    @Test
    fun 진행중이거나_끝난_모임은_수정할_수_없다() {
        for (status in listOf(MeetingStatus.IN_PROGRESS, MeetingStatus.ENDED, MeetingStatus.CANCELED)) {
            val m = MockMeetings.hosted.copy(status = status)
            val r = repo(m)
            assertTrue(status.name, runBlocking { r.updateMeeting(hostedId, m.copy(title = "x")) }.isFailure)
            assertEquals(MockMeetings.hosted.title, get(r, hostedId).title)
        }
    }

    @Test
    fun 마감된_모임도_진행중이_되기_전이면_수정할_수_있다() {
        val m = MockMeetings.hosted.copy(status = MeetingStatus.CLOSED, confirmedCount = 3, confirmedIds = listOf("u1", "u2", "u3"))
        val r = repo(m)
        assertTrue(runBlocking { r.updateMeeting(hostedId, m.copy(title = "마감 후 수정")) }.isSuccess)
        assertEquals("마감 후 수정", get(r, hostedId).title)
    }

    @Test
    fun 시작을_지금부터_90분_안으로_바꾸면_실패한다() {
        val m = MockMeetings.hosted
        val r = repo(m)
        val tooSoon = m.copy(startAtMillis = now + 60 * 60_000L)
        assertTrue(runBlocking { r.updateMeeting(hostedId, tooSoon) }.isFailure)
        assertEquals(m.startAtMillis, get(r, hostedId).startAtMillis)
        val ok = m.copy(startAtMillis = now + 90 * 60_000L)
        assertTrue(runBlocking { r.updateMeeting(hostedId, ok) }.isSuccess)
    }

    @Test
    fun 일시를_안_바꾸면_90분_검사를_하지_않는다() {
        // 시작이 이미 90분 안으로 들어온 모임이어도 제목만 고치는 것은 된다.
        val m = MockMeetings.hosted.copy(startAtMillis = now + 30 * 60_000L)
        val r = repo(m)
        assertTrue(runBlocking { r.updateMeeting(hostedId, m.copy(title = "제목만")) }.isSuccess)
    }

    @Test
    fun 정원은_확정_인원보다_작을_수_없다() {
        val m = MockMeetings.hosted.copy(confirmedCount = 1)
        val r = repo(m)
        assertTrue(runBlocking { r.updateMeeting(hostedId, m.copy(capacity = 0)) }.isFailure)
    }

    @Test
    fun 없는_모임이면_실패한다() {
        assertTrue(runBlocking { repo(MockMeetings.hosted).updateMeeting("없는 id", MockMeetings.hosted) }.isFailure)
    }
}
