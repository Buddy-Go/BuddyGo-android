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
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                HEALTH_CAPACITY_RANGE.forEach { n ->
                    ChoiceChip("${n}명", selected = capacity == n, enabled = true, modifier = Modifier.weight(1f)) { capacity = n }
                }
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
        SheetOutlineButton(text = "적용", enabled = capacity != null) { capacity?.let { onApply(it, gender) } }
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

// 헬스 정원은 호스트 포함 2~3명 (service-rules 4장). 서버 설정값이라 바뀌면 여기만 고친다.
val HEALTH_CAPACITY_RANGE = 2..3

// 호스트 성별과 다른 성별 제한은 고를 수 없다 **(가정)**. 호스트 성별을 모르면 모두 고를 수 있다.
fun allowedGenderLimits(host: Gender?): List<GenderLimit> = when (host) {
    Gender.MALE -> listOf(GenderLimit.ANYONE, GenderLimit.MALE_ONLY)
    Gender.FEMALE -> listOf(GenderLimit.ANYONE, GenderLimit.FEMALE_ONLY)
    null -> GenderLimit.entries
}
