package com.example.myapplication.ui.meeting.create

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 10f 시작 시각: 오늘을 골랐다면 이미 지난 시각은 고를 수 없다
class StartTimeRulesTest {
    private val tz = TimeZone.getTimeZone("Asia/Seoul")
    private val today = DateOption(2026, 11, 16, "오늘")
    private val tomorrow = DateOption(2026, 11, 17, "화")

    private fun at(hour: Int, minute: Int): Long = Calendar.getInstance(tz).apply {
        clear(); set(2026, Calendar.NOVEMBER, 16, hour, minute, 0)
    }.timeInMillis

    @Test
    fun 오늘이면_지금_이후로_10분_단위_올림한_시각이_최소다() {
        assertEquals(23 * 60 + 50, StartTimeRules.minStart(today, at(23, 50), tz))
        assertEquals(11 * 60 + 10, StartTimeRules.minStart(today, at(11, 1), tz))
        assertEquals(11 * 60, StartTimeRules.minStart(today, at(11, 0), tz))
        assertEquals(11 * 60 + 10, StartTimeRules.minStart(today, at(11, 10), tz))
    }

    @Test
    fun 자정_직전이면_마지막_칸_23시50분으로_막는다() {
        assertEquals(23 * 60 + 50, StartTimeRules.minStart(today, at(23, 55), tz))
    }

    @Test
    fun 오늘이_아니면_제한이_없다() {
        assertNull(StartTimeRules.minStart(tomorrow, at(23, 50), tz))
        assertNull(StartTimeRules.minStart(null, at(10, 0), tz))
    }

    @Test
    fun 처음_값은_고른_값_없으면_19시이고_제한보다_이르면_제한으로_올린다() {
        assertEquals(19 * 60, StartTimeRules.initialStart(null, null))
        assertEquals(9 * 60, StartTimeRules.initialStart(9 * 60, null))
        assertEquals(23 * 60 + 50, StartTimeRules.initialStart(null, 23 * 60 + 50))
        assertEquals(20 * 60, StartTimeRules.initialStart(null, 20 * 60))
        assertEquals(19 * 60, StartTimeRules.initialStart(null, 15 * 60))
    }

    @Test
    fun 제한_시각의_시에서만_분이_제한된다() {
        assertEquals(3, StartTimeRules.minMinuteIndex(11 * 60 + 30, 11))
        assertEquals(0, StartTimeRules.minMinuteIndex(11 * 60 + 30, 12))
        assertEquals(0, StartTimeRules.minMinuteIndex(null, 11))
    }
}
