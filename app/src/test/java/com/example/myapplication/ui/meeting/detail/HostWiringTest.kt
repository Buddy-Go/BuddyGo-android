package com.example.myapplication.ui.meeting.detail

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.MeetingMember
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.Notice
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeNoticeRepository
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.ui.common.UiState
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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

// 모임 상세에 호스트 영역(5a-H 계열)을 끼우는 규칙과 취소·삭제 동작
@OptIn(ExperimentalCoroutinesApi::class)
class HostWiringTest {
    private val created = mutableListOf<MeetingDetailViewModel>()
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    @Before fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun vm(id: String, repo: FakeMeetingRepository = FakeMeetingRepository()) =
        MeetingDetailViewModel(id, repo, FakeUserRepository(), FakeNoticeRepository(), nowMillis = 1_798_761_600_000L)
            .also { created += it }

    @Test
    fun 호스트일_때만_호스트_영역_값이_채워진다() {
        assertNotNull(vm(MockMeetings.hosted.id).state.value.hostSection)
        assertNull(vm(MockMeetings.recruiting.id).state.value.hostSection)
        assertNull(vm(MockMeetings.closed.id).state.value.hostSection)
    }

    @Test
    fun 호스트_영역에는_확정_명단과_신청자_수가_들어간다() {
        val host = vm(MockMeetings.hosted.id).state.value.hostSection!!
        assertEquals(listOf("민준"), host.confirmedNicknames)
        assertEquals(2, host.pendingApplicantCount)
        assertEquals(3, host.capacity)
        assertFalse(host.isClosed)
        assertFalse(host.isStartedOrLater)
    }

    @Test
    fun 확정_명단에서_호스트가_맨_앞이다() {
        val meeting = MockMeetings.closed.copy(
            members = listOf(MeetingMember("u1", "민준"), MeetingMember("u3", "지훈")),
        )
        assertEquals(listOf("지훈", "민준"), orderedConfirmedNames(meeting, emptyMap()))
        val noMembers = MockMeetings.hosted.copy(members = emptyList(), confirmedIds = listOf("u2", "u1"))
        assertEquals(listOf("민준", "서연"), orderedConfirmedNames(noMembers, mapOf("u1" to "민준", "u2" to "서연")))
    }

    @Test
    fun 진행중_종료_취소된_모임은_수정과_취소_메뉴가_없다() {
        for (status in listOf(MeetingStatus.IN_PROGRESS, MeetingStatus.ENDED, MeetingStatus.CANCELED)) {
            val s = hostSectionStateOf(MockMeetings.hosted.copy(status = status), listOf("민준"), null, seoul)
            assertTrue(status.name, s.isStartedOrLater)
            assertTrue(status.name, s.menuActions.isEmpty())
        }
        assertTrue(hostSectionStateOf(MockMeetings.hosted.copy(status = MeetingStatus.CLOSED), listOf("민준"), null, seoul).isClosed)
    }

    @Test
    fun 마지막_공지가_미리보기로_들어간다() {
        val notice = Notice("n9", "m7", "집합 장소 변경", "후문에서 만나요", 1_788_784_200_000L)
        val s = hostSectionStateOf(MockMeetings.hosted, listOf("민준"), notice, seoul)
        assertEquals("n9", s.latestNotice?.noticeId)
        assertEquals("9/7 21:30", s.latestNotice?.timeText)
        assertEquals("전체", s.noticeActionLabel)
        assertEquals("작성", hostSectionStateOf(MockMeetings.hosted, listOf("민준"), null, seoul).noticeActionLabel)
    }

    @Test
    fun 예시_공지가_있는_모임은_호스트_영역에_공지가_보인다() {
        // FakeNoticeRepository의 예시 공지는 내가 호스트인 m7에 있다.
        val host = vm(MockMeetings.hosted.id).state.value.hostSection!!
        assertNotNull(host.latestNotice)
    }

    @Test
    fun 메뉴의_취소와_삭제는_각각_시트와_확인창을_연다() {
        val v = vm(MockMeetings.hosted.id)
        v.onHostAction(HostAction.CANCEL)
        assertEquals(HostDialog.CANCEL, v.state.value.hostDialog)
        v.onHostDialogDismiss()
        assertNull(v.state.value.hostDialog)
        v.onHostAction(HostAction.DELETE)
        assertEquals(HostDialog.DELETE, v.state.value.hostDialog)
    }

    @Test
    fun 수정은_아직_안내만_한다() {
        val v = vm(MockMeetings.hosted.id)
        v.onHostAction(HostAction.EDIT)
        assertNull(v.state.value.hostDialog)
        assertNotNull(v.state.value.message)
    }

    @Test
    fun 취소하면_모임이_취소_상태가_되고_안내가_나온다() {
        val repo = FakeMeetingRepository()
        val v = vm(MockMeetings.hosted.id, repo)
        v.onHostAction(HostAction.CANCEL)
        v.onCancelMeetingConfirm("갑작스러운 일정이 생겼어요")
        assertEquals(MeetingStatus.CANCELED, v.state.value.loaded?.status)
        assertEquals("모임을 취소했어요", v.state.value.notice)
        assertNull(v.state.value.hostDialog)
    }

    @Test
    fun 취소_사유가_비었거나_너무_길면_취소하지_않는다() {
        val v = vm(MockMeetings.hosted.id)
        v.onCancelMeetingConfirm("   ")
        v.onCancelMeetingConfirm("가".repeat(CANCEL_REASON_MAX_LENGTH + 1))
        assertEquals(MeetingStatus.RECRUITING, v.state.value.loaded?.status)
    }

    @Test
    fun 삭제하면_화면을_닫도록_알린다() {
        val repo = FakeMeetingRepository()
        val v = vm(MockMeetings.hosted.id, repo)
        v.onDeleteMeetingConfirm()
        assertTrue(v.state.value.deleted)
        assertTrue(repo.let { kotlinx.coroutines.runBlocking { it.getMeeting(MockMeetings.hosted.id) } }.isFailure)
    }

    @Test
    fun 확정_참가자가_있는_모임은_삭제할_수_없고_안내한다() {
        val repo = FakeMeetingRepository(listOf(MockMeetings.hosted.copy(confirmedCount = 2, confirmedIds = listOf("u1", "u2"))))
        val v = vm(MockMeetings.hosted.id, repo)
        v.onDeleteMeetingConfirm()
        assertFalse(v.state.value.deleted)
        assertNotNull(v.state.value.message)
        assertTrue(v.state.value.meeting is UiState.Success)
    }
}
