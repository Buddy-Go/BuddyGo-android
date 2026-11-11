package com.example.myapplication.ui.explore.filter

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Sport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

// 서버가 받는 종목이 헬스뿐이라 종목 카드를 숨기므로, 탐색 조건은 헬스가 늘 골라진 상태로 시작한다.
@OptIn(ExperimentalCoroutinesApi::class)
class OnlySportFilterTest {
    private val created = mutableListOf<FilterViewModel>()

    @Before fun 준비() { Dispatchers.setMain(UnconfinedTestDispatcher()); FilterStore.clear() }

    @After fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
        FilterStore.clear()
    }

    private fun viewModel(onlySport: Sport?) = FilterViewModel(onlySport = onlySport).also { created += it }

    @Test
    fun 종목이_없는_조건은_하나뿐인_종목으로_채운다() {
        assertEquals(Sport.HEALTH, withOnlySport(MeetingFilter(), Sport.HEALTH).sport)
    }

    @Test
    fun 이미_종목이_있으면_그대로_두고_종목이_여럿이면_채우지_않는다() {
        val f = MeetingFilter(sport = Sport.HEALTH, minMannerTemperature = 30)

        assertEquals(f, withOnlySport(f, Sport.HEALTH))
        assertEquals(MeetingFilter(), withOnlySport(MeetingFilter(), null))
    }

    @Test
    fun 앱_기본값은_서버가_받는_종목_하나로_채운다() {
        assertEquals(Sport.HEALTH, withOnlySport(MeetingFilter()).sport)
    }

    @Test
    fun 헬스가_골라져_있어서_기준_조건을_바로_열_수_있다() {
        // '종목을 먼저 골라 주세요'로 막히지 않는다
        assertNotNull(criteriaSummary(withOnlySport(MeetingFilter(), Sport.HEALTH)))
    }

    @Test
    fun 시작할_때와_초기화한_뒤에도_헬스가_골라져_있다() {
        val vm = viewModel(Sport.HEALTH)
        assertEquals(Sport.HEALTH, vm.state.value.draft.sport)

        vm.onReset()

        assertEquals(Sport.HEALTH, vm.state.value.draft.sport)
    }

    @Test
    fun 종목이_여럿이면_예전처럼_비어서_시작한다() {
        assertEquals(null, viewModel(null).state.value.draft.sport)
    }
}
