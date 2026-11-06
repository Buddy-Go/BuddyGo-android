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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoticeDetailViewModelTest {
    private var vm: NoticeDetailViewModel? = null

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun viewModel(noticeId: String) = NoticeDetailViewModel(
        MockMeetings.closed.id, noticeId, FakeNoticeRepository(), FakeMeetingRepository(), FakeUserRepository(),
    ).also { vm = it }

    @Test
    fun 있는_공지는_그대로_보여_준다() {
        val v = viewModel("n3")
        assertEquals("스쿼트 랙 예약했어요", v.state.notice?.title)
        assertFalse(v.state.loadFailed)
    }

    @Test
    fun 없는_공지는_빈_화면_대신_불러오기_실패_상태가_된다() {
        val v = viewModel("없는-공지")
        assertTrue(v.state.loadFailed)
        assertEquals(null, v.state.notice)
    }
}
