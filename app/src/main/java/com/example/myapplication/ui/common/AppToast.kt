package com.example.myapplication.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

enum class ToastType { Success, Warning, Error }

// 16j 토스트 한 개의 모양. 보여 주고 숨기는 것은 ToastState + ToastHost가 맡는다.
@Composable
fun AppToast(message: String, type: ToastType, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(AppColor.Surface)
            .padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToastIcon(type)
        Spacer(Modifier.width(AppSpace.xl))
        Text(message, color = AppColor.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ToastIcon(type: ToastType) {
    val bg = when (type) {
        ToastType.Success -> AppColor.AccentSoft
        ToastType.Warning -> AppColor.ChipNeutralBg
        ToastType.Error -> AppColor.DestructiveSoftBg
    }
    Box(Modifier.size(28.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        when (type) {
            ToastType.Success -> Canvas(Modifier.size(14.dp)) {
                val sw = 2.dp.toPx()
                val c = AppColor.AccentStrong
                drawLine(c, Offset(size.width * .1f, size.height * .55f), Offset(size.width * .4f, size.height * .85f), sw, StrokeCap.Round)
                drawLine(c, Offset(size.width * .4f, size.height * .85f), Offset(size.width * .92f, size.height * .2f), sw, StrokeCap.Round)
            }
            ToastType.Warning -> ExclamationIcon(AppColor.TextPrimary, size = 16.dp)
            ToastType.Error -> Canvas(Modifier.size(10.dp)) {
                val sw = 2.dp.toPx()
                val c = AppColor.DestructiveText
                drawLine(c, Offset(0f, 0f), Offset(size.width, size.height), sw, StrokeCap.Round)
                drawLine(c, Offset(size.width, 0f), Offset(0f, size.height), sw, StrokeCap.Round)
            }
        }
    }
}

// 화면이 들고 있는 토스트 상태. 코루틴 안에서 show()를 부르면 잠시 보였다가 사라진다.
//   val toast = remember { ToastState() }
//   scope.launch { toast.show("신청이 확정됐어요", ToastType.Success) }
//   Box { ...화면...; ToastHost(toast, Modifier.align(Alignment.BottomCenter)) }
@Stable
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var type by mutableStateOf(ToastType.Success)
        private set

    suspend fun show(message: String, type: ToastType, durationMillis: Long = 2500) {
        this.message = message
        this.type = type
        delay(durationMillis)
        this.message = null
    }
}

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    // 사라지는 동안에도 마지막 문구를 보여 주려고 마지막 값을 기억해 둔다.
    var lastMessage by remember { mutableStateOf("") }
    state.message?.let { lastMessage = it }
    AnimatedVisibility(
        visible = state.message != null,
        modifier = modifier.padding(horizontal = AppSpace.xxl, vertical = AppSpace.xxl),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        AppToast(lastMessage, state.type)
    }
}

@Preview(showBackground = true, widthDp = 390, backgroundColor = 0xFFF7F7F8)
@Composable
private fun AppToastPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Column(Modifier.padding(AppSpace.xxl), verticalArrangement = Arrangement.spacedBy(AppSpace.x4)) {
            AppToast("신청이 확정됐어요", ToastType.Success)
            AppToast("마감까지 5시간 남았어요", ToastType.Warning)
            AppToast("요청을 처리하지 못했어요", ToastType.Error)
        }
    }
}
