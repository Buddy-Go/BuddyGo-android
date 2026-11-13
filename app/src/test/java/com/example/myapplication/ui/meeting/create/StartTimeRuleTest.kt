package com.example.myapplication.ui.meeting.create

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 10f 시작 시각 규칙: 지금부터 90분 이후만 고를 수 있다
class StartTimeRuleTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int = 0): Long =
        Calendar.getInstance(utc).apply { clear(); set(y, mo - 1, d, h, mi, s) }.timeInMillis
    private fun day(y: Int, mo: Int, d: Int) = DateOption(y, mo, d, "")

    @Test
    fun todayRoundsUpToTenMinutes() {
        // 14:03 + 90분 = 15:33 → 15:40
        assertEquals(15 * 60 + 40, earliestStartMinutesOf(day(2026, 11, 14), at(2026, 11, 14, 14, 3), utc))
        // 딱 떨어지면 그대로: 14:00 + 90분 = 15:30
        assertEquals(15 * 60 + 30, earliestStartMinutesOf(day(2026, 11, 14), at(2026, 11, 14, 14, 0), utc))
        // 초가 남아 있으면 다음 칸
        assertEquals(15 * 60 + 40, earliestStartMinutesOf(day(2026, 11, 14), at(2026, 11, 14, 14, 0, 5), utc))
    }

    @Test
    fun laterDaysHaveNoLimit() {
        assertNull(earliestStartMinutesOf(day(2026, 11, 15), at(2026, 11, 14, 14, 0), utc))
    }

    @Test
    fun lateNightCarriesIntoTomorrow() {
        // 23:20 + 90분 = 내일 00:50 → 오늘은 고를 시간이 없고, 내일은 00:50부터
        val now = at(2026, 11, 14, 23, 20)
        assertEquals(24 * 60, earliestStartMinutesOf(day(2026, 11, 14), now, utc))
        assertEquals(50, earliestStartMinutesOf(day(2026, 11, 15), now, utc))
    }

    @Test
    fun sheetStartsAtEarliestInsteadOf19() {
        assertEquals(15 * 60 + 40, initialTimeSheetStart(null, 15 * 60 + 40))
        assertEquals(19 * 60, initialTimeSheetStart(null, null))
        assertEquals(19 * 60, initialTimeSheetStart(null, 24 * 60))
        // 이미 고른 값이 유효하면 그대로, 지났으면 가장 이른 시각
        assertEquals(18 * 60, initialTimeSheetStart(18 * 60, 15 * 60 + 40))
        assertEquals(15 * 60 + 40, initialTimeSheetStart(9 * 60, 15 * 60 + 40))
    }

    @Test
    fun pastPickIsClamped() {
        assertEquals(15 to 4, clampToEarliest(9, 0, 15 * 60 + 40))
        assertEquals(15 to 4, clampToEarliest(15, 2, 15 * 60 + 40))
        assertEquals(16 to 0, clampToEarliest(16, 0, 15 * 60 + 40))
        assertEquals(9 to 0, clampToEarliest(9, 0, null))
    }
}
