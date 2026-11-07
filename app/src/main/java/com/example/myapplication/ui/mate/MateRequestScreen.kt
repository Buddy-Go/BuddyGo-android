package com.example.myapplication.ui.mate

import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.common.ErrorType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.MateRequest
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 받은 메이트 신청 ← 11c 신청 2건, 11c-2 빈 상태
// 메이트 탭의 배너나 '메이트 신청 받음' 알림에서 들어온다.
@Composable
fun MateRequestScreen(
    viewModel: MateRequestViewModel,
    onBack: () -> Unit,
    onProfileClick: (userId: String) -> Unit,
) {
    MateRequestContent(
        state = viewModel.state,
        onBack = onBack,
        onProfileClick = onProfileClick,
        onAccept = viewModel::onAccept,
        onReject = viewModel::onReject,
        onRetry = viewModel::load,
    )
}

@Composable
private fun MateRequestContent(
    state: MateRequestState,
    onBack: () -> Unit,
    onProfileClick: (String) -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AppSpace.xxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "뒤로 가기",
                tint = AppColor.TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBack),
            )
            Text(text = state.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
        }

        if (state.loadFailed) {
            FullScreenError(type = ErrorType.SERVER, onRetry = onRetry, modifier = Modifier.weight(1f))
            return@Column
        }

        if (!state.isLoading && state.requests.isEmpty()) {
            // 11c-2
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "받은 신청이 없어요", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColor.TextDisabled)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.md, bottom = AppSpace.x4),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xl),
        ) {
            state.requests.forEach { request ->
                RequestCard(
                    request = request,
                    enabled = state.processingRequestId == null,
                    onProfileClick = { onProfileClick(request.userId) },
                    onAccept = { onAccept(request.requestId) },
                    onReject = { onReject(request.requestId) },
                )
            }
            Text(
                text = "수락하면 서로의 모임 활동을 볼 수 있어요",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpace.md),
            )
        }
    }
}

@Composable
private fun RequestCard(
    request: MateRequest,
    enabled: Boolean,
    onProfileClick: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColor.Border, shape)
            .padding(AppSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpace.lg),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onProfileClick),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
        ) {
            MateAvatar(userId = request.userId, nickname = request.nickname, size = 38.dp, fontSize = 14.sp)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxs)) {
                Text(text = request.nickname, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                MannerLine(text = request.infoText())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            DecisionButton(text = "거절", background = AppColor.Surface, textColor = AppColor.TextPrimary, bordered = true, enabled = enabled, onClick = onReject, modifier = Modifier.weight(1f))
            DecisionButton(text = "수락", background = AppColor.Accent, textColor = AppColor.OnAccent, bordered = false, enabled = enabled, onClick = onAccept, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DecisionButton(
    text: String,
    background: Color,
    textColor: Color,
    bordered: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(background)
            .then(if (bordered) Modifier.border(1.dp, AppColor.Border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

// ---------- 미리보기 ----------

@Preview(name = "11c 메이트 신청 2건", widthDp = 360, heightDp = 800)
@Composable
private fun Preview11c() = MateRequestContent(
    state = MateRequestState(
        requests = listOf(
            MateRequest("q1", "mt5", "준호", 42.5f, 18),
            MateRequest("q2", "mt4", "수아", 39.8f, 9),
        ),
        isLoading = false,
    ),
    onBack = {},
    onProfileClick = {},
    onAccept = {},
    onReject = {},
)

@Preview(name = "11c-2 메이트 신청 빈 상태", widthDp = 360, heightDp = 800)
@Composable
private fun Preview11c2() = MateRequestContent(
    state = MateRequestState(isLoading = false),
    onBack = {},
    onProfileClick = {},
    onAccept = {},
    onReject = {},
)
