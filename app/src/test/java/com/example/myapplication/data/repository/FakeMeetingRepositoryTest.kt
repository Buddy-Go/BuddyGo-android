package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.CHECK_IN_RADIUS_METERS
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
}
