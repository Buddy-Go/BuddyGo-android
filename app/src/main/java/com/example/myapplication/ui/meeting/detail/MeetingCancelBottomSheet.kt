package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 모임 취소 시트 ← 5a-X2 경고(사유 비어 있음), 5a-X2-2 사유 입력됨. 호스트 전용.
// 확정 참가자(호스트 제외)가 1명 이상일 때만 띄운다. 0명이면 MeetingDeleteDialog(5a-X3).
// 사유는 필수·최대 CANCEL_REASON_MAX_LENGTH자이고 확정 참가자에게 알림으로 전달된다 (service-rules 4장).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingCancelBottomSheet(
    confirmedOthersCount: Int,
    onConfirm: (reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
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
        Box(
            modifier = Modifier
                .imePadding()
                .padding(start = AppSpace.x4, end = AppSpace.x4, bottom = AppSpace.xxl),
        ) {
            MeetingCancelSheetBody(
                confirmedOthersCount = confirmedOthersCount,
                reason = reason,
                onReasonChange = { reason = it.take(CANCEL_REASON_MAX_LENGTH) },
                onBack = onDismiss,
                onConfirm = { onConfirm(reason.trim()) },
            )
        }
    }
}

@Composable
internal fun MeetingCancelSheetBody(
    confirmedOthersCount: Int,
    reason: String,
    onReasonChange: (String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val canConfirm = canSubmitCancelReason(reason)
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
        Text(text = "모임을 취소할까요?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)

        // 경고 상자
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.md))
                .background(AppColor.DestructiveSoftBg)
                .padding(AppSpace.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpace.sm),
        ) {
            WarningLine("확정된 메이트 ${confirmedOthersCount}명에게 취소 알림이 가요")
            WarningLine("매너온도가 내려가요")
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
                Text(
                    text = "취소 사유",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColor.TextPrimary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "필수",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.IconMuted,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            val textStyle = TextStyle(
                fontSize = 14.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextPrimary,
            )
            val fieldShape = RoundedCornerShape(AppRadius.md)
            BasicTextField(
                value = reason,
                onValueChange = onReasonChange,
                textStyle = textStyle,
                cursorBrush = SolidColor(AppColor.TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .border(1.dp, AppColor.Border, fieldShape)
                    .padding(horizontal = AppSpace.xl, vertical = AppSpace.lg),
                decorationBox = { inner ->
                    Box {
                        if (reason.isEmpty()) {
                            Text(text = "메이트들에게 그대로 전달돼요", style = textStyle.copy(color = AppColor.TextDisabled))
                        }
                        inner()
                    }
                },
            )
            Text(
                text = "${reason.length} / $CANCEL_REASON_MAX_LENGTH",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            SheetButton(
                text = "돌아가기",
                background = AppColor.Surface,
                textColor = AppColor.TextPrimary,
                bordered = true,
                enabled = true,
                onClick = onBack,
                modifier = Modifier.weight(1f),
            )
            // 5a-X2: 사유가 없으면 회색 / 5a-X2-2: 빨강
            SheetButton(
                text = "모임 취소",
                background = if (canConfirm) AppColor.Danger else AppColor.ChipNeutralBg,
                textColor = if (canConfirm) AppColor.Surface else AppColor.TextDisabled,
                bordered = false,
                enabled = canConfirm,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WarningLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.sm)) {
        Text(text = "·", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.DestructiveText)
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.DestructiveText)
    }
}

@Composable
private fun SheetButton(
    text: String,
    background: Color,
    textColor: Color,
    bordered: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(background)
            .then(if (bordered) Modifier.border(1.dp, AppColor.Border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

// ---------- 미리보기 (시트 내용만) ----------

@Composable
private fun PreviewBody(reason: String) {
    Box(
        modifier = Modifier
            .background(AppColor.Surface)
            .padding(AppSpace.x4),
    ) {
        MeetingCancelSheetBody(confirmedOthersCount = 2, reason = reason, onReasonChange = {}, onBack = {}, onConfirm = {})
    }
}

@Preview(name = "5a-X2 모임 취소 · 경고", widthDp = 360)
@Composable
private fun Preview5aX2() = PreviewBody("")

@Preview(name = "5a-X2-2 모임 취소 · 사유 입력됨", widthDp = 360)
@Composable
private fun Preview5aX22() = PreviewBody("갑작스러운 일정이 생겨 진행이 어렵게 됐습니다. 죄송합니다.")
