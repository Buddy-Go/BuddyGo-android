package com.example.myapplication.ui.mate

import com.example.myapplication.ui.common.SportChip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.MateSummary
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 메이트 탭 ← 11a 목록, 11a-2 빈 상태
// 줄 아래: 지금 운동 중이면 '● 지금 운동 중 · 종목', 아니면 '종목 칩 + 모임 제목 · 시각'.
@Composable
fun MateListTab(
    mates: List<MateSummary>,
    now: Long,
    onMateClick: (userId: String) -> Unit, // 15a 프로필
    modifier: Modifier = Modifier,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    if (mates.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpace.md, Alignment.CenterVertically),
        ) {
            Text(text = "아직 메이트가 없어요", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            Text(text = "모임에서 만난 사람에게 메이트를 신청해보세요", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
        }
        return
    }
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        mates.forEach { mate ->
            MateRow(mate = mate, line = mate.line(now, timeZone), onClick = { onMateClick(mate.userId) })
        }
    }
}

@Composable
private fun MateRow(mate: MateSummary, line: MateLine, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = AppSpace.x4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
        ) {
            MateAvatar(userId = mate.userId, nickname = mate.nickname)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.xxs)) {
                Text(text = mate.nickname, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
                    if (line.isLive) {
                        Box(Modifier.size(7.dp).background(AppColor.Accent, RoundedCornerShape(AppRadius.pill)))
                    }
                    line.sport?.let { SportChip(it, fontSize = 11) }
                    Text(
                        text = line.text,
                        fontSize = 13.sp,
                        fontWeight = if (line.isLive) FontWeight.SemiBold else FontWeight.Medium,
                        color = when {
                            line.isLive -> AppColor.AccentStrong
                            mate.nextActivity == null -> AppColor.TextSecondary
                            else -> AppColor.TextBody
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = AppColor.IconMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
    }
}
