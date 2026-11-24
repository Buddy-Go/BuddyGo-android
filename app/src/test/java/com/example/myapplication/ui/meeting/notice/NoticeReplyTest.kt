package com.example.myapplication.ui.meeting.notice

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeNoticeRepository
import com.example.myapplication.data.repository.FakeUserRepository
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
class NoticeReplyTest {
    private var vm: NoticeDetailViewModel? = null

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel() = NoticeDetailViewModel(
        MockMeetings.closed.id, "n3", FakeNoticeRepository(), FakeMeetingRepository(), FakeUserRepository(),
    ).also { vm = it }

    @Test
    fun 답글은_원댓글_바로_뒤에_붙고_원댓글에만_달_수_있다() {
        val v = viewModel()
        val parent = v.state.comments.first { it.parentId == null }
        assertTrue(v.state.canReplyTo(parent))

        v.onReplyClick(parent.id)
        assertEquals(parent.id, v.state.replyingTo?.id)
        v.onCommentInputChange("답글이에요")
        v.onSend()

        val comments = v.state.comments
        val index = comments.indexOfFirst { it.id == parent.id }
        assertEquals("답글이에요", comments[index + 1].body)
        assertEquals(parent.id, comments[index + 1].parentId)
        assertNull("보낸 뒤에는 답글 모드가 풀린다", v.state.replyingTo)
        assertEquals("", v.state.commentInput)

        // 답글에는 답글을 달 수 없다
        assertFalse(v.state.canReplyTo(comments[index + 1]))
        v.onReplyClick(comments[index + 1].id)
        assertNull(v.state.replyingTo)
    }

    @Test
    fun 답글_취소하면_일반_댓글로_돌아온다() {
        val v = viewModel()
        val parent = v.state.comments.first { it.parentId == null }
        v.onReplyClick(parent.id)
        v.onReplyCancel()
        assertNull(v.state.replyingTo)
    }
}
