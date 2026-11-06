package com.example.myapplication.ui.explore.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DistrictCatalogTest {
    @Before
    fun 최근_검색_초기화() = RecentDistrictStore.clear()

    @Test
    fun 검색어가_비어_있으면_아무것도_보여_주지_않는다() {
        assertTrue(DistrictCatalog.search("").isEmpty())
        assertTrue(DistrictCatalog.search("   ").isEmpty())
    }

    @Test
    fun 구를_검색하면_그_구와_그_구의_동이_나온다() {
        val names = DistrictCatalog.search("마포").map { it.fullName }
        assertEquals("서울 마포구", names.first())
        assertTrue("서울 마포구 합정동" in names)
        assertTrue("서울 마포구 망원동" in names)
        assertFalse("서울 강북구" in names)
    }

    @Test
    fun 구가_동보다_먼저_나온다() {
        val results = DistrictCatalog.search("마포")
        assertEquals("마포구", results.first().name)
    }

    @Test
    fun 동만_검색해도_찾는다() {
        assertEquals(listOf("서울 마포구 합정동"), DistrictCatalog.search("합정").map { it.fullName })
    }

    @Test
    fun 공백이_섞여도_찾고_없는_검색어는_빈_목록이다() {
        assertEquals(DistrictCatalog.search("마포구합정"), DistrictCatalog.search("마포구 합정"))
        assertTrue(DistrictCatalog.search("없는동네").isEmpty())
    }

    @Test
    fun 서울_스물다섯_개_구가_모두_있다() {
        assertEquals(25, DistrictCatalog.all.count { it.fullName.split(" ").size == 2 })
    }

    @Test
    fun 검색어와_겹치는_글자만_굵게_나눈다() {
        assertEquals(
            listOf(TextPiece("서울 ", false), TextPiece("마포", true), TextPiece("구 합정동", false)),
            highlightPieces("서울 마포구 합정동", "마포"),
        )
        assertEquals(listOf(TextPiece("서울 마포구", false)), highlightPieces("서울 마포구", "없음"))
        assertEquals(listOf(TextPiece("서울 마포구", false)), highlightPieces("서울 마포구", ""))
        assertEquals(listOf(TextPiece("마포", true)), highlightPieces("마포", "마포"))
    }

    @Test
    fun 최근_검색은_최신이_앞이고_같은_곳은_하나만_남는다() {
        assertEquals(listOf("마포구", "강북구"), listOf("강북구").withRecent("마포구"))
        assertEquals(listOf("강북구", "마포구", "종로구"), listOf("마포구", "강북구", "종로구").withRecent("강북구"))
    }

    @Test
    fun 최근_검색은_네_개까지만_둔다() {
        val recent = listOf("가", "나", "다", "라").withRecent("마")
        assertEquals(listOf("마", "가", "나", "다"), recent)
    }

    @Test
    fun 최근_검색_저장소는_추가와_삭제를_반영한다() {
        RecentDistrictStore.add("마포구")
        RecentDistrictStore.add("강북구")
        assertEquals(listOf("강북구", "마포구"), RecentDistrictStore.recent.value)
        RecentDistrictStore.remove("강북구")
        assertEquals(listOf("마포구"), RecentDistrictStore.recent.value)
    }

    @Test
    fun 구_동을_고르고_다시_누르면_해제된다() {
        val state = LocationSearchState().withToggled("마포구")
        assertEquals("마포구", state.selected)
        assertNull(state.withToggled("마포구").selected)
        assertEquals("강북구", state.withToggled("강북구").selected)
    }

    @Test
    fun 초기화하면_고른_곳과_검색어가_비워지고_최근_검색은_남는다() {
        val state = LocationSearchState(query = "마포", selected = "마포구", recent = listOf("강북구")).cleared()
        assertEquals("", state.query)
        assertNull(state.selected)
        assertEquals(listOf("강북구"), state.recent)
    }

    @Test
    fun 검색어가_있을_때만_결과를_보여_준다() {
        assertFalse(LocationSearchState().isSearching)
        assertFalse(LocationSearchState(query = "  ").isSearching)
        assertTrue(LocationSearchState(query = "마포").isSearching)
    }
}
