package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockNotifications
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNotificationRepositoryTest {
    private fun list(repo: FakeNotificationRepository) = runBlocking { repo.getNotifications().getOrThrow() }

    @Test
    fun 최신_알림이_앞에_온다() {
        val times = list(FakeNotificationRepository()).map { it.createdAtMillis }
        assertEquals(times.sortedDescending(), times)
    }

    @Test
    fun 읽음으로_바꾸면_다시_불러와도_읽은_상태다() {
        val repo = FakeNotificationRepository()
        assertTrue(runBlocking { repo.markAsRead(MockNotifications.unreadAccepted.id) }.isSuccess)
        assertTrue(list(repo).first { it.id == MockNotifications.unreadAccepted.id }.isRead)
        assertFalse("다른 알림은 그대로", list(repo).first { it.id == MockNotifications.unreadNotice.id }.isRead)
    }

    @Test
    fun 이미_읽은_알림을_다시_읽음으로_보내도_성공한다() {
        val repo = FakeNotificationRepository()
        assertTrue(runBlocking { repo.markAsRead(MockNotifications.readMate.id) }.isSuccess)
    }

    @Test
    fun 없는_알림이면_실패한다() {
        assertTrue(runBlocking { FakeNotificationRepository().markAsRead("없는 id") }.isFailure)
    }

    @Test
    fun 빈_목록도_성공으로_돌려준다() {
        assertTrue(list(FakeNotificationRepository(MockNotifications.empty)).isEmpty())
    }
}
