package com.example.myapplication.ui.notification

import com.example.myapplication.data.mock.MockNotifications
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.repository.FakeNotificationRepository
import com.example.myapplication.data.repository.NotificationRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import androidx.lifecycle.viewModelScope

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
    private var vm: NotificationViewModel? = null

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun list(v: NotificationViewModel) = (v.state.value as UiState.Success).data

    @Test
    fun 시작하면_알림을_불러온다() {
        val v = NotificationViewModel(FakeNotificationRepository()).also { vm = it }
        assertEquals(MockNotifications.all.size, list(v).size)
    }

    @Test
    fun 알림을_누르면_그_알림만_읽음이_되고_저장소에도_반영된다() {
        val repo = FakeNotificationRepository()
        val v = NotificationViewModel(repo).also { vm = it }
        v.onNotificationClick(MockNotifications.unreadNotice)
        assertTrue(list(v).first { it.id == "n2" }.isRead)
        assertTrue("다른 알림은 그대로", !list(v).first { it.id == "n1" }.isRead)
        v.load()
        assertTrue(list(v).first { it.id == "n2" }.isRead)
    }

    @Test
    fun 모두_읽음을_누르면_전부_읽음이_된다() {
        val v = NotificationViewModel(FakeNotificationRepository()).also { vm = it }
        v.onMarkAllRead()
        assertTrue(list(v).all { it.isRead })
        v.load()
        assertTrue(list(v).all { it.isRead })
    }

    @Test
    fun 네트워크_오류와_서버_오류를_구분한다() {
        fun failing(e: Throwable) = object : NotificationRepository {
            override suspend fun getNotifications() = Result.failure<List<Notification>>(e)
            override suspend fun markAsRead(notificationId: String) = Result.success(Unit)
            override suspend fun markAllAsRead() = Result.success(Unit)
        }
        val a = NotificationViewModel(failing(IOException())).also { vm = it }
        assertEquals(ErrorType.NETWORK, (a.state.value as UiState.Error).type)
        a.viewModelScope.cancel()
        val b = NotificationViewModel(failing(IllegalStateException())).also { vm = it }
        assertEquals(ErrorType.SERVER, (b.state.value as UiState.Error).type)
    }
}
