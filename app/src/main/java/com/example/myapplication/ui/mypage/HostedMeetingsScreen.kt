package com.example.myapplication.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.explore.filter.ChevronLeftIcon
import com.example.myapplication.ui.explore.iconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.activity.ActivityViewModel
import com.example.myapplication.ui.activity.HostActivityTab
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace

// 마이페이지 '개설한 모임'을 누르면 열리는, 내가 개설한 모임 목록. 활동 화면의 호스트 탭과 같은 카드를 쓴다.
@Composable
fun HostedMeetingsScreen(
    viewModel: ActivityViewModel,
    onBack: () -> Unit,
    onMeetingClick: (meetingId: String) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { viewModel.onResume() }
    val state = viewModel.state
    Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = AppSpace.md), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).iconButton("뒤로 가기", onBack), contentAlignment = Alignment.Center) {
                ChevronLeftIcon(AppColor.TextPrimary)
            }
            Text("개설한 모임", Modifier.padding(start = AppSpace.md), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
        val error = state.loadError
        if (error != null) {
            FullScreenError(error, onRetry = viewModel::onResume)
        } else {
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(AppSpace.x4)) {
                HostActivityTab(
                    upcoming = state.hostedUpcoming,
                    past = state.hostedPast,
                    now = state.now,
                    onMeetingClick = onMeetingClick,
                    onCreateClick = onCreateClick,
                    hasAny = state.hosted.isNotEmpty(),
                )
            }
        }
    }
}
