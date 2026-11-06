package com.example.myapplication.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 버튼 모양 두 가지
enum class DialogButtonStyle {
    Text,   // 5a-X3 삭제 확인: 오른쪽 아래 글자 버튼 두 개. 제목·설명은 왼쪽 정렬.
    Filled, // 9a·9b 체크인 확인: 같은 너비의 버튼 두 개(테두리 + 라임). 제목·설명은 가운데 정렬.
}

// 다이얼로그 공통 틀. 어두운 배경 위에 흰 둥근 카드를 띄운다. (route로 만들지 않음)
// - content: 제목과 버튼 사이에 넣을 내용 (예: 체크인의 '모임 장소에서 40m' 상자)
// - destructive: 확인 버튼이 되돌릴 수 없는 동작(삭제 등)이면 true. 글자형에서 빨간색이 된다.
// - confirmEnabled: false이면 확인 버튼이 회색이 되고 눌리지 않는다. (9b 범위 밖)
@Composable
fun ConfirmDialog(
    title: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    style: DialogButtonStyle = DialogButtonStyle.Text,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val centered = style == DialogButtonStyle.Filled
        Column(
            modifier = modifier
                .padding(horizontal = AppSpace.x5 + AppSpace.x4)
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.lg + 4.dp))
                .background(AppColor.Surface)
                .padding(AppSpace.x4 + AppSpace.md),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                text = title,
                color = AppColor.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            if (content != null) {
                Spacer(Modifier.height(AppSpace.x4))
                content()
            }
            if (message != null) {
                Spacer(Modifier.height(if (content != null) AppSpace.xl else AppSpace.md))
                Text(
                    text = message,
                    color = AppColor.TextSecondary,
                    fontSize = if (centered) 14.sp else 15.sp,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(AppSpace.x5))
            when (style) {
                DialogButtonStyle.Text -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.x5, Alignment.End),
                ) {
                    Text(
                        text = dismissText,
                        color = AppColor.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onDismiss),
                    )
                    Text(
                        text = confirmText,
                        color = if (destructive) AppColor.DestructiveText else AppColor.AccentStrong,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = confirmEnabled, onClick = onConfirm),
                    )
                }
                DialogButtonStyle.Filled -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
                ) {
                    FilledDialogButton(dismissText, onDismiss, primary = false, modifier = Modifier.weight(1f))
                    FilledDialogButton(confirmText, onConfirm, primary = true, enabled = confirmEnabled, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FilledDialogButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(
                when {
                    !primary -> AppColor.Surface
                    enabled -> AppColor.Accent
                    else -> AppColor.SkeletonBase
                },
            )
            .then(if (!primary) Modifier.border(1.dp, AppColor.Border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary && !enabled) AppColor.TextDisabled else AppColor.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun ConfirmDialogTextPreview() {
    MyApplicationTheme(dynamicColor = false) {
        ConfirmDialog(
            title = "모임을 삭제할까요?",
            message = "아직 확정된 메이트가 없어요",
            confirmText = "삭제",
            dismissText = "취소",
            destructive = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun ConfirmDialogFilledPreview() {
    MyApplicationTheme(dynamicColor = false) {
        ConfirmDialog(
            title = "아직 조금 멀어요",
            message = "반경 100m 안에서 체크인할 수 있어요",
            confirmText = "체크인",
            dismissText = "닫기",
            style = DialogButtonStyle.Filled,
            confirmEnabled = false,
            onConfirm = {},
            onDismiss = {},
        ) {
            Text("모임 장소에서 320m", color = AppColor.TextPrimary, fontSize = 16.sp)
        }
    }
}
