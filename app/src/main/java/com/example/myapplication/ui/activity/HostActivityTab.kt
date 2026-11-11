package com.example.myapplication.ui.activity

import com.example.myapplication.ui.common.sportLabel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.SportSupport
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 호스트 탭 ← 13-H 모임 목록, 13-H2 빈 상태
@Composable
fun HostActivityTab(
    upcoming: List<HostedMeetingItem>,
    past: List<HostedMeetingItem>,
    now: Long,
    onMeetingClick: (meetingId: String) -> Unit,
    onCreateClick: () -> Unit, // 13-H2 '모임 개설하기' → 10a
    timeZone: TimeZone = TimeZone.getDefault(),
    hasAny: Boolean = true, // 종목 필터를 빼고도 모임이 하나도 없으면 false
) {
    if (upcoming.isEmpty() && past.isEmpty() && hasAny) {
        FilteredEmpty()
        return
    }
    if (upcoming.isEmpty() && past.isEmpty()) {
        ActivityEmptyState(message = "아직 개설한 모임이 없어요", buttonText = "모임 개설하기", onClick = onCreateClick)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
        if (upcoming.isNotEmpty()) {
            ActivitySectionLabel("예정 ${upcoming.size}")
            upcoming.forEach { item ->
                ActivityMeetingCard(
                    startAt = item.startAt,
                    isPast = false,
                    title = item.title,
                    meta = item.metaText(now, timeZone),
                    onClick = { onMeetingClick(item.meetingId) },
                    timeZone = timeZone,
                ) {
                    SportTag(item.sport, large = true)
                    HostedStatusChip(item)
                    if (item.status == HostedStatus.RECRUITING && item.pendingApplicantCount > 0) {
                        Box(Modifier.weight(1f))
                        ApplicantDot(item.pendingApplicantCount)
                    }
                }
            }
        }
        if (past.isNotEmpty()) {
            ActivitySectionLabel("지난 모임")
            past.forEach { item ->
                ActivityMeetingCard(
                    startAt = item.startAt,
                    isPast = true,
                    title = item.title,
                    meta = item.metaText(now, timeZone),
                    onClick = { onMeetingClick(item.meetingId) },
                    timeZone = timeZone,
                ) {
                    SportTag(item.sport, large = true)
                    HostedStatusChip(item)
                }
            }
        }
    }
}

// 모집중 n/m(초록) · 마감 n/m(검정) · 진행중 · 종료 · 취소(회색)
@Composable
private fun HostedStatusChip(item: HostedMeetingItem) {
    val count = "${item.confirmedCount}/${item.capacity}"
    when (item.status) {
        HostedStatus.RECRUITING -> StatusChip("모집중 $count", AppColor.Accent, AppColor.OnAccent, large = true)
        HostedStatus.CLOSED -> StatusChip("마감 $count", AppColor.TextPrimary, AppColor.Surface, large = true)
        HostedStatus.IN_PROGRESS -> StatusChip("진행중", AppColor.AccentSoft, AppColor.AccentStrong, large = true)
        HostedStatus.ENDED -> StatusChip("종료", AppColor.ChipNeutralBg, AppColor.TextSecondary, large = true)
        HostedStatus.CANCELLED -> StatusChip("취소", AppColor.ChipNeutralBg, AppColor.TextSecondary, large = true)
    }
}

// 카드의 종목 칩 (13-H·13-P): 옅은 라임 바탕에 초록 글자
@Composable
internal fun SportTag(sport: Sport, large: Boolean) {
    StatusChip(sportLabel(sport), AppColor.AccentSoft, AppColor.AccentStrong, large)
}

// 종목 필터로 거른 결과가 비었을 때
@Composable
internal fun FilteredEmpty() {
    Box(Modifier.fillMaxWidth().padding(vertical = AppSpace.x5), contentAlignment = Alignment.Center) {
        Text(text = "이 종목의 모임이 없어요", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextDisabled)
    }
}

// 13-H 종목 필터: 전체 + 서버가 받는 종목. 고른 칸은 라임 알약, 나머지는 테두리 알약.
// 서버가 받는 종목이 하나뿐이면 거를 게 없어 아무것도 그리지 않는다.
@Composable
internal fun ActivitySportFilter(selected: Sport?, onSelect: (Sport?) -> Unit) {
    if (!SportSupport.hasChoice) return
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        (listOf<Sport?>(null) + SportSupport.supported).forEach { sport ->
            val isSelected = sport == selected
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(if (isSelected) AppColor.Accent else AppColor.Surface)
                    .then(if (isSelected) Modifier else Modifier.border(1.dp, AppColor.Border, RoundedCornerShape(AppRadius.pill)))
                    .clickable { onSelect(sport) }
                    .padding(horizontal = AppSpace.x4),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (sport == null) "전체" else sportLabel(sport),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AppColor.OnAccent else AppColor.TextBody,
                )
            }
        }
    }
}

// '● 신청자 2'
@Composable
private fun ApplicantDot(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(AppColor.Positive, RoundedCornerShape(AppRadius.pill)),
        )
        Text(text = "신청자 $count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColor.Positive)
    }
}

// ---------- 호스트·참여 탭이 같이 쓰는 부품 ----------

@Composable
internal fun ActivitySectionLabel(text: String) {
    Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColor.TextSecondary)
}

// 왼쪽 날짜 상자 + 오른쪽 칩·제목·한 줄 정보. 지난 모임은 흐리게(60%)
@Composable
internal fun ActivityMeetingCard(
    startAt: Long,
    isPast: Boolean,
    title: String,
    meta: String,
    onClick: () -> Unit,
    timeZone: TimeZone,
    chips: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AppRadius.lg)
    val date = dateBox(startAt, timeZone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.6f else 1f)
            .clip(shape)
            .border(1.dp, AppColor.Border, shape)
            .clickable(onClick = onClick)
            .padding(AppSpace.xxl),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
    ) {
        Column(
            modifier = Modifier
                .width(52.dp)
                .clip(RoundedCornerShape(AppRadius.md))
                .background(if (isPast) AppColor.ChipNeutralBg else AppColor.AccentSoft)
                .padding(vertical = AppSpace.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val dateColor = if (isPast) AppColor.TextDisabled else AppColor.AccentStrong
            Text(text = date.monthText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = dateColor)
            Text(text = date.dayText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = dateColor)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
                content = chips,
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPast) AppColor.TextBody else AppColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = meta, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
        }
    }
}

// large: 호스트 탭 상태 칩(26dp, 12sp) / 아니면 참여 탭 상태 칩(22dp, 11sp)
@Composable
internal fun StatusChip(text: String, background: Color, textColor: Color, large: Boolean) {
    Box(
        modifier = Modifier
            .height(if (large) 26.dp else 22.dp)
            .background(background, RoundedCornerShape(AppRadius.pill))
            .padding(horizontal = if (large) AppSpace.lg else AppSpace.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = if (large) 12.sp else 11.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

// 13-H2, 13-P2
@Composable
internal fun ActivityEmptyState(message: String, buttonText: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSpace.x4, vertical = AppSpace.x5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpace.xxl, Alignment.CenterVertically),
    ) {
        Text(text = message, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextDisabled)
        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(AppRadius.md))
                .background(AppColor.Accent)
                .clickable(onClick = onClick)
                .padding(horizontal = AppSpace.x4),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = buttonText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.OnAccent)
        }
    }
}
