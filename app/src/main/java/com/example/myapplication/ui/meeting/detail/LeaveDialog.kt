package com.example.myapplication.ui.meeting.detail

import androidx.compose.runtime.Composable
import com.example.myapplication.ui.common.ConfirmDialog

// 나가기 확인창. 나간 시점에 따라 모임과 내 매너온도에 생기는 일을 알려 준다. (service-rules 5장)
// 데드라인 후 마감 상태에서 나가면 모임이 취소되므로 그 안내가 꼭 들어간다.
@Composable
fun LeaveDialog(
    plan: LeavePlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = plan.title,
        message = plan.message,
        confirmText = "나가기",
        dismissText = "취소",
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
