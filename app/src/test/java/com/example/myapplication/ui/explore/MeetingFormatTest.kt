package com.example.myapplication.ui.explore

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeetingFormatTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(seoul).apply { clear(); set(year, month - 1, day, hour, minute) }.timeInMillis

    private val now = millis(2026, 11, 6, 15)

    @Test
    fun 천_미터_미만은_미터로_표시한다() {
        assertEquals("450m", formatDistance(450))
        assertEquals("0m", formatDistance(0))
        assertEquals("999m", formatDistance(999))
    }

    @Test
    fun 천_미터_이상은_킬로미터로_표시하고_정수면_소수점을_뺀다() {
        assertEquals("1km", formatDistance(1000))
        assertEquals("2km", formatDistance(2000))
        assertEquals("1.2km", formatDistance(1200))
        assertEquals("1.3km", formatDistance(1250))
    }

    @Test
    fun 거리를_모르면_표시하지_않는다() {
        assertNull(formatDistance(null))
    }

    @Test
    fun 오늘은_오늘이라고_쓴다() {
        assertEquals("오늘 오전 7:00", formatStartTime(millis(2026, 11, 6, 7), now, seoul))
        assertEquals("오늘 오후 7:30", formatStartTime(millis(2026, 11, 6, 19, 30), now, seoul))
    }

    @Test
    fun 다음_날은_내일이라고_쓴다() {
        assertEquals("내일 오전 6:30", formatStartTime(millis(2026, 11, 7, 6, 30), now, seoul))
    }

    @Test
    fun 그_밖의_날은_월과_일을_쓴다() {
        assertEquals("11월 8일 오전 9:00", formatStartTime(millis(2026, 11, 8, 9), now, seoul))
        assertEquals("11월 5일 오전 9:00", formatStartTime(millis(2026, 11, 5, 9), now, seoul))
    }

    @Test
    fun 해가_바뀌어도_다음_날이면_내일이다() {
        val newYearsEve = millis(2026, 12, 31, 22)
        assertEquals("내일 오전 1:00", formatStartTime(millis(2027, 1, 1, 1), newYearsEve, seoul))
    }

    @Test
    fun 자정과_정오는_열두_시로_쓴다() {
        assertEquals("오늘 오전 12:05", formatStartTime(millis(2026, 11, 6, 0, 5), now, seoul))
        assertEquals("오늘 오후 12:00", formatStartTime(millis(2026, 11, 6, 12), now, seoul))
    }
}
