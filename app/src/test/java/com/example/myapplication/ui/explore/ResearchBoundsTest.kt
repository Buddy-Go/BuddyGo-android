package com.example.myapplication.ui.explore

import com.example.myapplication.data.model.MapBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchBoundsTest {
    private val yongsan = MapBounds(37.52, 126.97, 37.55, 127.01)
    private val hongdae = MapBounds(37.55, 126.91, 37.57, 126.94)

    @Test
    fun 코드로_옮겨도_재검색은_지금_보이는_영역을_쓴다() {
        // 손으로 용산을 보다가(검색 버튼이 뜬 상태) 내 위치 버튼으로 홍대로 옮긴 경우
        val state = ExploreState().withCameraMoved(yongsan).withBounds(hongdae)
        assertTrue("검색 버튼은 그대로 떠 있다", state.showResearchButton)
        assertEquals(hongdae, state.afterResearch().searchBounds)
    }

    @Test
    fun 코드로_옮긴_것만으로는_검색_버튼이_뜨지_않는다() {
        val state = ExploreState().withBounds(hongdae)
        assertFalse(state.showResearchButton)
        assertEquals(hongdae, state.cameraBounds)
    }

    @Test
    fun 영역을_못_읽으면_이전_영역을_지킨다() {
        val state = ExploreState().withBounds(hongdae).withBounds(null)
        assertEquals(hongdae, state.cameraBounds)
    }
}

class RangeLabelTest {
    private val hongdae = com.example.myapplication.data.model.MapBounds(37.55, 126.91, 37.57, 126.94)

    @org.junit.Test
    fun 처음에는_전국() {
        org.junit.Assert.assertEquals("전국", ExploreState().rangeLabel)
    }

    @org.junit.Test
    fun 현_지도에서_검색하면_검색한_지도_범위() {
        val state = ExploreState().withCameraMoved(hongdae).afterResearch()
        org.junit.Assert.assertEquals("검색한 지도 범위", state.rangeLabel)
    }
}
