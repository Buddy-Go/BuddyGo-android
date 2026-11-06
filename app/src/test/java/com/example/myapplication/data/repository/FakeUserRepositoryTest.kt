package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockUsers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeUserRepositoryTest {
    private val repo = FakeUserRepository()

    @Test
    fun 내_프로필_숫자와_많이_받은_태그를_준다() {
        val s = runBlocking { repo.getProfileStats(MockUsers.me.id) }.getOrThrow()
        assertEquals(32, s.receivedReviewCount)
        assertEquals(9, s.hostedMeetingCount)
        assertEquals(s.topTags.sortedByDescending { it.count }, s.topTags)
    }

    @Test
    fun 기록이_없는_사용자는_0과_빈_목록이다() {
        val s = runBlocking { repo.getProfileStats(MockUsers.noRecord.id) }.getOrThrow()
        assertEquals(0, s.receivedReviewCount)
        assertTrue(runBlocking { repo.getReceivedReviews(MockUsers.noRecord.id) }.getOrThrow().isEmpty())
    }

    @Test
    fun 받은_후기는_최신순이다() {
        val list = runBlocking { repo.getReceivedReviews(MockUsers.me.id) }.getOrThrow()
        assertEquals(list.sortedByDescending { it.createdAtMillis }, list)
    }

    @Test
    fun 알림_설정은_기본이_켜짐이고_바꾼_값이_남는다() {
        assertTrue(runBlocking { repo.isNotificationEnabled() }.getOrThrow())
        runBlocking { repo.setNotificationEnabled(false) }
        assertFalse(runBlocking { repo.isNotificationEnabled() }.getOrThrow())
    }
}
