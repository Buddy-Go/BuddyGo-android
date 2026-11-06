package com.example.myapplication.ui.meeting.detail

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.CheckInOutOfRangeException
import com.example.myapplication.data.model.CheckInResult
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.GeoPoint
import com.example.myapplication.ui.explore.LastKnownLocation
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
class MeetingDetailViewModelTest {
    private val created = mutableListOf<MeetingDetailViewModel>()

    @Before
    fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private class FailingMeetings(
        private val getError: Throwable? = null,
        private val favoriteError: Throwable? = null,
        private val cancelError: Throwable? = null,
        private val checkInError: Throwable? = null,
        private val leaveError: Throwable? = null,
        private val delegate: MeetingRepository = FakeMeetingRepository(),
    ) : MeetingRepository by delegate {
        override suspend fun getMeeting(meetingId: String): Result<Meeting> =
            getError?.let { Result.failure(it) } ?: delegate.getMeeting(meetingId)
        override suspend fun setFavorite(meetingId: String, favorite: Boolean): Result<Unit> =
            favoriteError?.let { Result.failure(it) } ?: delegate.setFavorite(meetingId, favorite)
        override suspend fun cancelApplication(meetingId: String): Result<Unit> =
            cancelError?.let { Result.failure(it) } ?: delegate.cancelApplication(meetingId)
        override suspend fun leave(meetingId: String): Result<Unit> =
            leaveError?.let { Result.failure(it) } ?: delegate.leave(meetingId)
        override suspend fun checkIn(meetingId: String, latitude: Double, longitude: Double): Result<CheckInResult> =
            checkInError?.let { Result.failure(it) } ?: delegate.checkIn(meetingId, latitude, longitude)
    }

    private fun vm(id: String, repo: MeetingRepository = FakeMeetingRepository()) =
        MeetingDetailViewModel(id, repo, nowMillis = 1_798_761_600_000L).also { created += it }

    @Test
    fun 모임을_불러오고_내_역할을_판단한다() {
        val noRole = vm(MockMeetings.recruiting.id)
        assertTrue(noRole.state.value.meeting is UiState.Success)
        assertEquals(MeetingRole.NONE, noRole.state.value.role)
        assertEquals(MeetingRole.PENDING, vm(MockMeetings.pending.id).state.value.role)
        assertEquals(MeetingRole.CONFIRMED, vm(MockMeetings.closed.id).state.value.role)
        assertEquals(MeetingRole.HOST, vm(MockMeetings.hosted.id).state.value.role)
    }

    @Test
    fun 없는_모임이면_서버_오류_네트워크_오류면_네트워크_에러다() {
        assertEquals(UiState.Error(ErrorType.SERVER), vm("없는 id").state.value.meeting)
        assertEquals(UiState.Error(ErrorType.NETWORK), vm("m1", FailingMeetings(getError = IOException())).state.value.meeting)
    }

    @Test
    fun 하단_버튼은_내_역할에_맞게_나온다() {
        assertEquals(DetailAction.APPLY, vm(MockMeetings.recruiting.id).state.value.bottomAction?.action)
        assertEquals(DetailAction.CANCEL_APPLICATION, vm(MockMeetings.pending.id).state.value.bottomAction?.action)
        assertNull("호스트는 호스트 영역이 맡는다", vm(MockMeetings.hosted.id).state.value.bottomAction)
    }

    @Test
    fun 찜을_누르면_바로_바뀌고_저장되며_다시_누르면_풀린다() {
        val repo = FakeMeetingRepository()
        val vm = vm(MockMeetings.recruiting.id, repo)
        assertFalse(vm.state.value.loaded!!.isFavorite)
        vm.onToggleFavorite()
        assertTrue(vm.state.value.loaded!!.isFavorite)
        vm.load()
        assertTrue("저장소에도 남았다", vm.state.value.loaded!!.isFavorite)
        vm.onToggleFavorite()
        assertFalse(vm.state.value.loaded!!.isFavorite)
    }

    @Test
    fun 찜_저장에_실패하면_되돌리고_안내한다() {
        val vm = vm(MockMeetings.recruiting.id, FailingMeetings(favoriteError = IOException()))
        vm.onToggleFavorite()
        assertFalse(vm.state.value.loaded!!.isFavorite)
        assertEquals("찜을 저장하지 못했어요. 다시 시도해주세요.", vm.state.value.message)
        vm.onMessageShown()
        assertNull(vm.state.value.message)
    }

