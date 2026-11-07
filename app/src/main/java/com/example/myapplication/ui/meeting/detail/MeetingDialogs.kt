package com.example.myapplication.ui.meeting.detail

import androidx.compose.runtime.Composable
import com.example.myapplication.ui.common.ConfirmDialog

// 모임 상세 호스트용 다이얼로그 (문진곤)
// 5a-X3 모임 삭제 확인: 확정 참가자(호스트 제외)가 0명일 때. 삭제하면 대기 신청은 자동 거절되고 매너온도 영향은 없다 (service-rules 4장).
@Composable
fun MeetingDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = "모임을 삭제할까요?",
        message = "아직 확정된 메이트가 없어요",
        confirmText = "삭제",
        dismissText = "취소",
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
