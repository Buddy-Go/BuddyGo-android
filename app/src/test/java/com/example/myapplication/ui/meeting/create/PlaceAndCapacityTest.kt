package com.example.myapplication.ui.meeting.create

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.ui.explore.GeoPoint
import java.util.TimeZone
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

// 모임 개설의 장소(10g), 정원·성별, 달력
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceAndCapacityTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val created = mutableListOf<MeetingCreateViewModel>()

    @Before fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        PlaceSelectionStore.consumeConfirmed()
        Dispatchers.resetMain()
    }

    private fun vm(repo: FakeMeetingRepository = FakeMeetingRepository(emptyList())) =
        MeetingCreateViewModel(FakeUserRepository(), repo, nowMillis = { 1_798_761_600_000L }).also { created += it }

    // ---- 장소 검색 ----

    @Test
    fun 카카오_키워드_응답에서_장소를_뽑고_거리는_카카오_값을_쓴다() {
        val json = """{"documents":[{"place_name":"스포애니 합정점","road_address_name":"서울 마포구 양화로 45","address_name":"서울 마포구 서교동 464-1","x":"126.9139","y":"37.5496","distance":"1200"},
            {"place_name":"","x":"1","y":"2"},{"place_name":"좌표 없음","x":"","y":""}]}"""
        val r = KakaoPlaceParser.parse(json, null)
        assertEquals(1, r.size)
        assertEquals("스포애니 합정점", r[0].name)
        assertEquals("서울 마포구 양화로 45", r[0].address)
        assertEquals("서울 마포구 서교동 464-1", r[0].lotAddress)
        assertEquals(37.5496, r[0].latitude, 0.0)
        assertEquals(1200, r[0].distanceMeters)
    }

    @Test
    fun 도로명_주소가_없으면_지번을_주소로_쓰고_거리는_내_위치에서_잰다() {
        val json = """{"documents":[{"place_name":"북한산","road_address_name":"","address_name":"서울 강북구 우이동 산 1","x":"127.0114","y":"37.6586"}]}"""
        val r = KakaoPlaceParser.parse(json, GeoPoint(37.6586, 127.0114))
        assertEquals("서울 강북구 우이동 산 1", r[0].address)
        assertEquals(0, r[0].distanceMeters)
    }

    @Test
    fun 예시_장소는_이름이나_주소로_찾고_내_위치가_있으면_가까운_순이다() {
        val near = GeoPoint(37.5557, 126.9014) // 망원
        val r = SamplePlaces.search("스포애니", near)
        assertEquals(listOf("스포애니 망원점", "스포애니 합정점", "스포애니 신촌점"), r.map { it.name })
        assertTrue(r.zipWithNext().all { (a, b) -> a.distanceMeters!! <= b.distanceMeters!! })
        assertEquals(1, SamplePlaces.search("양화로", null).size)
        assertTrue(SamplePlaces.search("  ", null).isEmpty())
        assertTrue(SamplePlaces.search("없는곳", null).isEmpty())
    }

    @Test
    fun 최근_장소는_앞으로_올리고_다섯_개까지만_둔다() {
        val a = SamplePlaces.all[0]
        val list = SamplePlaces.all.reversed().fold(emptyList<PlaceResult>()) { acc, p -> acc.withRecent(p) }
        assertEquals(5, list.size)
        assertEquals(a.name, list.first().name)
        assertEquals(listOf(a.name), listOf(SamplePlaces.all).flatten().filter { it.name == a.name }.map { it.name })
        assertEquals(list.size, list.withRecent(list[2]).size)
        assertEquals(list[2].name, list.withRecent(list[2]).first().name)
    }

    @Test
    fun 검색_상태는_입력이_있거나_주변_찾기면_결과를_보인다() {
        assertFalse(PlaceSearchState().showsResults)
        assertTrue(PlaceSearchState(query = "스").showsResults)
        assertTrue(PlaceSearchState(searchedNearby = true).showsResults)
        assertTrue(PlaceSearchState(query = "스").isEmptyResult)
        assertFalse(PlaceSearchState(query = "스", loading = true).isEmptyResult)
        assertFalse(PlaceSearchState(query = "스", failed = true).isEmptyResult)
    }

    @Test
    fun 검색_ViewModel은_결과를_채우고_실패를_알린다() {
        val ok = PlaceSearchViewModel(PlaceSearcher { _, _ -> SamplePlaces.all.take(2) }, debounceMs = 0)
        ok.onQueryChange("스포")
        assertEquals(2, ok.state.value.results.size)
        assertFalse(ok.state.value.failed)
        ok.onClearQuery()
        assertTrue(ok.state.value.results.isEmpty())
        val bad = PlaceSearchViewModel(PlaceSearcher { _, _ -> error("네트워크") }, debounceMs = 0)
        bad.onQueryChange("스포")
        assertTrue(bad.state.value.failed)
        ok.viewModelScope.cancel(); bad.viewModelScope.cancel()
    }

    @Test
    fun 위치를_못_얻으면_주변_찾기는_실패로_안내한다() {
        val v = PlaceSearchViewModel(PlaceSearcher { _, _ -> SamplePlaces.all }, debounceMs = 0)
        v.onNearby(null)
        assertTrue(v.state.value.failed)
        assertTrue(v.state.value.showsResults)
        v.onNearby(GeoPoint(37.5496, 126.9139))
        assertFalse(v.state.value.failed)
        assertEquals(SamplePlaces.all.size, v.state.value.results.size)
        v.viewModelScope.cancel()
    }

    // ---- 장소 확인 → 개설 화면 ----

    @Test
    fun 장소를_설정하면_개설_화면에_들어가고_좌표가_모임으로_간다() {
        val v = vm()
        val picked = SamplePlaces.all[0]
        PlaceSelectionStore.confirm(PlaceSelection(picked.name, "3층", picked.address, picked.lotAddress, picked.latitude, picked.longitude))
        assertEquals(picked.name, v.state.place?.name)
        assertEquals("3층", v.state.place?.detail)
        assertNull("받아 갔으면 비운다", PlaceSelectionStore.confirmed.value)
        val place = v.state.place!!.toMeetingPlace()
        assertEquals(picked.latitude, place.latitude, 0.0)
        assertEquals(picked.lotAddress, place.lotAddress)
        assertEquals("3층", place.detailAddress)
    }

    @Test
    fun 설정한_장소는_최근_장소_맨_앞에_들어간다() {
        val p = SamplePlaces.all[3]
        PlaceSelectionStore.confirm(PlaceSelection(p.name, null, p.address, p.lotAddress, p.latitude, p.longitude))
        assertEquals(p.name, PlaceSelectionStore.recent.value.first().name)
        PlaceSelectionStore.consumeConfirmed()
    }

    // ---- 정원·성별 ----

    @Test
    fun 호스트와_다른_성별_제한은_고를_수_없다() {
        assertEquals(listOf(GenderLimit.ANYONE, GenderLimit.MALE_ONLY), allowedGenderLimits(Gender.MALE))
        assertEquals(listOf(GenderLimit.ANYONE, GenderLimit.FEMALE_ONLY), allowedGenderLimits(Gender.FEMALE))
        assertEquals(GenderLimit.entries, allowedGenderLimits(null))
    }

    @Test
    fun 정원과_성별을_적용한다() {
        val v = vm()
        v.onCapacityClick()
        assertEquals(CreateSheet.CAPACITY, v.state.openSheet)
        v.onCapacityApply(3, GenderLimit.MALE_ONLY) // 목데이터의 나는 남성
        assertEquals(3, v.state.capacity)
        assertEquals(GenderLimit.MALE_ONLY, v.state.gender)
        assertNull(v.state.openSheet)
        assertEquals("3명 · 남성만", v.state.capacitySummary)
    }

    @Test
    fun 헬스_정원_범위_밖이거나_다른_성별_제한은_받지_않는다() {
        val v = vm()
        v.onCapacityApply(5, GenderLimit.ANYONE)
        assertNull(v.state.capacity)
        v.onCapacityApply(2, GenderLimit.FEMALE_ONLY)
        assertNull(v.state.capacity)
    }

    // ---- 달력 ----

    @Test
    fun 달력에서_고른_날짜는_날짜순으로_칩에_들어가고_선택된다() {
        val v = vm()
        val before = v.state.dateOptions.size
        v.onCalendarClick()
        assertTrue(v.state.showCalendar)
        v.onCalendarPick(2027, 1, 20)
        assertFalse(v.state.showCalendar)
        assertEquals(before + 1, v.state.dateOptions.size)
        assertEquals(2027, v.state.dateOptions[v.state.selectedDateIndex!!].year)
        assertEquals(20, v.state.dateOptions[v.state.selectedDateIndex!!].day)
        val order = v.state.dateOptions.map { it.year * 10_000 + it.month * 100 + it.day }
        assertEquals(order.sorted(), order)
    }

    @Test
    fun 이미_있는_날짜를_고르면_칩을_늘리지_않고_그_칩을_선택한다() {
        val v = vm()
        val third = v.state.dateOptions[2]
        v.onCalendarPick(third.year, third.month, third.day)
        assertEquals(5, v.state.dateOptions.size)
        assertEquals(2, v.state.selectedDateIndex)
    }

    @Test
    fun 날짜_칩_이름은_오늘이면_오늘이고_아니면_요일이다() {
        val now = 1_798_761_600_000L
        val today = java.util.Calendar.getInstance(seoul).apply { timeInMillis = now }
        val t = dateOptionOf(today.get(java.util.Calendar.YEAR), today.get(java.util.Calendar.MONTH) + 1, today.get(java.util.Calendar.DAY_OF_MONTH), now, seoul)
        assertEquals("오늘", t.topLabel)
        assertEquals("수", dateOptionOf(2027, 1, 6, now, seoul).topLabel)
    }

    // ---- 개설 ----

    private fun filled(v: MeetingCreateViewModel, startMinutesFromMidnight: Int) {
        v.onTitleChange("제목")
        v.onHealthCriteriaApply(listOf(com.example.myapplication.data.model.BodyPart.CHEST), true)
        v.onTimeApply(startMinutesFromMidnight, 60)
        v.onCapacityApply(2, GenderLimit.ANYONE)
        val p = SamplePlaces.all[0]
        PlaceSelectionStore.confirm(PlaceSelection(p.name, null, p.address, p.lotAddress, p.latitude, p.longitude))
    }

    @Test
    fun 필수_값을_다_채우면_개설할_수_있다() {
        val repo = FakeMeetingRepository(emptyList(), nowMillis = { 0L })
        val v = vm(repo)
        // 날짜는 기본(오늘)이고 시간이 지금부터 90분 이후여야 하므로, 시계를 아주 이전으로 둔 저장소에서는 통과한다.
        filled(v, 12 * 60)
        assertTrue(v.state.canSubmit)
        v.onSubmit()
        assertNotNull(v.state.createdMeetingId)
        val saved = runBlocking { repo.getMeeting(v.state.createdMeetingId!!) }.getOrThrow()
        assertEquals(SamplePlaces.all[0].latitude, saved.place.latitude, 0.0)
        assertEquals(2, saved.capacity)
    }

    @Test
    fun 시작이_지금부터_90분_안이면_개설되지_않고_안내가_나온다() {
        val now = System.currentTimeMillis()
        val repo = FakeMeetingRepository(emptyList(), nowMillis = { now })
        val v = MeetingCreateViewModel(FakeUserRepository(), repo, nowMillis = { now }).also { created += it }
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now + 30 * 60_000L }
        filled(v, cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE))
        v.onSubmit()
        assertNull(v.state.createdMeetingId)
        assertNotNull(v.state.errorMessage)
        assertTrue(runBlocking { repo.getMeetings().getOrThrow() }.isEmpty())
    }

    @Test
    fun 수정_모드와_잠긴_모임에서는_달력과_정원을_열지_않는다() {
        val locked = MockMeetings.hosted.copy(confirmedCount = 2, confirmedIds = listOf("u1", "u2"))
        val v = MeetingCreateViewModel(FakeUserRepository(), FakeMeetingRepository(listOf(locked)), editMeetingId = locked.id, nowMillis = { locked.startAtMillis - 86_400_000L }).also { created += it }
        v.onCalendarClick()
        v.onCapacityClick()
        assertFalse(v.state.showCalendar)
        assertNull(v.state.openSheet)
    }
}
