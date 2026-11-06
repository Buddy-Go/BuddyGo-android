package com.example.myapplication.ui.mate.review

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.repository.ReviewTargetInfo
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 메이트 평가 목록 ← 12a. 활동 내역의 평가 대기 카드(13-R)나 '모임 종료 · 평가 가능' 알림에서 들어온다.
@Composable
fun ReviewListScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit,
    onStartReview: () -> Unit, // 12b로 이동 (viewModel.onStartReview를 먼저 부른 뒤)
) {
    ReviewListContent(
        state = viewModel.state,
        onBack = onBack,
        onReviewClick = { userId ->
            viewModel.onStartReview(userId)
            onStartReview()
        },
    )
}

@Composable
private fun ReviewListContent(
    state: ReviewState,
    onBack: () -> Unit,
    onReviewClick: (String) -> Unit,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ReviewTopBar(iconRes = R.drawable.ic_arrow_back, iconDescription = "뒤로 가기", onIconClick = onBack)
        val session = state.session ?: return@Column
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.md, bottom = AppSpace.x4),
        ) {
            Text(text = session.meetingTitle, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            Text(
                text = state.meetingSubtitle(timeZone),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextSecondary,
                modifier = Modifier.padding(top = AppSpace.xxs),
            )
            Box(
                Modifier
                    .padding(vertical = AppSpace.xl)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColor.DividerSoft),
            )
            Text(text = state.targetHeader, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColor.TextSecondary)
            state.targets.forEachIndexed { index, target ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
                TargetRow(target = target, onReviewClick = { onReviewClick(target.userId) })
            }
        }
    }
}

@Composable
private fun TargetRow(target: ReviewTargetInfo, onReviewClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpace.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
    ) {
        ReviewAvatar(userId = target.userId, nickname = target.nickname)
        Text(
            text = target.nickname,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (target.isReviewed) {
            Text(text = "평가 완료 ✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColor.TextDisabled)
        } else {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(AppColor.Accent)
                    .clickable(onClick = onReviewClick)
                    .padding(horizontal = AppSpace.xxl),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "평가하기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColor.OnAccent)
            }
        }
    }
}

// ---------- 미리보기 ----------

@Preview(name = "12a 메이트 평가 목록", widthDp = 360, heightDp = 800)
@Composable
private fun Preview12a() {
    ReviewListContent(state = previewReviewState, onBack = {}, onReviewClick = {}, timeZone = TimeZone.getTimeZone("Asia/Seoul"))
}
