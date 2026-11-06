package com.example.myapplication.ui.notification

import com.example.myapplication.data.mock.MockNotifications
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val min = 60_000L
    private val hour = 3_600_000L
    // 2026-01-05 12:00 UTC
    private val now = 1_767_614_400_000L

    private fun n(type: NotificationType, at: Long = now, meetingId: String? = "m1", noticeId: String? = null, userId: String? = null) =
        Notification(id = "x", type = type, meetingId = meetingId, noticeId = noticeId, userId = userId, createdAtMillis = at)

    @Test
    fun 오늘과_이전으로_나누고_최신순으로_정렬한다() {
        val a = n(NotificationType.CHECKIN_OPENED, now - 5 * min).copy(id = "a")
        val b = n(NotificationType.NOTICE_WRITTEN, now - hour).copy(id = "b")
        val c = n(NotificationType.JOIN_ACCEPTED, now - 24 * hour).copy(id = "c")
        val s = groupNotifications(listOf(b, c, a), now, utc)
        assertEquals(listOf("a", "b"), s.today.map { it.id })
        assertEquals(listOf("c"), s.earlier.map { it.id })
    }

    @Test
    fun 알림이_없으면_빈_상태다() {
        assertTrue(groupNotifications(MockNotifications.empty, now, utc).isEmpty)
    }

    @Test
    fun 상대_시간_표기() {
        assertEquals("방금 전", formatRelativeTime(now - 20_000, now, utc))
        assertEquals("5분 전", formatRelativeTime(now - 5 * min, now, utc))
        assertEquals("1시간 전", formatRelativeTime(now - hour, now, utc))
        assertEquals("어제", formatRelativeTime(now - 24 * hour, now, utc))
        assertEquals("3일 전", formatRelativeTime(now - 72 * hour, now, utc))
        assertEquals("12월 29일", formatRelativeTime(now - 7 * 24 * hour, now, utc))
    }

    @Test
    fun 미래로_찍힌_알림은_오늘의_방금_전으로_본다() {
        val future = n(NotificationType.CHECKIN_OPENED, now + 56 * 24 * hour).copy(id = "f")
        assertEquals("방금 전", formatRelativeTime(future.createdAtMillis, now, utc))
        assertEquals(listOf("f"), groupNotifications(listOf(future), now, utc).today.map { it.id })
    }

    @Test
    fun 굵은_앞부분과_문장을_잇는다() {
        assertEquals("북한산 가볍게 모임이 곧 시작해요", joinSubject("북한산 가볍게", "모임이 곧 시작해요"))
        assertEquals("서연님이 메이트 신청을 보냈어요", joinSubject("서연", "님이 메이트 신청을 보냈어요"))
        assertEquals("신청이 수락됐어요", joinSubject(null, "신청이 수락됐어요"))
    }

    @Test
    fun 눌렀을_때_가는_곳() {
        assertEquals("meeting/m1", n(NotificationType.CHECKIN_OPENED).destination())
        assertEquals("meeting/m1/notices/nt1", n(NotificationType.NOTICE_WRITTEN, noticeId = "nt1").destination())
        assertEquals("meeting/m1", n(NotificationType.NOTICE_COMMENTED).destination())
        assertEquals("mate_request", n(NotificationType.MATE_REQUESTED, meetingId = null, userId = "u2").destination())
        assertEquals("mate/profile/u2", n(NotificationType.MATE_ACCEPTED, meetingId = null, userId = "u2").destination())
        assertEquals("review", n(NotificationType.MEETING_ENDED).destination())
        assertNull(n(NotificationType.JOIN_ACCEPTED, meetingId = null).destination())
    }

    @Test
    fun 종류별_아이콘() {
        assertEquals(NotificationIcon.PIN_CHECK, NotificationType.CHECKIN_OPENED.icon())
        assertEquals(NotificationIcon.MEGAPHONE, NotificationType.NOTICE_REPLIED.icon())
        assertEquals(NotificationIcon.PEOPLE, NotificationType.MATE_REQUESTED.icon())
        assertEquals(NotificationIcon.CLOSE, NotificationType.JOIN_REJECTED.icon())
        assertEquals(NotificationIcon.CHECK, NotificationType.JOIN_ACCEPTED.icon())
    }

    @Test
    fun 읽지_않은_알림은_읽어_줄_때_앞에_알린다() {
        val unread = MockNotifications.unreadNotice
        assertTrue(unread.spokenText("1시간 전").startsWith("읽지 않음, "))
        assertEquals("서연님이 메이트 신청을 보냈어요, 어제", MockNotifications.readMate.spokenText("어제"))
    }
}
