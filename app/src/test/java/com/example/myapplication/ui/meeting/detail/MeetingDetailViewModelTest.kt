package com.example.myapplication.ui.meeting.detail

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
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
        private val delegate: MeetingRepository = FakeMeetingRepository(),
    ) : MeetingRepository by delegate {
        override suspend fun getMeeting(meetingId: String): Result<Meeting> =
            getError?.let { Result.failure(it) } ?: delegate.getMeeting(meetingId)
        override suspend fun setFavorite(meetingId: String, favorite: Boolean): Result<Unit> =
            favoriteError?.let { Result.failure(it) } ?: delegate.setFavorite(meetingId, favorite)
        override suspend fun cancelApplication(meetingId: String): Result<Unit> =
            cancelError?.let { Result.failure(it) } ?: delegate.cancelApplication(meetingId)
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
}
