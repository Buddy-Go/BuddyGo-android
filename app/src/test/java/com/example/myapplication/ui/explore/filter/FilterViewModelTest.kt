package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.Sport
import java.util.Calendar
import java.util.TimeZone
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilterViewModelTest {
    @Before
    fun 준비() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        FilterStore.clear()
    }

    private val created = mutableListOf<FilterViewModel>()

    @After
    fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
        FilterStore.clear()
    }

    private val now = 1_798_761_600_000L
    // 종목이 여럿인 상황(onlySport = null)으로 돌려 종목 고르기·해제 동작을 그대로 확인한다. 하나뿐일 때는 OnlySportFilterTest
    private fun viewModel() = FilterViewModel(nowMillis = now, onlySport = null).also { created += it }
    private val FilterViewModel.draft get() = state.value.draft

    @Test
    fun 지금_적용된_조건에서_시작한다() {
        FilterStore.apply(MeetingFilter(sport = Sport.RUNNING, minMannerTemperature = 50))
        assertEquals(MeetingFilter(sport = Sport.RUNNING, minMannerTemperature = 50), viewModel().draft)
    }

    @Test
    fun 모임과_내_기록을_불러온다() {
        val vm = viewModel()
        assertTrue(vm.state.value.meetings.isNotEmpty())
        assertEquals(80, vm.state.value.myRecord?.benchPressKg)
    }

    @Test
    fun 종목은_하나만_고르고_다시_누르면_풀린다() {
        val vm = viewModel()
        vm.onSelectSport(Sport.HEALTH)
        vm.onSelectSport(Sport.RUNNING)
        assertEquals(Sport.RUNNING, vm.draft.sport)
        vm.onSelectSport(Sport.RUNNING)
        assertNull(vm.draft.sport)
    }

    @Test
    fun 날짜를_다시_누르면_풀리고_시간대는_여러_개를_고른다() {
        val vm = viewModel()
        val day = dateOptions(now).first().startOfDayMillis
        vm.onSelectDate(day)
        assertEquals(day, vm.draft.dateMillis)
        vm.onSelectDate(day)
        assertNull(vm.draft.dateMillis)
        vm.onToggleTimeSlot(TimeSlot.DAWN)
        vm.onToggleTimeSlot(TimeSlot.EVENING)
        assertEquals(setOf(TimeSlot.DAWN, TimeSlot.EVENING), vm.draft.timeSlots)
        vm.onToggleTimeSlot(TimeSlot.DAWN)
        assertEquals(setOf(TimeSlot.EVENING), vm.draft.timeSlots)
    }

    @Test
    fun 달력에서_고른_날은_우리_시간대의_그날_0시로_들어가고_창이_닫힌다() {
        val vm = viewModel()
        vm.onOpenSheet(FilterSheet.DATE_PICKER)
        val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2027, 0, 20, 0, 0, 0) }.timeInMillis
        vm.onPickDate(utcMidnight)
        assertEquals(localStartOfDay(utcMidnight), vm.draft.dateMillis)
        assertNull(vm.state.value.openSheet)
    }

    @Test
    fun 시간_범위를_고르면_시간대_선택은_지워진다() {
        val vm = viewModel()
        vm.onToggleTimeSlot(TimeSlot.EVENING)
        vm.onOpenSheet(FilterSheet.TIME_RANGE)
        vm.onSetTimeStart(9 * 60)
        vm.onSetTimeEnd(10 * 60)
        assertEquals(TimeOfDayRange(9 * 60, 10 * 60), vm.draft.customRange)
        assertTrue(vm.draft.timeSlots.isEmpty())
        vm.onClearTimeRange()
        assertNull(vm.draft.customRange)
    }

    @Test
    fun 시간_범위_시트를_확인_없이_닫으면_범위가_남지_않는다() {
        val vm = viewModel()
        vm.onOpenSheet(FilterSheet.TIME_RANGE)
        assertEquals(TimeOfDayRange.DEFAULT, vm.draft.customRange)
        vm.onDismissSheet()
        assertNull(vm.draft.customRange)
        assertNull(vm.state.value.openSheet)
    }

    @Test
    fun 기준_시트에서_헬스_기준을_켜면_내_기록으로_초기값이_채워진다() {
        val vm = viewModel()
        vm.onSelectSport(Sport.HEALTH)
        vm.onOpenSheet(FilterSheet.CRITERIA)
        vm.onTogglePart(BodyPart.CHEST)
        vm.onSetCriteriaOn(true)
        assertTrue(vm.draft.health.criteriaOn)
        assertEquals(70..90, vm.draft.health.ranges[BodyPart.CHEST])
        vm.onSetRange(BodyPart.CHEST, 100..120)
        vm.onResetToMine()
        assertEquals(70..90, vm.draft.health.ranges[BodyPart.CHEST])
    }

    @Test
    fun 기준_시트를_적용_없이_닫으면_고친_기준이_되돌아간다() {
        val vm = viewModel()
        vm.onSelectSport(Sport.HEALTH)
        vm.onOpenSheet(FilterSheet.CRITERIA)
        vm.onTogglePart(BodyPart.ARM)
        vm.onDismissSheet()
        assertTrue(vm.draft.health.parts.isEmpty())
    }

    @Test
    fun 기준_시트에서_적용하면_고친_기준이_남는다() {
        val vm = viewModel()
        vm.onSelectSport(Sport.HEALTH)
        vm.onOpenSheet(FilterSheet.CRITERIA)
        vm.onTogglePart(BodyPart.ARM)
        vm.onApplySheet()
        assertEquals(setOf(BodyPart.ARM), vm.draft.health.parts)
    }

    @Test
    fun 러닝과_등산_기준도_내_기록이_초기값이다() {
        val vm = viewModel()
        vm.onSelectSport(Sport.RUNNING)
        vm.onSetCriteriaOn(true)
        assertEquals(300..360, vm.draft.running.paceRangeSec)
        vm.onSelectSport(Sport.HIKING)
        vm.onSetCriteriaOn(true)
        assertEquals(com.example.myapplication.data.model.HikingStyle.HOBBY, vm.draft.hiking.style)
    }

    @Test
    fun 종목을_고르지_않으면_기준_스위치는_아무것도_바꾸지_않는다() {
        val vm = viewModel()
        vm.onSetCriteriaOn(true)
        assertEquals(MeetingFilter(), vm.draft)
    }

    @Test
    fun 상세_조건에서_성별과_매너온도를_고른다() {
        val vm = viewModel()
        vm.onSetGender(GenderLimit.FEMALE_ONLY)
        vm.onSetMannerOn(true)
        vm.onSetMinManner(70)
        assertEquals(GenderLimit.FEMALE_ONLY, vm.draft.gender)
        assertEquals(70, vm.draft.minMannerTemperature)
        vm.onSetGender(GenderLimit.ANYONE)
        vm.onSetMannerOn(false)
        assertNull(vm.draft.gender)
        assertNull(vm.draft.minMannerTemperature)
    }

    @Test
    fun 위치_검색에서_돌아온_구_동이_조건에_들어가고_빈_값은_해제다() {
        val vm = viewModel()
        vm.onSetDistrict("마포구")
        assertEquals("마포구", vm.draft.district)
        vm.onSetDistrict("")
        assertNull(vm.draft.district)
        vm.onSetDistrict("마포구")
        vm.onSetDistrict(null)
        assertNull(vm.draft.district)
    }

    @Test
    fun 찾기를_눌러야_탐색에_적용되고_초기화는_고르던_조건을_비운다() {
        val vm = viewModel()
        vm.onSelectSport(Sport.HIKING)
        assertNull("누르기 전에는 적용되지 않는다", FilterStore.applied.value.sport)
        vm.onApply()
        assertEquals(Sport.HIKING, FilterStore.applied.value.sport)
        vm.onReset()
        assertEquals(MeetingFilter(), vm.draft)
        assertEquals("초기화만으로는 적용된 조건이 바뀌지 않는다", Sport.HIKING, FilterStore.applied.value.sport)
    }

    @Test
    fun 찾기_개수는_고르는_조건을_따라간다() {
        val vm = viewModel()
        val all = vm.state.value.resultCount
        vm.onSelectSport(Sport.HEALTH)
        assertTrue(vm.state.value.resultCount < all)
        assertFalse(vm.state.value.resultCount < 0)
    }
}
