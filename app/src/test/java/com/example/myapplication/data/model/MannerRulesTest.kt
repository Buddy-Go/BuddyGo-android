package com.example.myapplication.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 점수 값은 정의서가 정하지 않아 임시다. 여기서는 정의서가 정한 관계(부호, 크기 순서, 태그 개수만 센다)를 확인한다.
class MannerRulesTest {
    @Test
    fun 좋음은_올리고_아쉬움은_내린다() {
        assertTrue(MannerRules.reviewDelta(ReviewVerdict.GOOD, 0) > 0)
        assertTrue(MannerRules.reviewDelta(ReviewVerdict.BAD, 0) < 0)
    }

    @Test
    fun 보통은_좋음보다_낮거나_같다() {
        assertTrue(MannerRules.reviewDelta(ReviewVerdict.NORMAL, 2) <= MannerRules.reviewDelta(ReviewVerdict.GOOD, 2))
        assertTrue(MannerRules.reviewScores.getValue(ReviewVerdict.NORMAL).base >= 0)
    }

    @Test
    fun 태그_한_개마다_그_판정의_태그_점수를_더한다() {
        val good = MannerRules.reviewScores.getValue(ReviewVerdict.GOOD)
        assertEquals(good.base + 3 * good.perTag, MannerRules.reviewDelta(ReviewVerdict.GOOD, 3), 0.0001f)
        val bad = MannerRules.reviewScores.getValue(ReviewVerdict.BAD)
        assertEquals(bad.base + 2 * bad.perTag, MannerRules.reviewDelta(ReviewVerdict.BAD, 2), 0.0001f)
    }

    @Test
    fun 태그는_문구와_관계없이_개수만_센다() {
        assertEquals(MannerRules.reviewDelta(ReviewVerdict.GOOD, 2), MannerRules.reviewDelta(ReviewVerdict.GOOD, 2), 0f)
        assertEquals(MannerRules.reviewDelta(ReviewVerdict.GOOD, 0), MannerRules.reviewDelta(ReviewVerdict.GOOD, -1), 0f)
    }

    @Test
    fun 지각은_노쇼보다_작게_내린다() {
        assertTrue(MannerRules.Penalty.LATE.delta > MannerRules.Penalty.NO_SHOW.delta)
    }

    @Test
    fun 데드라인_후와_마감_후가_더_크게_내린다() {
        assertTrue(MannerRules.Penalty.LEAVE_AFTER_DEADLINE.delta < MannerRules.Penalty.LEAVE_BEFORE_DEADLINE.delta)
        assertTrue(MannerRules.Penalty.HOST_CANCEL_AFTER_CLOSED.delta < MannerRules.Penalty.HOST_CANCEL_BEFORE_CLOSED.delta)
    }

    @Test
    fun 모든_벌점은_음수이고_매너온도는_0_밑으로_내려가지_않는다() {
        assertTrue(MannerRules.Penalty.entries.all { it.delta < 0 })
        assertEquals(0f, MannerRules.applied(0.3f, MannerRules.Penalty.NO_SHOW.delta), 0f)
        assertEquals(MannerRules.START_TEMPERATURE - 0.5f, MannerRules.applied(MannerRules.START_TEMPERATURE, -0.5f), 0.0001f)
    }
}
