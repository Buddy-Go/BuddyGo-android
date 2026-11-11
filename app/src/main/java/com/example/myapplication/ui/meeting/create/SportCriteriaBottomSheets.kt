package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.ui.record.RecordState
import com.example.myapplication.ui.record.hikingStyleDescription
import com.example.myapplication.ui.record.hikingStyleLabel
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 10b 러닝 기준 시트: 거리와 호스트 본인 페이스 한 값. 둘 다 골라야 적용할 수 있다. (service-rules 4장)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunningCriteriaBottomSheet(
    initial: RunningCriteria,
    onApply: (distanceKm: Float, paceSec: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var distance by remember { mutableStateOf(initial.distanceKm) }
    var pace by remember { mutableStateOf(initial.paceSec) }
    CreateSheetFrame(onDismiss = onDismiss) {
        SheetTitle(title = "러닝 기준", subtitle = "거리 · 내 페이스")
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            SheetSectionLabel("거리")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpace.md), verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                RUNNING_DISTANCE_OPTIONS.forEach { km ->
                    OptionChip("${formatDistanceKm(km)}km", selected = distance == km) { distance = km }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            SheetSectionLabel("내 페이스 (1km당)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpace.md), verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                RecordState.PACE_OPTIONS.forEach { sec ->
                    OptionChip(RecordState.paceLabel(sec), selected = pace == sec) { pace = sec }
                }
            }
            Text("모임 참가자는 이 페이스 ±30초를 기준으로 맞는지 확인해요", fontSize = 12.sp, color = AppColor.TextSecondary)
        }
        val ready = distance != null && pace != null
        SheetOutlineButton(text = "적용", enabled = ready) {
            val d = distance
            val p = pace
            if (d != null && p != null) onApply(d, p)
        }
    }
}

// 10c 등산 기준 시트: 취미 / 전문 중 하나
@Composable
fun HikingCriteriaBottomSheet(
    initial: HikingStyle?,
    onApply: (HikingStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    var style by remember { mutableStateOf(initial) }
    CreateSheetFrame(onDismiss = onDismiss) {
        SheetTitle(title = "등산 기준", subtitle = "등산 성향")
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            HikingStyle.entries.forEach { s ->
                val selected = style == s
                val shape = RoundedCornerShape(AppRadius.lg)
                Column(
                    Modifier.fillMaxWidth().clip(shape)
                        .background(if (selected) AppColor.AccentSoft else AppColor.Surface)
                        .border(if (selected) 2.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border, shape)
                        .clickable { style = s }.padding(AppSpace.xxl),
                    verticalArrangement = Arrangement.spacedBy(AppSpace.md),
                ) {
                    Text(hikingStyleLabel(s), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text(hikingStyleDescription(s), fontSize = 14.sp, color = AppColor.TextSecondary)
                }
            }
        }
        SheetOutlineButton(text = "적용", enabled = style != null) { style?.let(onApply) }
    }
}

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.pill)
    Box(
        Modifier.height(44.dp).clip(shape)
            .background(if (selected) AppColor.Accent else AppColor.Surface)
            .border(1.dp, if (selected) AppColor.Accent else AppColor.Border, shape)
            .clickable(onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
    }
}

// 종목 선택 (10a·10b·10c 위쪽): 헬스 · 러닝 · 등산 세 칸
@Composable
fun SportPicker(selected: com.example.myapplication.data.model.Sport, onSelect: (com.example.myapplication.data.model.Sport) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
        Text("종목", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            // 서버가 받는 종목만 보여 준다 (SportSupport). 러닝·등산은 화면에서 아예 뺐다.
            com.example.myapplication.data.model.SportSupport.supported.forEach { sport ->
                val on = sport == selected
                val shape = RoundedCornerShape(AppRadius.lg)
                Column(
                    Modifier.weight(1f).height(84.dp).clip(shape)
                        .background(if (on) AppColor.AccentSoft else AppColor.Surface)
                        .border(1.dp, if (on) AppColor.Accent else AppColor.Border, shape)
                        .clickable(onClickLabel = "${com.example.myapplication.ui.common.sportLabel(sport)} 모임으로 개설") { onSelect(sport) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    com.example.myapplication.ui.record.SportIcon(sport, if (on) AppColor.AccentStrong else AppColor.TextPrimary, size = 26.dp)
                    Text(
                        com.example.myapplication.ui.common.sportLabel(sport), Modifier.padding(top = AppSpace.md),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) AppColor.AccentStrong else AppColor.TextPrimary,
                    )
                }
            }
        }
    }
}
