package com.example.myapplication.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 평가 대기 카드 ← 13-R 평소(n일 남음) / 마감 임박(오늘까지, 초록 굵은 글자)
// 카드나 '평가'를 누르면 평가 목록(12a)으로 간다.
@Composable
fun ReviewPendingCard(
    item: ReviewPendingItem,
    now: Long,
    onClick: () -> Unit,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    val due = item.dueText(now, timeZone)
    val shape = RoundedCornerShape(AppRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColor.ReviewCardBg)
            .border(1.dp, AppColor.Accent, shape)
            .clickable(onClick = onClick)
            .padding(AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(AppColor.AccentSoft, RoundedCornerShape(AppRadius.md)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_thermometer),
                contentDescription = null,
                tint = AppColor.AccentStrong,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.xxs)) {
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = due.text,
                fontSize = 12.sp,
                fontWeight = if (due.isUrgent) FontWeight.Bold else FontWeight.Medium,
                color = if (due.isUrgent) AppColor.AccentStrong else AppColor.TextSecondary,
            )
        }
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(AppRadius.pill))
                .background(AppColor.Accent)
                .padding(horizontal = AppSpace.xl),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "평가", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColor.OnAccent)
        }
    }
}

// ---------- 미리보기 ----------

private const val PREVIEW_DAY = 86_400_000L
private const val PREVIEW_NOW = 1788566400000L // 2026-09-05 09:00 (서울)

@Preview(name = "13-R 평가 대기 카드 · 평소 / 마감 임박", widthDp = 360)
@Composable
private fun Preview13R() {
    val seoul = TimeZone.getTimeZone("Asia/Seoul")
    Column(
        modifier = Modifier
            .background(AppColor.Surface)
            .padding(AppSpace.x4),
        verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
    ) {
        val item = ReviewPendingItem("p1", "등·이두 같이 치실 분", endedAt = PREVIEW_NOW, reviewDueAt = PREVIEW_NOW + 5 * PREVIEW_DAY)
        ReviewPendingCard(item = item, now = PREVIEW_NOW, onClick = {}, timeZone = seoul)
        ReviewPendingCard(item = item, now = item.reviewDueAt - 3_600_000L, onClick = {}, timeZone = seoul)
    }
}
