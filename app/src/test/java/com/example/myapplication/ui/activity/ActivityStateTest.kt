package com.example.myapplication.ui.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

// 활동 내역(13-H, 13-P, 13-R) 표시 글자와 예정/지난 구분
class ActivityStateTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour
    private val now = 1788566400000L // 2026-09-05 09:00 (서울)

    @Test
    fun 날짜_상자와_시각() {
        assertEquals(DateBox("9월", "5"), dateBox(now, seoul))
        assertEquals("오전 9:00", clockText(now, seoul))
        assertEquals("오후 8:30", clockText(now + 11 * hour + 30 * minute, seoul))
        assertEquals("오후 12:00", clockText(now + 3 * hour, seoul))
    }

    @Test
    fun 남은_시간() {
        assertEquals("5시간", remainingText(now, now + 5 * hour + 10 * minute))
        assertEquals("3일", remainingText(now, now + 3 * day + 2 * hour))
        assertEquals("20분", remainingText(now, now + 20 * minute))
        assertNull(remainingText(now, now - minute))
    }

    @Test
    fun 호스트_카드_아래줄_13H() {
        val recruiting = HostedMeetingItem("a", "t", now + 10 * hour, 2, 3, HostedStatus.RECRUITING, deadlineAt = now + 5 * hour)
        assertEquals("오후 7:00 · 마감까지 5시간", recruiting.metaText(now, seoul))
        val closed = recruiting.copy(startAt = now + 3 * day + 11 * hour, status = HostedStatus.CLOSED)
        assertEquals("오후 8:00 · 시작까지 3일", closed.metaText(now, seoul))
        val ended = recruiting.copy(startAt = now - day, status = HostedStatus.ENDED)
        assertEquals("오전 9:00 종료", ended.metaText(now, seoul))
    }

    @Test
    fun 참여_카드_아래줄_13P() {
        val item = JoinedMeetingItem("a", "t", now - 2 * hour - 30 * minute, "망원한강공원", 0.8, JoinedStatus.CONFIRMED)
        assertEquals("망원한강공원 · 오전 6:30 · 0.8km", item.metaText(seoul))
        assertEquals("망원한강공원 · 오전 6:30 · 3km", item.copy(distanceKm = 3.0).metaText(seoul))
        assertEquals("망원한강공원 · 오전 6:30", item.copy(distanceKm = null).metaText(seoul))
    }

    @Test
    fun 평가_대기_기한_13R() {
        val item = ReviewPendingItem("m", "t", endedAt = now, reviewDueAt = now + 5 * day)
        assertEquals(ReviewDueText("9/5 종료 · 5일 남음", isUrgent = false), item.dueText(now, seoul))
        assertEquals(ReviewDueText("9/5 종료 · 오늘까지", isUrgent = true), item.dueText(item.reviewDueAt - hour, seoul))
    }

    @Test
    fun 예정과_지난_모임_구분() {
        val s = sampleActivityState(now)
        assertTrue(s.hostedUpcoming.all { it.isUpcoming })
        assertTrue(s.hostedPast.none { it.isUpcoming })
        assertEquals(3, s.hostedUpcoming.size)
        assertEquals(listOf("j1"), s.joinedUpcoming.map { it.meetingId })
        assertFalse(JoinedMeetingItem("x", "t", 0, "p", null, JoinedStatus.NO_SHOW).isUpcoming)
        assertEquals("참여 27 · 개설 9", s.statsText)
        assertEquals("평가 대기 1", s.reviewHeader)
    }
}
