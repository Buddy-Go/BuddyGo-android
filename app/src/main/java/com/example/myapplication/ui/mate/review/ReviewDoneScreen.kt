package com.example.myapplication.ui.mate.review

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.repository.ReviewTargetInfo
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 평가 완료 ← 12c 메이트 추가 제안 있음, 12c-2 추가 대상 없음(가운데 정렬)
@Composable
fun ReviewDoneScreen(
    viewModel: ReviewViewModel,
    onGoActivity: () -> Unit, // '활동 내역으로' → 13
) {
    ReviewDoneContent(state = viewModel.state, onMateRequest = viewModel::onMateRequest, onGoActivity = onGoActivity)
}

@Composable
private fun ReviewDoneContent(
    state: ReviewState,
    onMateRequest: (String) -> Unit,
    onGoActivity: () -> Unit,
) {
    val suggestions = state.mateSuggestions
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpace.x4, vertical = AppSpace.x4),
            // 12c-2: 제안할 사람이 없으면 완료 문구만 가운데
            verticalArrangement = if (suggestions.isEmpty()) Arrangement.Center else Arrangement.Top,
        ) {
            DoneHeader(state.doneSubtitle, big = suggestions.isEmpty())
            if (suggestions.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = AppSpace.x4)) {
                    Text(text = "메이트로 추가할까요?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text(
                        text = "수락하면 서로의 모임 활동을 볼 수 있어요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColor.TextSecondary,
                        modifier = Modifier.padding(top = AppSpace.xxs, bottom = AppSpace.md),
                    )
                    suggestions.forEach { target ->
                        SuggestionRow(target = target, onRequest = { onMateRequest(target.userId) })
                    }
                }
            }
        }
        ReviewBottomButton(text = "활동 내역으로", enabled = true, onClick = onGoActivity, showTopLine = false)
    }
}

@Composable
private fun DoneHeader(subtitle: String, big: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpace.xs),
    ) {
        Box(
            modifier = Modifier
                .size(if (big) 64.dp else 44.dp)
                .background(AppColor.AccentSoft, RoundedCornerShape(AppRadius.pill)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = AppColor.AccentStrong,
                modifier = Modifier.size(if (big) 28.dp else 22.dp),
            )
        }
        Text(
            text = "평가를 모두 마쳤어요",
            fontSize = if (big) 19.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.TextPrimary,
            modifier = Modifier.padding(top = AppSpace.lg),
        )
        Text(text = subtitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextBody)
        Text(text = "남긴 평가는 상대 프로필에 반영돼요", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
    }
}

// '메이트 신청'(초록 테두리) / 보낸 뒤 '신청함'(회색)
@Composable
private fun SuggestionRow(target: ReviewTargetInfo, onRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpace.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
    ) {
        ReviewAvatar(userId = target.userId, nickname = target.nickname, size = 40.dp)
        Text(
            text = target.nickname,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        val requested = target.isMateRequested
        val shape = RoundedCornerShape(AppRadius.pill)
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(shape)
                .border(1.dp, if (requested) AppColor.Border else AppColor.Accent, shape)
                .clickable(enabled = !requested, onClick = onRequest)
                .padding(horizontal = AppSpace.xxl),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (requested) "신청함" else "메이트 신청",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (requested) AppColor.TextSecondary else AppColor.AccentStrong,
            )
        }
    }
}

// ---------- 미리보기 ----------

private val previewDone = previewReviewState.copy(
    session = previewReviewState.session?.let { s -> s.copy(targets = s.targets.map { it.copy(isReviewed = true) }) },
    isAllDone = true,
)

@Preview(name = "12c 평가 완료", widthDp = 360, heightDp = 800)
@Composable
private fun Preview12c() = ReviewDoneContent(state = previewDone, onMateRequest = {}, onGoActivity = {})

@Preview(name = "12c-2 평가 완료 · 추가 대상 없음", widthDp = 360, heightDp = 800)
@Composable
private fun Preview12c2() = ReviewDoneContent(
    state = previewDone.copy(
        session = previewDone.session?.let { s -> s.copy(targets = s.targets.map { it.copy(isMate = true) }) },
    ),
    onMateRequest = {},
    onGoActivity = {},
)
