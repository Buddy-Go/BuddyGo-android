package com.example.myapplication.ui.explore.filter

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSearchViewModelTest {
    @Before
    fun 준비() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        RecentDistrictStore.clear()
        listOf("종로구", "강북구", "마포구").forEach(RecentDistrictStore::add)
    }

    private val created = mutableListOf<LocationSearchViewModel>()

    private fun vm(initial: String?) = LocationSearchViewModel(initial, DistrictSearcher { DistrictCatalog.search(it) }, debounceMs = 0).also { created += it }

    @After
    fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
        RecentDistrictStore.clear()
    }

    @Test
    fun 조건에_있던_구_동이_고른_상태로_시작한다() {
        assertEquals("마포구", vm("마포구").state.value.selected)
        assertNull(vm(null).state.value.selected)
    }

    @Test
    fun 최근_검색을_저장소에서_받는다() {
        assertEquals(listOf("마포구", "강북구", "종로구"), vm(null).state.value.recent)
    }

    @Test
    fun 검색어를_넣으면_결과가_나오고_지우면_최근_검색으로_돌아간다() {
        val vm = vm(null)
        vm.onQueryChange("합정")
        assertTrue(vm.state.value.isSearching)
        assertEquals(listOf("서울 마포구 합정동"), vm.state.value.results.map { it.fullName })
        vm.onClearQuery()
        assertEquals(false, vm.state.value.isSearching)
    }

    @Test
    fun 적용하면_고른_곳을_돌려주고_최근_검색_맨_앞에_쌓는다() {
        val vm = vm(null)
        vm.onSelect("영등포구")
        assertEquals("영등포구", vm.onApply())
        assertEquals("영등포구", RecentDistrictStore.recent.value.first())
        assertEquals("최대 네 개", 4, RecentDistrictStore.recent.value.size)
    }

    @Test
    fun 아무것도_고르지_않고_적용하면_null이라_위치_조건이_풀린다() {
        val vm = vm("마포구")
        vm.onSelect("마포구") // 다시 누르면 해제
        assertNull(vm.onApply())
        assertEquals("고르지 않았으니 최근 검색은 그대로", listOf("마포구", "강북구", "종로구"), RecentDistrictStore.recent.value)
    }

    @Test
    fun 최근_검색을_지우면_목록에서_사라진다() {
        val vm = vm(null)
        vm.onRemoveRecent("강북구")
        assertEquals(listOf("마포구", "종로구"), vm.state.value.recent)
    }

    @Test
    fun 초기화하면_고른_곳과_검색어가_비워진다() {
        val vm = vm("마포구")
        vm.onQueryChange("강")
        vm.onReset()
        assertNull(vm.state.value.selected)
        assertEquals("", vm.state.value.query)
    }
}
