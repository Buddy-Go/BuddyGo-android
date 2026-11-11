package com.example.myapplication.ui.auth

import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.local.InMemoryKeyValueStore
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.data.repository.FakeMateRepository
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.MateRepository
import com.example.myapplication.data.repository.RealMateRepository
import com.example.myapplication.data.repository.RealUserRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.ui.mate.MateProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 서버가 지원하지 않는 기능(비밀번호 찾기, 신고)을 '됐다'고 알리지 않는지 확인한다.
@OptIn(ExperimentalCoroutinesApi::class)
class UnsupportedFeaturesTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val client = ApiClient("http://localhost:1/", TokenStore(InMemoryKeyValueStore()), onSessionExpired = {})
    private val tokens = TokenStore(InMemoryKeyValueStore())
    private fun realUsers() = RealUserRepository(client.authApi, client.userApi, tokens, client.reviewApi, FakeUserRepository())

    // 서버가 신고를 받지 않는 저장소 (목 저장소를 감싸서 신고만 끈다)
    private class NoReportMates(private val inner: MateRepository = FakeMateRepository()) : MateRepository by inner {
        override val supportsReport: Boolean get() = false
    }

    @Test
    fun 서버_저장소는_비밀번호_찾기를_지원하지_않는다고_답한다() = runTest {
        val users = realUsers()

        assertFalse(users.supportsPasswordReset)
        assertTrue(users.requestPasswordReset("a@b.com").exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun 지원하지_않으면_비밀번호_찾기_화면은_준비_중으로_시작한다() {
        val unsupported = object : UserRepository by FakeUserRepository() { override val supportsPasswordReset = false }

        assertFalse(FindPasswordViewModel(unsupported).state.available)
        assertTrue(FindPasswordViewModel(FakeUserRepository()).state.available)
    }

    @Test
    fun 준비_중이면_메일을_보냈다고_하지_않는다() = runTest {
        val vm = FindPasswordViewModel(realUsers())
        vm.onEmailChange("a@b.com")

        vm.onSend()

        assertFalse(vm.state.sent)
    }

    @Test
    fun 서버_저장소는_신고를_지원하지_않는다고_답한다() {
        val mates = RealMateRepository(client.buddyApi, client.userApi, client.meetingApi, client.reviewApi)

        assertFalse(mates.supportsReport)
        assertTrue(FakeMateRepository().supportsReport)
    }

    @Test
    fun 신고를_못_받으면_메뉴를_숨기고_접수됐다고_하지_않는다() {
        val vm = MateProfileViewModel("u2", NoReportMates())

        assertFalse(vm.state.canReport)
        vm.onReport("스팸")
        assertNull(vm.state.reportedMessage)
    }

    @Test
    fun 신고를_받는_저장소면_기존처럼_접수_안내를_한다() {
        val vm = MateProfileViewModel("u2", FakeMateRepository())

        assertTrue(vm.state.canReport)
        vm.onReport("스팸")
        assertEquals("신고가 접수됐어요", vm.state.reportedMessage)
    }
}
