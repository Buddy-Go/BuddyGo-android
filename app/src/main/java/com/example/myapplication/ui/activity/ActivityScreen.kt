package com.example.myapplication.ui.activity

import com.example.myapplication.data.model.Sport
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.example.myapplication.ui.common.FullScreenError
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 활동 내역 화면 ← 13-H, 13-H2, 13-P, 13-P2, 13-R (하단 탭 '활동')
// 하단 탭 바(BottomNavBar, 공용)는 화면 바깥 틀에서 붙인다고 보고 여기서는 그리지 않는다.
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onReviewClick: (meetingId: String) -> Unit, // 13-R → 12a 평가 목록
    onMeetingClick: (meetingId: String) -> Unit, // 카드 → 5번 모임 상세
    onCreateClick: () -> Unit, // 13-H2 → 10a
    onExploreClick: () -> Unit, // 13-P2 → 3번 탐색
) {
    LaunchedEffect(Unit) { viewModel.onResume() }
    viewModel.state.loadError?.let { error ->
        FullScreenError(type = error, onRetry = viewModel::onResume, showHome = false)
        return
    }
    ActivityContent(
        state = viewModel.state,
        onTabSelect = viewModel::onTabSelect,
        onSportFilterSelect = viewModel::onSportFilterSelect,
        onReviewClick = onReviewClick,
        onMeetingClick = onMeetingClick,
        onCreateClick = onCreateClick,
        onExploreClick = onExploreClick,
    )
}

@Composable
private fun ActivityContent(
    state: ActivityState,
    onTabSelect: (ActivityTab) -> Unit,
    onSportFilterSelect: (Sport?) -> Unit = {},
    onReviewClick: (String) -> Unit,
    onMeetingClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onExploreClick: () -> Unit,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding(),
    ) {
        // 제목 + 통계
        Column(modifier = Modifier.padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xxl)) {
            Text(text = "활동", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            Text(
                text = state.statsText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextSecondary,
                modifier = Modifier.padding(top = AppSpace.xs, bottom = AppSpace.xxl),
            )
        }

        // 평가 대기 (있을 때만)
        if (state.reviewPending.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = AppSpace.x4, end = AppSpace.x4, bottom = AppSpace.xxl),
                verticalArrangement = Arrangement.spacedBy(AppSpace.lg),
            ) {
                ActivitySectionLabel(state.reviewHeader)
                state.reviewPending.forEach { item ->
                    ReviewPendingCard(item = item, now = state.now, onClick = { onReviewClick(item.meetingId) }, timeZone = timeZone)
                }
            }
        }

        UnderlineTabs(
            selected = state.selectedTab,
            hostLabel = state.hostTabLabel,
            joinedLabel = state.joinedTabLabel,
            onSelect = onTabSelect,
        )

        val isEmpty = when (state.selectedTab) {
            ActivityTab.HOST -> state.hosted.isEmpty()
            ActivityTab.JOINED -> state.joined.isEmpty()
        }
        // 13-H 종목 필터는 모임이 4개 이상일 때만 (13-P 3건에서는 숨김)
        if (state.showSportFilter(state.selectedTab)) {
            Box(Modifier.padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xxl)) {
                ActivitySportFilter(selected = state.sportFilter, onSelect = onSportFilterSelect)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (isEmpty) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xxl, bottom = AppSpace.xxl),
        ) {
            when (state.selectedTab) {
                ActivityTab.HOST -> HostActivityTab(
                    upcoming = state.hostedUpcoming,
                    past = state.hostedPast,
                    now = state.now,
                    onMeetingClick = onMeetingClick,
                    onCreateClick = onCreateClick,
                    timeZone = timeZone,
                    hasAny = state.hosted.isNotEmpty(),
                )
                ActivityTab.JOINED -> JoinedActivityTab(
                    upcoming = state.joinedUpcoming,
                    past = state.joinedPast,
                    onMeetingClick = onMeetingClick,
                    onExploreClick = onExploreClick,
                    timeZone = timeZone,
                    hasAny = state.joined.isNotEmpty(),
                )
            }
        }
    }
}

// '호스트 2 | 참여 3' 밑줄 탭
@Composable
private fun UnderlineTabs(
    selected: ActivityTab,
    hostLabel: String,
    joinedLabel: String,
    onSelect: (ActivityTab) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.padding(horizontal = AppSpace.x4),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.x4),
        ) {
            UnderlineTab(hostLabel, selected == ActivityTab.HOST) { onSelect(ActivityTab.HOST) }
            UnderlineTab(joinedLabel, selected == ActivityTab.JOINED) { onSelect(ActivityTab.JOINED) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
    }
}

@Composable
private fun UnderlineTab(label: String, selected: Boolean, onClick: () -> Unit) {
    var textWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) AppColor.TextPrimary else AppColor.TextSecondary,
            modifier = Modifier
                .padding(start = AppSpace.xxs, end = AppSpace.xxs, bottom = AppSpace.lg)
                .onSizeChanged { textWidth = with(density) { it.width.toDp() } },
        )
        UnderlineBar(width = textWidth + AppSpace.xxs * 2, color = if (selected) AppColor.TextPrimary else Color.Transparent)
    }
}

@Composable
private fun UnderlineBar(width: Dp, color: Color) {
    Box(Modifier.width(width).height(2.dp).background(color))
}

// ---------- 미리보기 ----------

// 미리보기는 시간이 고정돼야 해서 now를 고정값으로 둔다.
private const val PREVIEW_NOW = 1788566400000L // 2026-09-05 09:00 (서울)

@Composable
private fun PreviewContent(state: ActivityState) {
    ActivityContent(
        state = state,
        onTabSelect = {},
        onReviewClick = {},
        onMeetingClick = {},
        onCreateClick = {},
        onExploreClick = {},
        timeZone = TimeZone.getTimeZone("Asia/Seoul"),
    )
}

@Preview(name = "13-H 호스트 · 모임 목록", widthDp = 360, heightDp = 1000)
@Composable
private fun Preview13H() = PreviewContent(sampleActivityState(PREVIEW_NOW))

@Preview(name = "13-H2 호스트 · 빈 상태", widthDp = 360, heightDp = 800)
@Composable
private fun Preview13H2() = PreviewContent(sampleActivityState(PREVIEW_NOW).copy(hosted = emptyList(), reviewPending = emptyList()))

@Preview(name = "13-P 참여 · 모임 목록", widthDp = 360, heightDp = 800)
@Composable
private fun Preview13P() = PreviewContent(sampleActivityState(PREVIEW_NOW).copy(selectedTab = ActivityTab.JOINED))

@Preview(name = "13-P2 참여 · 빈 상태", widthDp = 360, heightDp = 800)
@Composable
private fun Preview13P2() = PreviewContent(
    sampleActivityState(PREVIEW_NOW).copy(selectedTab = ActivityTab.JOINED, joined = emptyList(), reviewPending = emptyList()),
)
