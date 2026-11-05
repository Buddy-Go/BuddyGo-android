package com.example.myapplication.ui.mate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.MateActivity
import com.example.myapplication.data.model.MateActivityKind
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.data.model.MateSummary
import com.example.myapplication.data.model.WorkedOutPerson
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 메이트 화면 (하단 탭 '메이트') ← 제목 + 신청 배너 + 탭 바 (11a, 11a-2, 11b)
// 하단 탭 바(BottomNavBar, 공용)는 화면 바깥 틀에서 붙인다고 보고 여기서는 그리지 않는다.
@Composable
fun MateScreen(
    viewModel: MateViewModel,
    onRequestsClick: () -> Unit, // 배너 → 11c
    onProfileClick: (userId: String) -> Unit, // → 15
) {
    LaunchedEffect(Unit) { viewModel.refresh() }
    MateContent(
        state = viewModel.state,
        onTabSelect = viewModel::onTabSelect,
        onSearchChange = viewModel::onSearchChange,
        onRequestsClick = onRequestsClick,
        onProfileClick = onProfileClick,
        onSendRequest = viewModel::onSendRequest,
    )
}

@Composable
private fun MateContent(
    state: MateState,
    onTabSelect: (MateTab) -> Unit,
    onSearchChange: (String) -> Unit,
    onRequestsClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Box(
            modifier = Modifier
                .height(52.dp)
                .padding(horizontal = AppSpace.x4),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = "메이트", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }

        state.bannerText?.let { text ->
            Row(
                modifier = Modifier
                    .padding(start = AppSpace.x4, end = AppSpace.x4, bottom = AppSpace.lg)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadius.lg))
                    .background(AppColor.SurfaceMuted)
                    .clickable(onClick = onRequestsClick)
                    .padding(horizontal = AppSpace.xxl, vertical = AppSpace.xxl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = "메이트 신청 보기",
                    tint = AppColor.IconMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // 탭 바 (초록 밑줄)
        Column {
            Row(
                modifier = Modifier.padding(horizontal = AppSpace.x4),
                horizontalArrangement = Arrangement.spacedBy(AppSpace.x5),
            ) {
                AccentTab("메이트", state.selectedTab == MateTab.MATES) { onTabSelect(MateTab.MATES) }
                AccentTab("함께 운동한 사람", state.selectedTab == MateTab.WORKED_OUT) { onTabSelect(MateTab.WORKED_OUT) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
        }

        when (state.selectedTab) {
            MateTab.MATES -> MateListTab(
                mates = state.mates,
                now = state.now,
                onMateClick = onProfileClick,
                modifier = Modifier.weight(1f),
                timeZone = timeZone,
            )
            MateTab.WORKED_OUT -> WorkedOutTab(
                people = state.filteredWorkedOut,
                emptyText = state.workedOutEmptyText,
                searchQuery = state.searchQuery,
                onSearchChange = onSearchChange,
                onPersonClick = onProfileClick,
                onRequest = onSendRequest,
                modifier = Modifier.weight(1f),
                timeZone = timeZone,
            )
        }
    }
}

@Composable
private fun AccentTab(label: String, selected: Boolean, onClick: () -> Unit) {
    var widthDp by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) AppColor.AccentStrong else AppColor.TextSecondary,
            modifier = Modifier
                .padding(top = AppSpace.xxl)
                .onSizeChanged { widthDp = with(density) { it.width.toDp() } },
        )
        Box(Modifier.width(widthDp).height(2.dp).background(if (selected) AppColor.Accent else Color.Transparent))
    }
}

// ---------- 미리보기 ----------

private const val PREVIEW_NOW = 1788566400000L // 2026-09-05(토) 09:00 (서울)
private const val HOUR = 3_600_000L
private const val DAY = 24 * HOUR

internal val previewMates = listOf(
    MateSummary("m1", "민준", MateActivity("a1", MateActivityKind.EXERCISING_NOW, "아침 가슴 운동", "스포애니 합정점", PREVIEW_NOW - HOUR)),
    MateSummary("m2", "지훈", MateActivity("a2", MateActivityKind.HOSTING, "퇴근 후 등·이두 같이 치실 분", "에이블짐 망원점", PREVIEW_NOW + 11 * HOUR)),
    MateSummary("m4", "태현", MateActivity("a3", MateActivityKind.JOINED, "강남 스포애니", "강남 스포애니", PREVIEW_NOW + DAY + 5 * HOUR)),
    MateSummary("m3", "하늘", null),
    MateSummary("m5", "예진", null),
)

private val previewWorkedOut = listOf(
    WorkedOutPerson("w1", "서연", PREVIEW_NOW - 1 * DAY, MateRelation.NONE),
    WorkedOutPerson("w2", "우진", PREVIEW_NOW - 1 * DAY, MateRelation.REQUESTED),
    WorkedOutPerson("m1", "민준", PREVIEW_NOW - 5 * DAY, MateRelation.MATE),
    WorkedOutPerson("w3", "태윤", PREVIEW_NOW - 16 * DAY, MateRelation.NONE),
)

private val previewState = MateState(
    mates = previewMates,
    workedOut = previewWorkedOut,
    pendingRequestCount = 2,
    isLoading = false,
    now = PREVIEW_NOW,
)

@Composable
private fun PreviewContent(state: MateState) {
    MateContent(
        state = state,
        onTabSelect = {},
        onSearchChange = {},
        onRequestsClick = {},
        onProfileClick = {},
        onSendRequest = {},
        timeZone = TimeZone.getTimeZone("Asia/Seoul"),
    )
}

@Preview(name = "11a 메이트 탭", widthDp = 360, heightDp = 800)
@Composable
private fun Preview11a() = PreviewContent(previewState)

@Preview(name = "11a-2 메이트 빈 상태", widthDp = 360, heightDp = 800)
@Composable
private fun Preview11a2() = PreviewContent(MateState(isLoading = false, now = PREVIEW_NOW))

@Preview(name = "11b 함께 운동한 사람 탭", widthDp = 360, heightDp = 800)
@Composable
private fun Preview11b() = PreviewContent(previewState.copy(selectedTab = MateTab.WORKED_OUT))
