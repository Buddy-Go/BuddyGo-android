package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 모임 개설 시트(10d, 10e, 10f)가 같이 쓰는 틀과 부품.
// 탐색 필터 시트(4d~4f)도 모양이 같아서, 나중에 ui/common으로 옮길지 팀과 정한다.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateSheetFrame(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = AppRadius.sheet, topEnd = AppRadius.sheet),
        containerColor = AppColor.Surface,
        scrimColor = AppColor.Scrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(AppColor.Grabber),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = AppSpace.x4, end = AppSpace.x4, bottom = AppSpace.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
            content = content,
        )
    }
}

// "헬스 기준  모임에서 할 운동과…" 처럼 큰 제목 옆에 작은 설명
@Composable
internal fun SheetTitle(title: String, subtitle: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.TextPrimary,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.TextSecondary,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
internal fun SheetSectionLabel(text: String) {
    Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColor.TextSecondary)
}

// 10d·10e '적용' (테두리 버튼)
@Composable
internal fun SheetOutlineButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .border(1.dp, if (enabled) AppColor.Accent else AppColor.Border, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) AppColor.AccentStrong else AppColor.TextDisabled,
        )
    }
}

// 10f '확인' (채운 버튼)
@Composable
internal fun SheetFilledButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppColor.Accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.OnAccent)
    }
}

// 10d '내 기록 표시하기' 토글 (46×28)
@Composable
internal fun PillToggle(checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(if (checked) AppColor.Accent else AppColor.Grabber)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .shadow(1.dp, RoundedCornerShape(AppRadius.pill))
                .background(AppColor.Surface, RoundedCornerShape(AppRadius.pill)),
        )
    }
}

// ---------- 미리보기: 시트 내용만 (10d, 10d-2, 10d 보조, 10e, 10f) ----------

@Composable
private fun SheetPreviewBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.Surface)
            .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.x4, bottom = AppSpace.xxl),
        content = content,
    )
}

private val previewRecord = ExerciseRecord(benchPressKg = 80, armLevel = SkillLevel.INTERMEDIATE)

@Preview(name = "10d-After 헬스 기준", widthDp = 360)
@Composable
private fun Preview10d() = SheetPreviewBox {
    HealthCriteriaSheetBody(
        parts = setOf(BodyPart.CHEST, BodyPart.ARM),
        showMyRecord = true,
        myRecord = previewRecord,
        onPartToggle = {},
        onShowMyRecordToggle = {},
        onApply = {},
    )
}

@Preview(name = "10d-2 헬스 기준 기록 숨김", widthDp = 360)
@Composable
private fun Preview10d2() = SheetPreviewBox {
    HealthCriteriaSheetBody(
        parts = setOf(BodyPart.CHEST, BodyPart.ARM),
        showMyRecord = false,
        myRecord = previewRecord,
        onPartToggle = {},
        onShowMyRecordToggle = {},
        onApply = {},
    )
}

@Preview(name = "10d 보조 기록 없음 · 부위 미선택", widthDp = 360)
@Composable
private fun Preview10dEmpty() = SheetPreviewBox {
    HealthCriteriaSheetBody(
        parts = emptySet(),
        showMyRecord = true,
        myRecord = ExerciseRecord(),
        onPartToggle = {},
        onShowMyRecordToggle = {},
        onApply = {},
    )
}

@Preview(name = "10e 설명", widthDp = 360)
@Composable
private fun Preview10e() = SheetPreviewBox {
    DescriptionSheetBody(
        text = "벤치 5×5 먼저 하고 삼두 보조 운동 두 개로 마무리해요. 스팟 서로 봐주실 분이면 좋아요.",
        onTextChange = {},
        onApply = {},
    )
}

@Preview(name = "10f 시간 설정", widthDp = 360)
@Composable
private fun Preview10f() = SheetPreviewBox {
    TimeSheetBody(
        hour = 19,
        minuteIndex = 0,
        durationMinutes = 90,
        onHourSelect = {},
        onMinuteSelect = {},
        onDurationMinus = {},
        onDurationPlus = {},
        onConfirm = {},
    )
}
