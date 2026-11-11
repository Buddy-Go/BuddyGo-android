package com.example.myapplication.ui.meeting.create

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 10f 진행 시간: 최소 30분, 10분 단위, 최대 4시간 (서버 설정과 같다)
class DurationRulesTest {
    @Test
    fun 눌러서_10분씩_늘고_줄어든다() {
        assertEquals(70, DurationRules.plus(60))
        assertEquals(50, DurationRules.minus(60))
        assertEquals(40, DurationRules.plus(30))
    }

    @Test
    fun 최소_30분_아래로는_내려가지_않는다() {
        assertEquals(30, DurationRules.minus(30))
        assertEquals(30, DurationRules.minus(40))
        assertFalse(DurationRules.canMinus(30))
        assertTrue(DurationRules.canMinus(40))
    }

    @Test
    fun 최대_4시간을_넘지_않는다() {
        assertEquals(240, DurationRules.plus(240))
        assertEquals(240, DurationRules.plus(230))
        assertFalse(DurationRules.canPlus(240))
        assertTrue(DurationRules.canPlus(230))
    }

    @Test
    fun 처음은_30분이고_모든_값이_10분_단위다() {
        var d = DurationRules.MIN
        val seen = mutableListOf(d)
        while (DurationRules.canPlus(d)) { d = DurationRules.plus(d); seen += d }
        assertEquals(30, seen.first())
        assertEquals(240, seen.last())
        assertTrue(seen.all { it % 10 == 0 })
        assertEquals(22, seen.size) // 30, 40, ..., 240
        assertEquals("30분", durationText(seen.first()))
        assertEquals("1시간 10분", durationText(70))
    }
}
