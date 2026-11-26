package com.example.myapplication.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MannerRulesTest {
    @Test
    fun 게이지_위치는_0에서_1_사이로_자른다() {
        assertEquals(0.365f, mannerFraction(36.5f), 0.0001f)
        assertEquals(0f, mannerFraction(-5f), 0f)
        assertEquals(1f, mannerFraction(120f), 0f)
    }

    @Test
    fun 단계는_온도_구간대로_나뉜다() {
        assertEquals(MannerTier.FREEZING, mannerTier(19.9f))
        assertEquals(MannerTier.COOL, mannerTier(20f))
        assertEquals(MannerTier.COOL, mannerTier(36.4f))
        assertEquals(MannerTier.WARM, mannerTier(36.5f))
        assertEquals(MannerTier.WARM, mannerTier(44.9f))
        assertEquals(MannerTier.TOASTY, mannerTier(45f))
        assertEquals(MannerTier.HOT, mannerTier(60f))
    }

    @Test
    fun 숫자는_소수_첫째_자리로_보여_준다() {
        assertEquals("36.5", mannerNumber(36.5f))
        assertEquals("44.1", mannerNumber(44.1f))
        assertEquals("0.0", mannerNumber(0f))
        assertEquals("100.0", mannerNumber(100f))
    }

    @Test
    fun 시작점과_비교한_설명() {
        assertEquals("시작점과 같아요", mannerDeltaText(36.5f))
        assertEquals("시작점보다 7.6° 높아요", mannerDeltaText(44.1f))
        assertEquals("시작점보다 16.5° 낮아요", mannerDeltaText(20f))
    }
}
