package com.example.myapplication.ui.meeting.detail

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.JoinRequest
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
class ApplyViewModelTest {
    private val created = mutableListOf<ApplyViewModel>()

    @Before
    fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    // 신청이 실패하거나 보낸 내용을 기록하는 가짜 저장소
    private class RecordingMeetings(
        private val applyError: Throwable? = null,
        private val delegate: MeetingRepository = FakeMeetingRepository(),
    ) : MeetingRepository by delegate {
        val sentMessages = mutableListOf<String?>()
        override suspend fun apply(meetingId: String, message: String?): Result<Unit> {
            sentMessages += message
            return applyError?.let { Result.failure(it) } ?: delegate.apply(meetingId, message)
        }
    }

    private fun vm(id: String = MockMeetings.recruiting.id, repo: MeetingRepository = FakeMeetingRepository()) =
        ApplyViewModel(id, repo).also { created += it }

    @Test
    fun 모임과_내_정보를_불러온다() {
        val vm = vm()
        assertTrue(vm.state.value.meeting is UiState.Success)
        assertEquals("민준", vm.state.value.me?.nickname)
    }

    @Test
    fun 없는_모임이면_서버_오류_네트워크_오류면_네트워크_에러다() {
        assertEquals(UiState.Error(ErrorType.SERVER), vm("없는 id").state.value.meeting)
        val failing = object : MeetingRepository by FakeMeetingRepository() {
            override suspend fun getMeeting(meetingId: String): Result<Meeting> = Result.failure(IOException())
        }
        assertEquals(UiState.Error(ErrorType.NETWORK), vm(repo = failing).state.value.meeting)
    }

    @Test
    fun 내_기록_요약이_있으면_보여_주고_없으면_미설정이다() {
        // 내 기록(가슴 80kg, 팔 중급)이 있는 헬스 모임
        assertEquals("가슴 80kg · 팔 중급", vm(MockMeetings.recruiting.id).state.value.recordSummary)
    }

    @Test
    fun 한마디는_이백_자까지만_받고_글자_수를_보여_준다() {
        val vm = vm()
        vm.onMessageChange("가".repeat(300))
        assertEquals(200, vm.state.value.message.length)
        assertEquals("200 / 200", vm.state.value.messageLengthText)
        vm.onMessageChange("")
        assertEquals("0 / 200", vm.state.value.messageLengthText)
    }

    @Test
    fun 한마디를_쓰고_신청하면_바로_보내고_대기_상태가_된다() {
        val repo = RecordingMeetings()
        val vm = vm(repo = repo)
        vm.onMessageChange("  같이 해요  ")
        vm.onSubmit()
        assertEquals(listOf<String?>("같이 해요"), repo.sentMessages)
        assertTrue(vm.state.value.done)
        assertFalse(vm.state.value.confirmEmpty)
        // 신청이 반영됐다: 같은 저장소로 다시 보면 대기 중이다.
        val role = (kotlinx.coroutines.runBlocking { repo.getMeeting(MockMeetings.recruiting.id) }.getOrThrow()).roleOf("u1")
        assertEquals(MeetingRole.PENDING, role)
    }

    @Test
    fun 한마디가_비어_있으면_먼저_확인하고_확인해야_보낸다() {
        val repo = RecordingMeetings()
        val vm = vm(repo = repo)
        vm.onSubmit()
        assertTrue(vm.state.value.confirmEmpty)
        assertTrue("아직 보내지 않았다", repo.sentMessages.isEmpty())
        vm.onConfirmEmpty()
        assertEquals(listOf<String?>(null), repo.sentMessages)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun 공백만_쓰고_신청해도_비어_있는_것으로_본다() {
        val vm = vm()
        vm.onMessageChange("   ")
        vm.onSubmit()
        assertTrue(vm.state.value.confirmEmpty)
    }

    @Test
    fun 확인창에서_한마디_쓰기를_고르면_보내지_않고_창만_닫힌다() {
        val repo = RecordingMeetings()
        val vm = vm(repo = repo)
        vm.onSubmit()
        vm.onDismissConfirmEmpty()
        assertFalse(vm.state.value.confirmEmpty)
        assertFalse(vm.state.value.done)
        assertTrue(repo.sentMessages.isEmpty())
    }

    @Test
    fun 신청이_실패하면_실패_띠가_뜨고_글을_고치면_사라지며_다시_보낼_수_있다() {
        val repo = RecordingMeetings(applyError = IOException("offline"))
        val vm = vm(repo = repo)
        vm.onMessageChange("같이 해요")
        vm.onSubmit()
        assertTrue(vm.state.value.failed)
        assertFalse(vm.state.value.done)
        assertFalse("다시 누를 수 있다", vm.state.value.submitting)
        vm.onMessageChange("같이 해요!")
        assertFalse(vm.state.value.failed)
        vm.onSubmit()
        assertEquals(2, repo.sentMessages.size)
    }
}
