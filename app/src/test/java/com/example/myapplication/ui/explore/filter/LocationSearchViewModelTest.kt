package com.example.myapplication.ui.explore.filter

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.GeoPoint
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

    @Test
    fun 중심_좌표가_있는_지역을_고르면_좌표를_기억해_둔다() {
        DistrictCenters.clear()
        val center = GeoPoint(35.8581745, 128.6306354)
        val vm = LocationSearchViewModel(null, DistrictSearcher { listOf(District("수성구", "대구 수성구", center)) }, debounceMs = 0).also { created += it }
        vm.onQueryChange("수성구")
        vm.onSelect("대구 수성구")
        assertEquals(center, DistrictCenters.get("대구 수성구"))
        DistrictCenters.clear()
    }

    @Test
    fun 좌표가_없는_지역을_골라도_이전에_기억한_좌표를_지우지_않는다() {
        DistrictCenters.clear()
        DistrictCenters.put("서울 마포구", GeoPoint(37.5663, 126.9019))
        val vm = vm(null)
        vm.onQueryChange("마포구")
        vm.onSelect("서울 마포구")
        assertEquals(GeoPoint(37.5663, 126.9019), DistrictCenters.get("서울 마포구"))
        DistrictCenters.clear()
    }

    @Test
    fun 이름이_같은_동이_여러_곳에_있어도_고른_하나만_선택되고_전체_이름으로_적용된다() {
        val 도원동 = listOf(
            District("도원동", "서울 용산구 도원동"), District("도원동", "인천 제물포구 도원동"),
            District("도원동", "대구 달서구 도원동"), District("도원동", "대구 중구 도원동"),
        )
        val vm = LocationSearchViewModel(null, DistrictSearcher { 도원동 }, debounceMs = 0).also { created += it }
        vm.onQueryChange("도원동")
        vm.onSelect("대구 달서구 도원동")
        assertEquals("대구 달서구 도원동", vm.state.value.selected)
        assertEquals(1, vm.state.value.results.count { it.fullName == vm.state.value.selected })
        assertEquals("대구 달서구 도원동", vm.onApply())
    }

    @Test
    fun 검색어만_쓰고_적용해도_결과가_하나뿐이면_그_지역이_적용된다() {
        val only = District("중구", "서울 중구", null)
        val one = LocationSearchViewModel(null, DistrictSearcher { listOf(only) }, debounceMs = 0).also { created += it }
        one.onQueryChange("서울 중구")
        assertEquals("서울 중구", one.onApply())

        val many = LocationSearchViewModel(null, DistrictSearcher { listOf(only, District("중구", "대구 중구", null)) }, debounceMs = 0).also { created += it }
        many.onQueryChange("중구")
        assertNull("결과가 여럿이면 고르지 않은 채로는 적용하지 않는다", many.onApply())
    }
}
