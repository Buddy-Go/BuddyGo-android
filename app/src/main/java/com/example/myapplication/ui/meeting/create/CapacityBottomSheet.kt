package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 정원·성별 시트. 디자인 프레임이 없어 10d~10f 시트와 같은 모양으로 만들었다. (service-rules 4장)
// 헬스는 호스트 포함 2~3명이고, 호스트 성별과 다른 성별 제한은 고를 수 없다.
@Composable
fun CapacityBottomSheet(
    range: IntRange,
    initialCapacity: Int?,
    initialGender: GenderLimit,
    hostGender: Gender?,
    onApply: (capacity: Int, gender: GenderLimit) -> Unit,
    onDismiss: () -> Unit,
) {
    var capacity by remember { mutableStateOf(initialCapacity) }
    var gender by remember { mutableStateOf(initialGender) }
    val allowed = allowedGenderLimits(hostGender)

    CreateSheetFrame(onDismiss = onDismiss) {
        SheetTitle(title = "정원 · 성별", subtitle = "호스트 포함")
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            SheetSectionLabel("정원")
            if (range.last - range.first < 3) {
                // 헬스(2~3명)처럼 선택지가 적으면 칸으로 고른다.
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                    range.forEach { n ->
                        ChoiceChip("${n}명", selected = capacity == n, enabled = true, modifier = Modifier.weight(1f)) { capacity = n }
                    }
                }
            } else {
                // 러닝·등산(2명 이상)은 −/+로 고른다. 처음에는 가장 작은 값에서 시작한다.
                CapacityStepper(value = capacity ?: range.first, range = range, onChange = { capacity = it })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            SheetSectionLabel("성별")
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                GenderLimit.entries.forEach { g ->
                    ChoiceChip(g.label, selected = gender == g, enabled = g in allowed, modifier = Modifier.weight(1f)) { gender = g }
                }
            }
            if (allowed.size < GenderLimit.entries.size) {
                Text("내 성별과 다른 성별로는 제한할 수 없어요", fontSize = 12.sp, color = AppColor.TextSecondary)
            }
        }
        // 헬스는 칸을 눌러 골라야 하고, 러닝·등산은 처음 보이는 값(가장 작은 값)으로도 적용할 수 있다.
        val effective = capacity ?: range.first.takeIf { range.last - range.first >= 3 }
        SheetOutlineButton(text = "적용", enabled = effective != null) { effective?.let { onApply(it, gender) } }
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) AppColor.AccentSoft else AppColor.Surface)
            .border(1.dp, if (selected) AppColor.Accent else AppColor.Border, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> AppColor.TextDisabled
                selected -> AppColor.AccentStrong
                else -> AppColor.TextPrimary
            },
        )
    }
}

// 정원 범위는 종목마다 다르다. 헬스 2~3명(호스트 포함), 러닝·등산 2명 이상 → capacityRangeOf(sport)

// 호스트 성별과 다른 성별 제한은 고를 수 없다 **(가정)**. 호스트 성별을 모르면 모두 고를 수 있다.
fun allowedGenderLimits(host: Gender?): List<GenderLimit> = when (host) {
    Gender.MALE -> listOf(GenderLimit.ANYONE, GenderLimit.MALE_ONLY)
    Gender.FEMALE -> listOf(GenderLimit.ANYONE, GenderLimit.FEMALE_ONLY)
    null -> GenderLimit.entries
}

@Composable
private fun CapacityStepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
        StepButton("−", enabled = value > range.first) { onChange(value - 1) }
        Text("${value}명", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        StepButton("+", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        Modifier.height(48.dp).width(64.dp).clip(shape).background(if (enabled) AppColor.AccentSoft else AppColor.ChipNeutralBg)
            .border(1.dp, if (enabled) AppColor.Accent else AppColor.Border, shape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (enabled) AppColor.AccentStrong else AppColor.TextDisabled) }
}
