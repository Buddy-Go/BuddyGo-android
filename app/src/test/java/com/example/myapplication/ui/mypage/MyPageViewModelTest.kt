package com.example.myapplication.ui.mypage

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.repository.FakeUserRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {
    private var vm: MyPageViewModel? = null

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun data(v: MyPageViewModel) = (v.state.value as UiState.Success).data

    @Test
    fun 시작하면_내_정보와_숫자와_후기와_알림_설정을_불러온다() {
        val v = MyPageViewModel(FakeUserRepository()).also { vm = it }
        assertEquals(MockUsers.me.id, data(v).user.id)
        assertEquals(32, data(v).stats.receivedReviewCount)
        assertTrue(data(v).reviews.isNotEmpty())
        assertTrue(data(v).notificationEnabled)
    }

    @Test
    fun 알림_설정을_누르면_껐다_켜고_저장소에도_남는다() {
        val repo = FakeUserRepository()
        val v = MyPageViewModel(repo).also { vm = it }
        v.onToggleNotification()
        assertFalse(data(v).notificationEnabled)
        v.load()
        assertFalse(data(v).notificationEnabled)
        v.onToggleNotification()
        assertTrue(data(v).notificationEnabled)
    }

    @Test
    fun 로그아웃에_성공하면_완료를_알린다() {
        val v = MyPageViewModel(FakeUserRepository()).also { vm = it }
        var done = false
        v.onLogout { done = true }
        assertTrue(done)
    }

    @Test
    fun 불러오기_실패는_오류_종류를_구분한다() {
        val repo = object : com.example.myapplication.data.repository.UserRepository by FakeUserRepository() {
            override suspend fun getCurrentUser() = Result.failure<com.example.myapplication.data.model.User>(IOException())
        }
        val v = MyPageViewModel(repo).also { vm = it }
        assertEquals(ErrorType.NETWORK, (v.state.value as UiState.Error).type)
    }
}
