package com.example.myapplication.ui.meeting.manage

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 참가자 관리 화면 ← 7a 신청 탭, 7b 확정 탭, 7c 신청 없음, 7d 마감 후. 호스트 전용.
// ViewModel은 화면을 띄우는 쪽(AppNavHost)에서 meetingId로 만들어 넘겨준다.
@Composable
fun ParticipantManageScreen(
    viewModel: ParticipantManageViewModel,
    onBack: () -> Unit,
) {
    ParticipantManageContent(
        state = viewModel.state,
        onBack = onBack,
        onTabSelect = viewModel::onTabSelect,
        onAccept = viewModel::onAccept,
        onReject = viewModel::onReject,
    )
}

@Composable
private fun ParticipantManageContent(
    state: ParticipantManageState,
    onBack: () -> Unit,
    onTabSelect: (ManageTab) -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
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
            Text(text = "참가자 관리", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xs, bottom = AppSpace.md),
            verticalArrangement = Arrangement.spacedBy(AppSpace.md),
        ) {
            if (state.showTabs) {
                SegmentTabs(
                    selected = state.visibleTab,
                    confirmedCount = state.confirmedCount,
                    applicantCount = state.applicantCount,
                    onSelect = onTabSelect,
                )
            }
            val emptyApplicants = state.visibleTab == ManageTab.APPLICANTS && state.applicants.isEmpty()
            if (emptyApplicants) {
                ApplicantsTab(
                    applicants = emptyList(),
                    processingApplicationId = null,
                    onAccept = onAccept,
                    onReject = onReject,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (state.visibleTab) {
                        ManageTab.CONFIRMED -> ConfirmedTab(members = state.confirmed, hint = state.confirmedHint)
                        ManageTab.APPLICANTS -> ApplicantsTab(
                            applicants = state.applicants,
                            processingApplicationId = state.processingApplicationId,
                            onAccept = onAccept,
                            onReject = onReject,
                        )
                    }
                }
            }
        }
    }
}

// "확정 3 | 신청 2" 세그먼트 탭
@Composable
private fun SegmentTabs(
    selected: ManageTab,
    confirmedCount: Int,
    applicantCount: Int,
    onSelect: (ManageTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppColor.ChipNeutralBg)
            .padding(3.dp),
    ) {
        SegmentTab(
            label = "확정 $confirmedCount",
            selected = selected == ManageTab.CONFIRMED,
            onClick = { onSelect(ManageTab.CONFIRMED) },
            modifier = Modifier.weight(1f),
        )
        SegmentTab(
            label = "신청 $applicantCount",
            selected = selected == ManageTab.APPLICANTS,
            onClick = { onSelect(ManageTab.APPLICANTS) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppRadius.sm)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) AppColor.Surface else Color.Transparent)
            .then(if (selected) Modifier.border(1.dp, AppColor.Border, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) AppColor.TextPrimary else AppColor.TextSecondary,
        )
    }
}

// ---------- 미리보기 ----------

@Composable
private fun PreviewContent(state: ParticipantManageState) {
    ParticipantManageContent(state = state, onBack = {}, onTabSelect = {}, onAccept = {}, onReject = {})
}

@Preview(name = "7a 신청 탭", widthDp = 360, heightDp = 800)
@Composable
private fun Preview7a() = PreviewContent(sampleState("m1"))

@Preview(name = "7b 확정 탭", widthDp = 360, heightDp = 800)
@Composable
private fun Preview7b() = PreviewContent(sampleState("m1").copy(selectedTab = ManageTab.CONFIRMED))

@Preview(name = "7c 신청 없음", widthDp = 360, heightDp = 800)
@Composable
private fun Preview7c() = PreviewContent(sampleState("m1").copy(applicants = emptyList()))

@Preview(name = "7d 마감 후", widthDp = 360, heightDp = 800)
@Composable
private fun Preview7d() = PreviewContent(sampleClosedState("m1"))
