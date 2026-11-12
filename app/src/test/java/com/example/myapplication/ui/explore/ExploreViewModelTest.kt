package com.example.myapplication.ui.explore

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockNotifications
import com.example.myapplication.data.model.GeoPoint
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeNotificationRepository
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.filter.District
import com.example.myapplication.ui.explore.filter.DistrictCenters
import com.example.myapplication.ui.explore.filter.DistrictSearcher
import com.example.myapplication.ui.explore.filter.FilterStore
import com.example.myapplication.ui.explore.filter.MeetingFilter
import java.io.IOException
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
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
class ExploreViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun 준비() {
        Dispatchers.setMain(dispatcher)
        FilterStore.clear()
        DistrictCenters.clear()
    }

    // 테스트가 만든 ViewModel은 끝날 때 정리한다. (앱에서는 화면이 사라질 때 정리된다)
    // 정리하지 않으면 남은 수집 코루틴이 다음 테스트에서 이미 해제된 Main에서 깨어나 실패한다.
    private val created = mutableListOf<ExploreViewModel>()

    @After
    fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
        FilterStore.clear()
        DistrictCenters.clear()
        LastKnownLocation.clear()
    }

    // 목 저장소를 그대로 쓰되 일부 호출만 실패시키는 가짜
    private class FailingMeetings(
        private val listError: Throwable? = null,
        private val favoriteError: Throwable? = null,
        private val delegate: MeetingRepository = FakeMeetingRepository(),
    ) : MeetingRepository by delegate {
        override suspend fun getMeetings(area: com.example.myapplication.data.model.MapBounds?): Result<List<Meeting>> =
            listError?.let { Result.failure(it) } ?: delegate.getMeetings(area)

        override suspend fun setFavorite(meetingId: String, favorite: Boolean): Result<Unit> =
            favoriteError?.let { Result.failure(it) } ?: delegate.setFavorite(meetingId, favorite)
    }

    private fun notifications(list: List<Notification>) = FakeNotificationRepository(list)

    private fun viewModel(
        meetings: MeetingRepository = FakeMeetingRepository(),
        notifications: NotificationRepository = FakeNotificationRepository(),
        users: com.example.myapplication.data.repository.UserRepository = com.example.myapplication.data.repository.FakeUserRepository(),
        // 테스트에서 실제 카카오 서버를 부르지 않도록 기본은 아무것도 못 찾는 검색기다.
        districts: DistrictSearcher = DistrictSearcher { emptyList() },
    ) = ExploreViewModel(meetings, notifications, users, districts).also { created += it }

    private fun ExploreViewModel.loaded() = (state.value.meetings as UiState.Success).data

    // getMeetings 호출 순서대로 미리 정한 응답을 준다. 응답이 Deferred면 완료될 때까지 기다린다.
    private class ScriptedMeetings(private val replies: List<suspend () -> Result<List<Meeting>>>) : MeetingRepository by FakeMeetingRepository() {
        var calls = 0
        override suspend fun getMeetings(area: com.example.myapplication.data.model.MapBounds?): Result<List<Meeting>> = replies[calls++]()
    }

    @Test
    fun 새로고침이_잠깐_실패해도_다시_시도해_새_목록을_반영한다() {
        val old = MockMeetings.all.take(1)
        val fresh = MockMeetings.all.take(2)
        val repo = ScriptedMeetings(listOf({ Result.success(old) }, { Result.failure(IOException("x")) }, { Result.success(fresh) }))
        val vm = viewModel(repo)
        vm.refreshMeetings()
        assertEquals("실패하면 보던 목록을 그대로 둔다", old, vm.loaded())
        dispatcher.scheduler.advanceTimeBy(1_500)
        assertEquals(fresh, vm.loaded())
    }

    @Test
    fun 늦게_도착한_이전_응답이_새_목록을_덮어쓰지_않는다() {
        val old = MockMeetings.all.take(1)
        val fresh = MockMeetings.all.take(2)
        val slow = CompletableDeferred<Result<List<Meeting>>>()
        val repo = ScriptedMeetings(listOf({ Result.success(old) }, { slow.await() }, { Result.success(fresh) }))
        val vm = viewModel(repo)
        vm.refreshMeetings() // 느린 요청
        vm.refreshMeetings() // 새 요청이 이전 요청을 취소한다
        slow.complete(Result.success(old))
        assertEquals(fresh, vm.loaded())
    }

    @Test
    fun 만들면_모임을_불러와서_성공_상태가_된다() {
        val vm = viewModel()
        assertEquals(MockMeetings.all.size, vm.loaded().size)
    }

    @Test
    fun 네트워크_오류는_네트워크_에러로_그_밖의_오류는_서버_에러로_보여_준다() {
        val network = viewModel(FailingMeetings(listError = IOException("offline")))
        assertEquals(UiState.Error(ErrorType.NETWORK), network.state.value.meetings)
        val server = viewModel(FailingMeetings(listError = IllegalStateException("500")))
        assertEquals(UiState.Error(ErrorType.SERVER), server.state.value.meetings)
    }

    @Test
    fun 핀을_누르면_선택되고_같은_핀을_다시_누르면_풀린다() {
        val vm = viewModel()
        vm.onSelectPin("서교동 123-4")
        assertEquals("서교동 123-4", vm.state.value.selectedPinKey)
        vm.onSelectPin("서교동 123-4")
        assertNull(vm.state.value.selectedPinKey)
    }

    @Test
    fun 시트를_올리고_내린다() {
        val vm = viewModel()
        vm.onSetSheetExpanded(false)
        assertFalse(vm.state.value.sheetExpanded)
        vm.onSetSheetExpanded(true)
        assertTrue(vm.state.value.sheetExpanded)
    }

    @Test
    fun 찜을_누르면_화면에_반영되고_저장소에도_저장된다() {
        val repo = FakeMeetingRepository()
        val vm = viewModel(repo)
        val id = MockMeetings.recruiting.id
        vm.onToggleFavorite(id)
        assertTrue(vm.loaded().first { it.id == id }.isFavorite)
        vm.load()
        assertTrue("저장소에도 남아 있어 다시 불러와도 켜져 있다", vm.loaded().first { it.id == id }.isFavorite)
    }

    @Test
    fun 찜_저장에_실패하면_원래_값으로_되돌린다() {
        val vm = viewModel(FailingMeetings(favoriteError = IOException("offline")))
        val id = MockMeetings.recruiting.id
        assertFalse(vm.loaded().first { it.id == id }.isFavorite)
        vm.onToggleFavorite(id)
        assertFalse(vm.loaded().first { it.id == id }.isFavorite)
    }

    @Test
    fun 없는_모임의_찜은_아무것도_하지_않는다() {
        val vm = viewModel()
        val before = vm.state.value
        vm.onToggleFavorite("없는 id")
        assertEquals(before, vm.state.value)
    }

    @Test
    fun 종목_칩을_누르면_적용된_조건이_바뀌고_다시_누르면_풀린다() {
        val vm = viewModel()
        vm.onToggleSportChip(Sport.RUNNING)
        assertEquals(Sport.RUNNING, FilterStore.applied.value.sport)
        assertEquals(Sport.RUNNING, vm.state.value.sportChip)
        vm.onToggleSportChip(Sport.RUNNING)
        assertNull(FilterStore.applied.value.sport)
    }

    @Test
    fun 조건_설정에서_적용한_조건이_곧바로_목록에_반영된다() {
        val vm = viewModel()
        val all = vm.state.value.visibleMeetings.size
        FilterStore.apply(MeetingFilter(sport = Sport.HEALTH))
        assertEquals(Sport.HEALTH, vm.state.value.filter.sport)
        assertTrue(vm.state.value.visibleMeetings.all { it.sport == Sport.HEALTH })
        assertTrue(vm.state.value.visibleMeetings.size < all)
    }

    @Test
    fun 조건이_바뀌면_고른_핀은_풀린다() {
        val vm = viewModel()
        vm.onSelectPin("서교동 123-4")
        FilterStore.apply(MeetingFilter(sport = Sport.RUNNING))
        assertNull(vm.state.value.selectedPinKey)
    }

    @Test
    fun 재검색하면_구_동_조건이_풀리고_다른_조건은_유지된다() {
        val vm = viewModel()
        FilterStore.apply(MeetingFilter(sport = Sport.HEALTH, district = "마포구"))
        vm.onCameraMoved()
        assertTrue(vm.state.value.cameraMoved)
        vm.onResearch()
        assertNull(FilterStore.applied.value.district)
        assertEquals(Sport.HEALTH, FilterStore.applied.value.sport)
        assertFalse(vm.state.value.cameraMoved)
    }

    @Test
    fun 찜_칩은_찜한_모임만_남기고_핀_선택을_푼다() {
        val vm = viewModel()
        vm.onSelectPin("서교동 123-4")
        vm.onToggleFavoriteOnly()
        assertTrue(vm.state.value.favoriteOnly)
        assertNull(vm.state.value.selectedPinKey)
        assertTrue(vm.state.value.visibleMeetings.all { it.isFavorite })
    }

    @Test
    fun 읽지_않은_알림이_있으면_벨에_점이_생긴다() {
        val vm = viewModel(notifications = notifications(MockNotifications.all))
        assertFalse("확인하기 전에는 점이 없다", vm.state.value.hasUnreadNotifications)
        vm.refreshNotifications()
        assertTrue(vm.state.value.hasUnreadNotifications)
    }

    @Test
    fun 모두_읽었거나_알림이_없으면_점이_없다() {
        val allRead = viewModel(notifications = notifications(MockNotifications.allRead))
        allRead.refreshNotifications()
        assertFalse(allRead.state.value.hasUnreadNotifications)
        val empty = viewModel(notifications = notifications(MockNotifications.empty))
        empty.refreshNotifications()
        assertFalse(empty.state.value.hasUnreadNotifications)
    }

    @Test
    fun 알림을_불러오지_못하면_점을_보여_주지_않는다() {
        val failing = object : NotificationRepository {
            override suspend fun getNotifications() = Result.failure<List<Notification>>(IOException())
            override suspend fun markAsRead(notificationId: String) = Result.success(Unit)
            override suspend fun markAllAsRead() = Result.success(Unit)
        }
        val vm = viewModel(notifications = failing)
        vm.refreshNotifications()
        assertFalse(vm.state.value.hasUnreadNotifications)
    }

    @Test
    fun 내_위치를_받으면_거리가_그_위치_기준으로_바뀐다() {
        val vm = viewModel()
        val before = vm.state.value.visibleMeetings.first { it.id == MockMeetings.recruiting.id }.distanceMeters
        vm.onMyLocation(GeoPoint(37.5551, 126.8978))
        val after = vm.state.value.visibleMeetings.first { it.id == MockMeetings.recruiting.id }.distanceMeters
        assertTrue(before != after)
        assertTrue(after!! in 2_000..2_500)
    }

    @Test
    fun 위치를_못_얻어도_이전에_받은_위치를_지우지_않는다() {
        val vm = viewModel()
        val me = GeoPoint(37.5551, 126.8978)
        vm.onMyLocation(me)
        vm.onMyLocation(null)
        assertEquals(me, vm.state.value.myLocation)
    }

    @Test
    fun 위치_권한은_한_번만_묻는다() {
        val vm = viewModel()
        assertFalse(vm.state.value.locationAsked)
        vm.onLocationAsked()
        assertTrue(vm.state.value.locationAsked)
    }

    @Test
    fun 알림_설정을_끄면_읽지_않은_알림이_있어도_벨_점이_없다() {
        val users = com.example.myapplication.data.repository.FakeUserRepository()
        val vm = viewModel(notifications = notifications(MockNotifications.all), users = users)
        vm.refreshNotifications()
        assertTrue(vm.state.value.hasUnreadNotifications)
        kotlinx.coroutines.runBlocking { users.setNotificationEnabled(false) }
        vm.refreshNotifications()
        assertFalse(vm.state.value.hasUnreadNotifications)
        kotlinx.coroutines.runBlocking { users.setNotificationEnabled(true) }
        vm.refreshNotifications()
        assertTrue(vm.state.value.hasUnreadNotifications)
    }

    // ---- 지도 영역 재검색 (service-rules 2장) ----

    private val mapo = com.example.myapplication.data.model.MapBounds(37.54, 126.88, 37.57, 126.95) // 합정·서교동 일대
    private fun visibleIds(vm: ExploreViewModel) = vm.loaded().map { it.id }.toSet()

    @Test
    fun 지도를_옮기면_보이는_영역을_기억하고_재검색하면_그_영역_안의_모임만_받는다() {
        val vm = viewModel()
        val total = vm.loaded().size
        vm.onCameraMoved(mapo)
        assertEquals("옮기기만 해서는 목록이 바뀌지 않는다", total, vm.loaded().size)
        vm.onResearch()
        assertEquals(mapo, vm.state.value.searchBounds)
        assertTrue(vm.loaded().isNotEmpty())
        assertTrue(vm.loaded().all { mapo.contains(it.place.latitude, it.place.longitude) })
        assertFalse("북한산 모임은 영역 밖이라 빠진다", MockMeetings.inProgress.id in visibleIds(vm))
        assertTrue(vm.loaded().size < total)
    }

    @Test
    fun 영역을_못_읽어도_재검색은_전체를_다시_받는다() {
        val vm = viewModel()
        val total = vm.loaded().size
        vm.onCameraMoved(null)
        vm.onResearch()
        assertNull(vm.state.value.searchBounds)
        assertEquals(total, vm.loaded().size)
    }

    @Test
    fun 다시_옮기고_재검색하면_새_영역으로_바뀐다() {
        val vm = viewModel()
        vm.onCameraMoved(mapo); vm.onResearch()
        val north = com.example.myapplication.data.model.MapBounds(37.64, 127.0, 37.68, 127.05) // 북한산 일대
        vm.onCameraMoved(north); vm.onResearch()
        assertEquals(north, vm.state.value.searchBounds)
        assertTrue(MockMeetings.inProgress.id in visibleIds(vm))
        assertFalse(MockMeetings.recruiting.id in visibleIds(vm))
    }

    @Test
    fun 재검색한_뒤_구_동을_고르면_영역_기준이_풀리고_전체에서_찾는다() {
        val vm = viewModel()
        vm.onCameraMoved(mapo); vm.onResearch()
        val bounded = vm.loaded().size
        FilterStore.apply(MeetingFilter(district = "강북구"))
        assertNull(vm.state.value.searchBounds)
        assertTrue("영역 밖이던 모임도 다시 받아 온다", vm.loaded().size > bounded)
    }

    @Test
    fun 종목을_바꿔도_재검색한_영역은_유지된다() {
        val vm = viewModel()
        vm.onCameraMoved(mapo); vm.onResearch()
        FilterStore.apply(MeetingFilter(sport = Sport.HEALTH))
        assertEquals(mapo, vm.state.value.searchBounds)
    }

    // ---- '현 지도에서 검색' 버튼 (네이버 지도 방식) ----

    @Test
    fun 처음에는_검색_버튼이_보이지_않는다() {
        assertFalse(viewModel().state.value.showResearchButton)
    }

    @Test
    fun 지도를_옮기면_버튼이_뜨지만_목록은_자동으로_다시_부르지_않는다() {
        val vm = viewModel()
        val before = vm.loaded()
        vm.onCameraMoved(mapo)
        assertTrue(vm.state.value.showResearchButton)
        assertEquals("버튼을 누르기 전에는 목록이 그대로", before, vm.loaded())
        assertNull(vm.state.value.searchBounds)
    }

    @Test
    fun 검색을_누르면_버튼이_사라지고_새_범위로_불러온다() {
        val vm = viewModel()
        vm.onCameraMoved(mapo)
        vm.onResearch()
        assertFalse(vm.state.value.showResearchButton)
        assertEquals(mapo, vm.state.value.searchBounds)
    }

    @Test
    fun 핀을_고르는_동안에는_옮겨도_버튼을_숨긴다() {
        val vm = viewModel()
        vm.onCameraMoved(mapo)
        vm.onSelectPin(vm.state.value.pins.first().key)
        assertFalse(vm.state.value.showResearchButton)
        vm.onClearPin()
        assertTrue("핀 선택을 풀면 다시 보인다", vm.state.value.showResearchButton)
    }

    @Test
    fun 다시_옮기면_버튼이_다시_뜬다() {
        val vm = viewModel()
        vm.onCameraMoved(mapo); vm.onResearch()
        assertFalse(vm.state.value.showResearchButton)
        vm.onCameraMoved(mapo)
        assertTrue(vm.state.value.showResearchButton)
    }

    // ---- 위치 조건을 고르면 지도가 그 지역으로 옮겨 간다 ----

    private val 수성구 = GeoPoint(latitude = 35.8581745, longitude = 128.6306354)

    @Test
    fun 처음에는_지도를_옮기라는_요청이_없다() {
        assertNull(viewModel().state.value.mapFocus)
    }

    @Test
    fun 중심을_아는_지역을_고르면_지도를_그곳으로_옮긴다() {
        val vm = viewModel()
        DistrictCenters.put("수성구", 수성구)
        FilterStore.apply(MeetingFilter(district = "수성구"))
        assertEquals(수성구, vm.state.value.mapFocus?.point)
    }

    @Test
    fun 중심을_모르면_이름으로_찾아서_옮긴다() {
        val vm = viewModel(districts = DistrictSearcher { listOf(District("수성구", "대구 수성구", 수성구)) })
        FilterStore.apply(MeetingFilter(district = "수성구"))
        assertEquals(수성구, vm.state.value.mapFocus?.point)
        assertEquals(수성구, DistrictCenters.get("수성구")) // 다음에는 찾지 않아도 된다
    }

    @Test
    fun 이름으로_찾을_때는_이름이_같고_좌표가_있는_결과만_쓴다() {
        val vm = viewModel(districts = DistrictSearcher {
            listOf(District("수성동", "대구 수성동", GeoPoint(1.0, 1.0)), District("수성구", "대구 수성구", null))
        })
        FilterStore.apply(MeetingFilter(district = "수성구"))
        assertNull(vm.state.value.mapFocus)
    }

    @Test
    fun 찾지_못하거나_검색이_실패하면_지도는_그대로다() {
        val vm = viewModel(districts = DistrictSearcher { throw IOException("끊김") })
        FilterStore.apply(MeetingFilter(district = "수성구"))
        assertNull(vm.state.value.mapFocus)
    }

    @Test
    fun 위치_조건을_풀면_지도를_옮기지_않는다() {
        val vm = viewModel()
        DistrictCenters.put("수성구", 수성구)
        FilterStore.apply(MeetingFilter(district = "수성구"))
        val first = vm.state.value.mapFocus
        FilterStore.apply(MeetingFilter())
        assertEquals(first, vm.state.value.mapFocus)
    }

    @Test
    fun 다른_조건만_바꾸면_지도를_다시_옮기지_않는다() {
        val vm = viewModel()
        DistrictCenters.put("수성구", 수성구)
        FilterStore.apply(MeetingFilter(district = "수성구"))
        val first = vm.state.value.mapFocus
        FilterStore.apply(MeetingFilter(district = "수성구", sport = Sport.HEALTH))
        assertEquals(first, vm.state.value.mapFocus)
    }

    @Test
    fun 다른_지역으로_바꾸면_요청_번호가_바뀌며_새_지역으로_옮긴다() {
        val vm = viewModel()
        val 마포구 = GeoPoint(37.5663, 126.9019)
        DistrictCenters.put("수성구", 수성구)
        DistrictCenters.put("마포구", 마포구)
        FilterStore.apply(MeetingFilter(district = "수성구"))
        val first = vm.state.value.mapFocus!!
        FilterStore.apply(MeetingFilter(district = "마포구"))
        val second = vm.state.value.mapFocus!!
        assertEquals(마포구, second.point)
        assertTrue(second.id > first.id)
    }

    @Test
    fun 전체_이름으로_저장된_조건도_이름으로_찾아_지도를_옮긴다() {
        val vm = viewModel(districts = DistrictSearcher { listOf(District("수성구", "대구 수성구", 수성구)) })
        FilterStore.apply(MeetingFilter(district = "대구 수성구"))
        assertEquals(수성구, vm.state.value.mapFocus?.point)
    }

    @Test
    fun 시트를_직접_올리거나_내리면_핀_때문에_올렸다는_표시가_지워진다() {
        val vm = viewModel()
        vm.onSetSheetExpanded(false)
        val key = vm.state.value.pins.first { it.isMultiple }.key
        vm.onSelectPin(key)
        assertTrue(vm.state.value.sheetRaisedByPin)
        vm.onSetSheetExpanded(true)
        assertFalse(vm.state.value.sheetRaisedByPin)
    }

    @Test
    fun 여러_모임이_있는_핀을_누르면_시트가_올라오고_다시_누르면_내려간다() {
        val vm = viewModel()
        vm.onSetSheetExpanded(false)
        val key = vm.state.value.pins.first { it.isMultiple }.key
        vm.onSelectPin(key)
        assertTrue(vm.state.value.sheetExpanded)
        vm.onClearPin()
        assertFalse(vm.state.value.sheetExpanded)
    }
}
