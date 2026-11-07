package com.example.myapplication.ui.explore

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockNotifications
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeNotificationRepository
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.filter.FilterStore
import com.example.myapplication.ui.explore.filter.MeetingFilter
import java.io.IOException
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
class ExploreViewModelTest {
    @Before
    fun 준비() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        FilterStore.clear()
    }

    // 테스트가 만든 ViewModel은 끝날 때 정리한다. (앱에서는 화면이 사라질 때 정리된다)
    // 정리하지 않으면 남은 수집 코루틴이 다음 테스트에서 이미 해제된 Main에서 깨어나 실패한다.
    private val created = mutableListOf<ExploreViewModel>()

    @After
    fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
        FilterStore.clear()
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
    ) = ExploreViewModel(meetings, notifications, users).also { created += it }

    private fun ExploreViewModel.loaded() = (state.value.meetings as UiState.Success).data

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
}
