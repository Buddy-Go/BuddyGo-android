package com.example.myapplication.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.api.Api
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// 앱을 보고 있는 동안 새 알림이 오면 화면 위에서 내려오는 배너로 알려 준다. (예: 모임에 신청한 사람이 생기면 호스트에게)
// 서버가 푸시(FCM)를 보내기 전에도, 푸시가 와도 앱 안에서는 같은 방식으로 보이도록 알림 목록을 일정 간격으로 확인한다.
// 처음 확인한 목록은 '이미 있던 알림'으로 보고 배너를 띄우지 않는다.
object NotificationWatcher {
    private const val POLL_MILLIS = 15_000L

    private val _banner = MutableStateFlow<Notification?>(null)
    val banner: StateFlow<Notification?> = _banner.asStateFlow()

    // 배너를 눌러 알림이 가리키는 화면으로 가자는 요청 (이동은 화면 틀이 맡는다)
    private val _open = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val open: SharedFlow<String> = _open.asSharedFlow()

    private val seen = mutableSetOf<String>()
    private var seeded = false

    // 푸시가 왔을 때 15초를 기다리지 않고 바로 확인해 달라는 요청. 여러 번 와도 한 번만 처리한다.
    private val pollNow = Channel<Unit>(Channel.CONFLATED)
    fun requestPoll() { pollNow.trySend(Unit) }

    // 화면이 보이는 동안만 돈다(repeatOnLifecycle). 취소되면 멈춘다.
    suspend fun run(repository: NotificationRepository) {
        while (true) {
            if (Api.isInitialized && Api.tokens.hasSession) poll(repository)
            // 일정 간격이 지나거나, 푸시가 와서 requestPoll()이 불리면 다음 확인으로 간다.
            withTimeoutOrNull(POLL_MILLIS) { pollNow.receive() }
        }
    }

    private suspend fun poll(repository: NotificationRepository) {
        val list = repository.getNotifications().getOrNull() ?: return
        if (!seeded) {
            seen += list.map { it.id }
            seeded = true
            return
        }
        val fresh = list.filter { !it.isRead && it.id !in seen }
        seen += list.map { it.id }
        // 여러 개가 한꺼번에 왔으면 가장 최근 것 하나만 보여 준다.
        fresh.maxByOrNull { it.createdAtMillis }?.let { _banner.value = it }
    }

    fun dismiss() { _banner.value = null }

    fun onBannerClick(notification: Notification) {
        _banner.value = null
        notification.destination()?.let { _open.tryEmit(it) }
    }

    // 로그아웃·세션 만료: 다음 사람에게 앞사람의 알림이 배너로 뜨지 않게 지운다.
    fun reset() {
        _banner.value = null
        seen.clear()
        seeded = false
    }
}

// 화면 맨 위에 겹쳐 그린다. 4초 뒤에 저절로 사라지고, 누르면 알림이 가리키는 화면으로 간다.
@Composable
fun InAppBannerHost(modifier: Modifier = Modifier) {
    val current by NotificationWatcher.banner.collectAsState()
    // 사라지는 동안에도 마지막 알림 문구를 보여 주려고 기억해 둔다.
    var last by remember { mutableStateOf<Notification?>(null) }
    current?.let { last = it }
    LaunchedEffect(current) {
        if (current != null) {
            delay(4_000L)
            NotificationWatcher.dismiss()
        }
    }
    AnimatedVisibility(
        visible = current != null,
        modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        last?.let { n -> BannerCard(n, onClick = { NotificationWatcher.onBannerClick(n) }) }
    }
}

@Composable
private fun BannerCard(notification: Notification, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Box(Modifier.statusBarsPadding().padding(horizontal = AppSpace.xl, vertical = AppSpace.md)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape)
                .clip(shape)
                .background(AppColor.Surface)
                .clickable(onClickLabel = "알림 열기", onClick = onClick)
                .padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("BuddyGo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColor.AccentStrong)
            Text(
                text = joinSubject(notification.subject, notification.message),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
