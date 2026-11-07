package com.example.myapplication.ui.meeting.create

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.GeoPoint
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 러닝(10b)·등산(10c) 개설과 집결지 지정(10g-4)
@OptIn(ExperimentalCoroutinesApi::class)
class AllSportsCreateTest {
    private val created = mutableListOf<MeetingCreateViewModel>()

    @Before fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        PlaceSelectionStore.consumeConfirmed()
        PlaceSelectionStore.sport = Sport.HEALTH
        Dispatchers.resetMain()
    }

    private fun vm(repo: FakeMeetingRepository = FakeMeetingRepository(emptyList(), nowMillis = { 0L })) =
        MeetingCreateViewModel(FakeUserRepository(), repo, nowMillis = { 1_798_761_600_000L }).also { created += it }

    // ---- 정원 범위 ----

    @Test
    fun 헬스는_2에서_3명_러닝과_등산은_2명_이상이다() {
        assertEquals(2..3, capacityRangeOf(Sport.HEALTH))
        assertEquals(2..GROUP_CAPACITY_MAX, capacityRangeOf(Sport.RUNNING))
        assertEquals(2..GROUP_CAPACITY_MAX, capacityRangeOf(Sport.HIKING))
    }

    // ---- 종목 선택 ----

    @Test
    fun 기본_종목은_헬스이고_종목을_바꾸면_기준_행_이름과_요약이_바뀐다() {
        val v = vm()
        assertEquals(Sport.HEALTH, v.state.sport)
        assertEquals("기준", v.state.criteriaTitle)
        v.onSportSelect(Sport.HIKING)
        assertEquals("등산 기준", v.state.criteriaTitle)
        assertNull("등산 기준을 고르기 전에는 미입력", v.state.criteriaSummary)
    }

    @Test
    fun 종목마다_기준을_따로_들고_있어_갔다_와도_남는다() {
        val v = vm()
        v.onHealthCriteriaApply(listOf(com.example.myapplication.data.model.BodyPart.CHEST), true)
        v.onSportSelect(Sport.RUNNING)
        v.onRunningCriteriaApply(6f, 360)
        v.onSportSelect(Sport.HEALTH)
        assertTrue(v.state.isCriteriaFilled)
        assertEquals("가슴", v.state.criteriaSummary?.take(2))
        v.onSportSelect(Sport.RUNNING)
        assertEquals("6km · 6'00\" 페이스", v.state.criteriaSummary)
    }

    @Test
    fun 정원이_새_종목의_범위를_벗어나면_다시_고르게_비운다() {
        val v = vm()
        v.onSportSelect(Sport.RUNNING)
        v.onCapacityApply(6, GenderLimit.ANYONE)
        assertEquals(6, v.state.capacity)
        v.onSportSelect(Sport.HEALTH) // 헬스는 2~3명
        assertNull(v.state.capacity)
        v.onCapacityApply(3, GenderLimit.ANYONE)
        v.onSportSelect(Sport.HIKING) // 3명은 등산 범위 안이라 남는다
        assertEquals(3, v.state.capacity)
    }

    @Test
    fun 같은_종목을_다시_고르면_아무것도_바뀌지_않는다() {
        val v = vm()
        v.onTitleChange("제목")
        val before = v.state
        v.onSportSelect(Sport.HEALTH)
        assertEquals(before, v.state)
    }

    @Test
    fun 수정_화면에서는_종목을_바꿀_수_없다() {
        val hosted = com.example.myapplication.data.mock.MockMeetings.hosted
        val v = MeetingCreateViewModel(FakeUserRepository(), FakeMeetingRepository(listOf(hosted)), editMeetingId = hosted.id, nowMillis = { hosted.startAtMillis - 86_400_000L }).also { created += it }
        v.onSportSelect(Sport.RUNNING)
        assertEquals(Sport.HEALTH, v.state.sport)
    }

    // ---- 기준 시트 ----

    @Test
    fun 기준_행을_누르면_종목에_맞는_시트가_열린다() {
        val v = vm()
        v.onCriteriaClick(); assertEquals(CreateSheet.HEALTH_CRITERIA, v.state.openSheet)
        v.onSheetDismiss(); v.onSportSelect(Sport.RUNNING)
        v.onCriteriaClick(); assertEquals(CreateSheet.RUNNING_CRITERIA, v.state.openSheet)
        v.onSheetDismiss(); v.onSportSelect(Sport.HIKING)
        v.onCriteriaClick(); assertEquals(CreateSheet.HIKING_CRITERIA, v.state.openSheet)
    }

    @Test
    fun 러닝_기준은_거리와_페이스가_둘_다_있어야_채워진_것이다() {
        assertFalse(RunningCriteria().isFilled)
        assertFalse(RunningCriteria(distanceKm = 5f).isFilled)
        assertFalse(RunningCriteria(paceSec = 330).isFilled)
        assertTrue(RunningCriteria(5f, 330).isFilled)
        assertEquals("5km · 5'30\" 페이스", RunningCriteria(5f, 330).summary)
        assertEquals("7.5km", formatDistanceKm(7.5f) + "km")
        assertEquals("21", formatDistanceKm(21f))
    }

    // ---- 모임으로 바꾸기 ----

    private fun filled(v: MeetingCreateViewModel, sport: Sport) {
        v.onSportSelect(sport)
        v.onTitleChange("제목")
        v.onTimeApply(12 * 60, 60)
        v.onCapacityApply(2, GenderLimit.ANYONE)
        val p = SamplePlaces.all[0]
        PlaceSelectionStore.confirm(PlaceSelection(p.name, null, p.address, p.lotAddress, p.latitude, p.longitude))
    }

    @Test
    fun 러닝_모임은_거리와_내_페이스를_담아_개설된다() {
        val repo = FakeMeetingRepository(emptyList(), nowMillis = { 0L })
        val v = vm(repo)
        filled(v, Sport.RUNNING)
        v.onRunningCriteriaApply(10f, 330)
        assertTrue(v.state.canSubmit)
        v.onSubmit()
        val saved = runBlocking { repo.getMeeting(v.state.createdMeetingId!!) }.getOrThrow()
        val c = saved.criteria as MeetingCriteria.Running
        assertEquals(10f, c.distanceKm)
        assertEquals(330, c.hostPaceSec)
        assertEquals(Sport.RUNNING, saved.sport)
    }

    @Test
    fun 등산_모임은_성향을_담아_개설된다() {
        val repo = FakeMeetingRepository(emptyList(), nowMillis = { 0L })
        val v = vm(repo)
        filled(v, Sport.HIKING)
        v.onHikingCriteriaApply(HikingStyle.PRO)
        v.onSubmit()
        val saved = runBlocking { repo.getMeeting(v.state.createdMeetingId!!) }.getOrThrow()
        assertEquals(HikingStyle.PRO, (saved.criteria as MeetingCriteria.Hiking).style)
    }

    @Test
    fun 기준을_고르지_않으면_개설할_수_없다() {
        val v = vm()
        filled(v, Sport.RUNNING)
        assertFalse(v.state.canSubmit)
        v.onRunningCriteriaApply(5f, 300)
        assertTrue(v.state.canSubmit)
        assertNotNull(v.state.toMeeting())
    }

    // ---- 집결지 지정 (10g-4) ----

    @Test
    fun 장소_행을_누르면_고르는_모임의_종목을_저장소에_알린다() {
        val v = vm()
        v.onSportSelect(Sport.RUNNING)
        v.onPlaceClick()
        assertEquals(Sport.RUNNING, PlaceSelectionStore.sport)
        v.onSportSelect(Sport.HEALTH)
        v.onPlaceClick()
        assertEquals(Sport.HEALTH, PlaceSelectionStore.sport)
    }

    @Test
    fun 집결지는_이름_주소는_그대로_두고_좌표만_지도_가운데로_바꾼다() {
        val place = SamplePlaces.all[4] // 여의도 한강공원
        val picked = meetingPointSelection(place, GeoPoint(37.5302, 126.9330), "  여의나루역 2번 출구 앞  ")
        assertEquals(place.name, picked.name)
        assertEquals(place.address, picked.address)
        assertEquals(37.5302, picked.latitude, 0.0)
        assertEquals(126.9330, picked.longitude, 0.0)
        assertEquals("여의나루역 2번 출구 앞", picked.detail)
        assertNull(meetingPointSelection(place, GeoPoint(1.0, 2.0), "   ").detail)
    }

    @Test
    fun 집결지로_설정한_좌표가_모임_장소로_간다() {
        val v = vm()
        filled(v, Sport.RUNNING)
        val place = SamplePlaces.all[4]
        PlaceSelectionStore.confirm(meetingPointSelection(place, GeoPoint(37.5302, 126.9330), "앞 편의점"))
        v.onRunningCriteriaApply(5f, 330)
        val m = v.state.toMeeting()!!
        assertEquals(37.5302, m.place.latitude, 0.0)
        assertEquals("앞 편의점", m.place.detailAddress)
    }
}
