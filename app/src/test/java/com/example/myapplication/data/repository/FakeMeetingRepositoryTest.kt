package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockMeetings
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
}