    @Test
    fun 신청을_취소하면_대기가_풀려_다시_신청할_수_있다() {
        val vm = vm(MockMeetings.pending.id)
        assertEquals(MeetingRole.PENDING, vm.state.value.role)
        vm.onCancelApplication()
        assertEquals(MeetingRole.NONE, vm.state.value.role)
        assertEquals(DetailAction.APPLY, vm.state.value.bottomAction?.action)
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun 신청_취소에_실패하면_대기_상태가_그대로이고_안내한다() {
        val vm = vm(MockMeetings.pending.id, FailingMeetings(cancelError = IOException()))
        vm.onCancelApplication()
        assertEquals(MeetingRole.PENDING, vm.state.value.role)
        assertEquals("신청을 취소하지 못했어요. 다시 시도해주세요.", vm.state.value.message)
        assertFalse("다시 누를 수 있다", vm.state.value.submitting)
    }

    @Test
    fun 시트를_열고_닫는다() {
        val vm = vm(MockMeetings.recruiting.id)
        vm.onOpenSheet(DetailSheet.PLACE)
        assertEquals(DetailSheet.PLACE, vm.state.value.openSheet)
        vm.onCloseSheet()
        assertNull(vm.state.value.openSheet)
    }

    @Test
    fun 탐색에서_알아낸_내_위치로_장소_시트의_거리를_다시_잰다() {
        LastKnownLocation.clear()
        val noLocation = vm(MockMeetings.recruiting.id)
        assertEquals("위치를 모르면 가져온 거리", MockMeetings.recruiting.distanceMeters, noLocation.state.value.distanceMeters)
        LastKnownLocation.set(GeoPoint(37.5551, 126.8978))
        val withLocation = vm(MockMeetings.recruiting.id)
        assertTrue(withLocation.state.value.distanceMeters!! in 2_000..2_500)
        LastKnownLocation.clear()
    }

    @Test
    fun 내_기록을_불러와_기준_시트에서_비교할_수_있다() {
        assertEquals(80, vm(MockMeetings.recruiting.id).state.value.myRecord?.benchPressKg)
    }

    // ---- 체크인 ----

    private val mountainId = MockMeetings.inProgress.id // 북한산 입구 37.6586, 127.0114
    private val mountain = GeoPoint(37.6586, 127.0114)

    @Test
    fun 체크인하기를_누르면_위치를_가져오는_중이다() {
        val vm = vm(mountainId)
        vm.onCheckInStart()
        assertEquals(CheckInPrompt.Locating, vm.state.value.checkIn)
    }

    @Test
    fun 위치를_못_얻으면_위치_안내를_보여_준다() {
        val vm = vm(mountainId)
        vm.onCheckInStart()
        vm.onCheckInLocation(null)
        assertEquals(CheckInPrompt.NoLocation, vm.state.value.checkIn)
    }

    @Test
    fun 모임_장소_근처이면_체크인할_수_있는_거리를_보여_준다() {
        val vm = vm(mountainId)
        vm.onCheckInStart()
        vm.onCheckInLocation(mountain)
        assertEquals(CheckInPrompt.Ready(0), vm.state.value.checkIn)
    }

    @Test
    fun 멀면_거리와_함께_범위_밖으로_보여_주고_체크인을_보내지_않는다() {
        val repo = FakeMeetingRepository()
        val vm = vm(mountainId, repo)
        vm.onCheckInStart()
        vm.onCheckInLocation(GeoPoint(37.6586 + 0.003, 127.0114))
        val ready = vm.state.value.checkIn as CheckInPrompt.Ready
        assertTrue(ready.distanceMeters in 300..360)
        vm.onCheckInConfirm()
        assertEquals("범위 밖이면 보내지 않아 그대로다", ready, vm.state.value.checkIn)
        assertEquals(1, vm.state.value.loaded!!.checkedInCount)
    }

    @Test
    fun 체크인에_성공하면_창이_닫히고_완료_안내와_체크인_수가_바뀐다() {
        val vm = vm(mountainId)
        vm.onCheckInStart()
        vm.onCheckInLocation(mountain)
        vm.onCheckInConfirm()
        assertNull(vm.state.value.checkIn)
        assertEquals("체크인했어요", vm.state.value.notice)
        assertEquals(2, vm.state.value.loaded!!.checkedInCount)
        assertEquals("체크인 완료", vm.state.value.bottomAction?.title)
        vm.onNoticeShown()
        assertNull(vm.state.value.notice)
    }

    @Test
    fun 서버가_범위_밖이라고_하면_서버가_잰_거리로_다시_보여_준다() {
        val vm = vm(mountainId, FailingMeetings(checkInError = CheckInOutOfRangeException(77)))
        vm.onCheckInStart()
        vm.onCheckInLocation(mountain) // 앱이 잰 거리는 0m라 보내지만 서버는 77m라고 답한다
        vm.onCheckInConfirm()
        assertEquals(CheckInPrompt.Ready(77), vm.state.value.checkIn)
        assertFalse(canCheckIn(77))
    }

    @Test
    fun 체크인을_보내지_못하면_실패_안내를_보여_준다() {
        val vm = vm(mountainId, FailingMeetings(checkInError = IOException("offline")))
        vm.onCheckInStart()
        vm.onCheckInLocation(mountain)
        vm.onCheckInConfirm()
        assertEquals(CheckInPrompt.Failed, vm.state.value.checkIn)
    }

    @Test
    fun 확인창을_닫으면_체크인_상태가_지워진다() {
        val vm = vm(mountainId)
        vm.onCheckInStart()
        vm.onCheckInLocation(mountain)
        vm.onCheckInDismiss()
        assertNull(vm.state.value.checkIn)
        assertNull(vm.state.value.checkInPoint)
    }

    @Test
    fun 창을_닫은_뒤에_늦게_온_위치는_무시한다() {
        val vm = vm(mountainId)
        vm.onCheckInStart()
        vm.onCheckInDismiss()
        vm.onCheckInLocation(mountain)
        assertNull(vm.state.value.checkIn)
    }

    @Test
    fun 위치를_받으면_마지막_내_위치로_저장한다() {
        LastKnownLocation.clear()
        val vm = vm(mountainId)
        vm.onCheckInStart()
        vm.onCheckInLocation(mountain)
        assertEquals(mountain, LastKnownLocation.point.value)
        LastKnownLocation.clear()
    }

    // ---- 나가기 ----

    private val closedId = MockMeetings.closed.id // 마감 · 내가 확정

    @Test
    fun 확정된_참가자에게는_나가기_메뉴가_있다() {
        // 목데이터의 마감 모임은 시작이 한참 뒤라 데드라인 전이다.
        assertTrue(vm(closedId).state.value.leavePlan != null)
        assertNull("호스트는 나가기가 없다", vm(MockMeetings.hosted.id).state.value.leavePlan)
        assertNull("신청하지 않았으면 없다", vm(MockMeetings.recruiting.id).state.value.leavePlan)
    }

    @Test
    fun 나가기를_누르면_확인창이_뜨고_닫을_수_있다() {
        val vm = vm(closedId)
        vm.onLeaveClick()
        assertEquals(vm.state.value.leavePlan, vm.state.value.leaveDialog)
        vm.onLeaveDismiss()
        assertNull(vm.state.value.leaveDialog)
    }

    @Test
    fun 나갈_수_없는_사람이_나가기를_눌러도_창이_뜨지_않는다() {
        val vm = vm(MockMeetings.recruiting.id)
        vm.onLeaveClick()
        assertNull(vm.state.value.leaveDialog)
    }

    @Test
    fun 나가기에_성공하면_내_역할이_없어지고_안내가_뜬다() {
        val vm = vm(closedId)
        assertEquals(MeetingRole.CONFIRMED, vm.state.value.role)
        vm.onLeaveClick()
        vm.onLeaveConfirm()
        assertNull(vm.state.value.leaveDialog)
        assertEquals("모임에서 나갔어요", vm.state.value.notice)
        assertEquals(MeetingRole.NONE, vm.state.value.role)
        assertEquals("확정 인원이 하나 줄었다", 1, vm.state.value.loaded!!.confirmedCount)
        assertEquals("마감이 풀려 모집중이 됐다", com.example.myapplication.data.model.MeetingStatus.RECRUITING, vm.state.value.loaded!!.status)
        assertNull("더 이상 나가기가 없다", vm.state.value.leavePlan)
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun 나가기에_실패하면_역할이_그대로이고_안내한다() {
        val vm = vm(closedId, FailingMeetings(leaveError = IOException()))
        vm.onLeaveClick()
        vm.onLeaveConfirm()
        assertEquals(MeetingRole.CONFIRMED, vm.state.value.role)
        assertEquals("나가지 못했어요. 다시 시도해주세요.", vm.state.value.message)
        assertNull(vm.state.value.leaveDialog)
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun 확인창이_없는데_나가기를_확인해도_아무_일도_없다() {
        val vm = vm(closedId)
        vm.onLeaveConfirm()
        assertEquals(MeetingRole.CONFIRMED, vm.state.value.role)
    }
}
