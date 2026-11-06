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

// 신청 → 호스트의 신청 목록 → 수락·거절이 이어지는지 (6a → 7)
class FakeMeetingRepositoryApplyFlowTest {
    private val id = MockMeetings.recruiting.id
    private val me = "u1"

    private fun requests(repo: FakeMeetingRepository) = runBlocking { repo.getJoinRequests(id).getOrThrow() }
    private fun meeting(repo: FakeMeetingRepository) = runBlocking { repo.getMeeting(id).getOrThrow() }

    @Test
    fun 신청한_한마디가_호스트의_신청_목록에_그대로_보인다() {
        val repo = FakeMeetingRepository()
        assertTrue(runBlocking { repo.apply(id, "  벤치 같이 하고 싶어요  ") }.isSuccess)
        val mine = requests(repo).first { it.applicantId == me }
        assertEquals("벤치 같이 하고 싶어요", mine.message)
        assertTrue(mine.requestedAtMillis > 0)
    }

    @Test
    fun 한마디를_안_쓰거나_공백이면_없는_것으로_본다() {
        val repo = FakeMeetingRepository()
        runBlocking { repo.apply(id, "   ") }
        assertEquals(null, requests(repo).first { it.applicantId == me }.message)
    }

    @Test
    fun 신청을_취소하면_목록과_한마디가_사라진다() {
        val repo = FakeMeetingRepository()
        runBlocking { repo.apply(id, "안녕하세요"); repo.cancelApplication(id) }
        assertTrue(requests(repo).none { it.applicantId == me })
        runBlocking { repo.apply(id, null) }
        assertEquals(null, requests(repo).first { it.applicantId == me }.message)
    }

    @Test
    fun 같은_모임에_두_번_신청하면_실패하고_목록에_하나만_있다() {
        val repo = FakeMeetingRepository()
        assertTrue(runBlocking { repo.apply(id, "첫 신청") }.isSuccess)
        assertTrue(runBlocking { repo.apply(id, "다시") }.isFailure)
        assertEquals(1, requests(repo).count { it.applicantId == me })
        assertEquals("첫 신청", requests(repo).first { it.applicantId == me }.message)
    }

    @Test
    fun 모집중이_아닌_모임에는_신청할_수_없다() {
        val repo = FakeMeetingRepository()
        assertTrue(runBlocking { repo.apply(MockMeetings.closed.id, "신청") }.isFailure)
        assertTrue(runBlocking { repo.apply(MockMeetings.canceled.id, "신청") }.isFailure)
    }

    @Test
    fun 수락하면_확정에_들어가고_신청_목록에서_빠진다() {
        val repo = FakeMeetingRepository()
        runBlocking { repo.apply(id, "신청") }
        assertTrue(runBlocking { repo.accept(id, "r-$id-$me") }.isSuccess)
        assertTrue(me in meeting(repo).confirmedIds)
        assertTrue(requests(repo).none { it.applicantId == me })
    }

    @Test
    fun 명단이_공개된_모임은_수락하면_명단에도_들어간다() {
        val repo = FakeMeetingRepository(listOf(MockMeetings.recruiting.copy(members = listOf(
            com.example.myapplication.data.model.MeetingMember("u3", "지훈"),
        ))))
        runBlocking { repo.apply(id, null); repo.accept(id, "r-$id-$me") }
        assertEquals(listOf("지훈", "민준"), meeting(repo).members.map { it.nickname })
    }

    @Test
    fun 명단이_숨겨진_모임은_수락해도_명단을_만들지_않는다() {
        val repo = FakeMeetingRepository()
        runBlocking { repo.apply(id, null); repo.accept(id, "r-$id-$me") }
        assertTrue(meeting(repo).members.isEmpty())
    }

    @Test
    fun 거절하면_신청이_사라지고_한마디도_지워진다() {
        val repo = FakeMeetingRepository()
        runBlocking { repo.apply(id, "신청"); repo.reject(id, "r-$id-$me") }
        assertTrue(requests(repo).none { it.applicantId == me })
        assertFalse(me in meeting(repo).confirmedIds)
    }
}

class FakeMeetingRepositorySeedTest {
    @Test
    fun 목데이터의_대기_신청에는_한마디가_들어_있다() {
        val repo = FakeMeetingRepository()
        val requests = runBlocking { repo.getJoinRequests(MockMeetings.hosted.id).getOrThrow() }
        assertEquals("벤치 처음인데 같이 해도 될까요?", requests.first { it.applicantId == "u2" }.message)
        assertTrue(requests.all { !it.message.isNullOrBlank() })
    }
}
