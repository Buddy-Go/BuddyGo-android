package com.example.myapplication.ui.explore.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace

// 4e 상세 조건 시트 (조건 설정 위에 뜬다): 성별과 호스트 매너온도 하한.
// '적용'은 시트를 닫고, 밖을 누르거나 내리면 열기 전 값으로 돌아간다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailConditionBottomSheet(
    state: FilterState,
    onSetGender: (GenderLimit) -> Unit,
    onSetMannerOn: (Boolean) -> Unit,
    onSetMinManner: (Int) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    val filter = state.draft
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.Surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = AppSpace.x4),
                verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                    Text("상세 조건", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text("성별 · 매너온도", fontSize = 15.sp, color = AppColor.TextSecondary, modifier = Modifier.padding(bottom = 3.dp))
                }
                Text("성별", fontSize = 15.sp, color = AppColor.TextSecondary)
                // '누구나'는 성별을 정하지 않은 상태라서, 성별이 null일 때 선택된 것으로 보인다.
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                    GenderLimit.entries.forEach { gender ->
                        val selected = (filter.gender ?: GenderLimit.ANYONE) == gender
                        OptionCard(genderLabel(gender), selected = selected, onClick = { onSetGender(gender) }, modifier = Modifier.weight(1f))
                    }
                }
                SwitchRow(
                    title = "매너온도 하한",
                    subtitle = null,
                    checked = filter.minMannerTemperature != null,
                    enabled = true,
                    onCheckedChange = onSetMannerOn,
                )
                // 하한을 켰을 때만 선택지가 보인다. (호스트 기준)
                filter.minMannerTemperature?.let { selected ->
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                        DetailDefaults.MANNER_OPTIONS.forEach { degrees ->
                            OptionCard("${degrees}°C+", selected = selected == degrees, onClick = { onSetMinManner(degrees) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            ActionButton(
                text = "적용", onClick = onApply,
                modifier = Modifier.padding(horizontal = AppSpace.x4).padding(top = AppSpace.xl, bottom = AppSpace.x4),
            )
        }
    }
}

fun genderLabel(gender: GenderLimit): String = when (gender) {
    GenderLimit.ANYONE -> "누구나"
    GenderLimit.FEMALE_ONLY -> "여성만"
    GenderLimit.MALE_ONLY -> "남성만"
}
