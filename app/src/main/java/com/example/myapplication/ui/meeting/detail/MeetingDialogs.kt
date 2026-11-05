package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 모임 상세 호스트용 다이얼로그 (문진곤)
// 5a-X3 모임 삭제 확인: 확정 참가자(호스트 제외)가 0명일 때. 삭제하면 대기 신청은 자동 거절되고 매너온도 영향은 없다 (service-rules 4장).
// TODO(임시): 공용 ConfirmDialog(문찬주)가 정해지면 그 틀로 바꾼다.
@Composable
fun MeetingDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        MeetingDeleteDialogCard(onConfirm = onConfirm, onDismiss = onDismiss)
    }
}

@Composable
internal fun MeetingDeleteDialogCard(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColor.Surface)
            .border(1.dp, AppColor.Border, shape)
            .padding(AppSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpace.md),
    ) {
        Text(text = "모임을 삭제할까요?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Text(text = "아직 확정된 메이트가 없어요", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpace.sm),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.xxl, Alignment.End),
        ) {
            Text(
                text = "취소",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.TextBody,
                modifier = Modifier.clickable(onClick = onDismiss),
            )
            Text(
                text = "삭제",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.Danger,
                modifier = Modifier.clickable(onClick = onConfirm),
            )
        }
    }
}

@Preview(name = "5a-X3 모임 삭제 확인", widthDp = 360)
@Composable
private fun Preview5aX3() {
    Box(modifier = Modifier.background(AppColor.Surface).padding(AppSpace.x4)) {
        MeetingDeleteDialogCard(onConfirm = {}, onDismiss = {})
    }
}
