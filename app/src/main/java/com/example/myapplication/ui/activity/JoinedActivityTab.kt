package com.example.myapplication.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 참여 탭 ← 13-P 모임 목록(필터 없음), 13-P2 빈 상태
@Composable
fun JoinedActivityTab(
    upcoming: List<JoinedMeetingItem>,
    past: List<JoinedMeetingItem>,
    onMeetingClick: (meetingId: String) -> Unit,
    onExploreClick: () -> Unit, // 13-P2 '모임 찾아보기' → 탐색
    timeZone: TimeZone = TimeZone.getDefault(),
    hasAny: Boolean = true,
) {
    if (upcoming.isEmpty() && past.isEmpty() && hasAny) {
        FilteredEmpty()
        return
    }
    if (upcoming.isEmpty() && past.isEmpty()) {
        ActivityEmptyState(message = "아직 신청한 모임이 없어요", buttonText = "모임 찾아보기", onClick = onExploreClick)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
        if (upcoming.isNotEmpty()) {
            ActivitySectionLabel("예정 ${upcoming.size}")
            upcoming.forEach { JoinedCard(it, isPast = false, onMeetingClick, timeZone) }
        }
        if (past.isNotEmpty()) {
            ActivitySectionLabel("지난 모임")
            past.forEach { JoinedCard(it, isPast = true, onMeetingClick, timeZone) }
        }
    }
}

@Composable
private fun JoinedCard(item: JoinedMeetingItem, isPast: Boolean, onMeetingClick: (String) -> Unit, timeZone: TimeZone) {
    ActivityMeetingCard(
        startAt = item.startAt,
        isPast = isPast,
        title = item.title,
        meta = item.metaText(timeZone),
        onClick = { onMeetingClick(item.meetingId) },
        timeZone = timeZone,
    ) {
        SportTag(item.sport, large = false)
        // 참여 확정·완료는 회색, 노쇼는 빨강 (13-P)
        if (item.status == JoinedStatus.NO_SHOW) {
            StatusChip(item.status.label, AppColor.DestructiveSoftBg, AppColor.Danger, large = false)
        } else {
            StatusChip(item.status.label, AppColor.ChipNeutralBg, AppColor.TextBody, large = false)
        }
    }
}
