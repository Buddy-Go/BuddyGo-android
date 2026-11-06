package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.common.ConfirmDialog
import com.example.myapplication.ui.common.DialogButtonStyle
import com.example.myapplication.ui.explore.RefreshIcon
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 9a 체크인 확인, 9b 체크인 확인(범위 밖). 모임 장소와의 거리를 보여 주고, 범위 안일 때만 체크인할 수 있다.
// 위치를 가져오는 중·위치를 못 얻음·보내지 못함은 디자인 프레임이 없어 같은 틀로 만들었다.
@Composable
fun CheckInDialog(
    dialog: CheckInPrompt,
    onRefresh: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val texts = checkInTextsOf(dialog)
    ConfirmDialog(
        title = texts.title,
        message = if (texts.distanceLine == null) texts.message else null,
        confirmText = texts.confirmText,
        dismissText = texts.dismissText,
        style = DialogButtonStyle.Filled,
        confirmEnabled = texts.confirmEnabled,
        // 위치를 못 얻었거나 보내지 못했을 때의 '다시 시도'는 위치를 새로 가져온다.
        onConfirm = if (dialog == CheckInPrompt.NoLocation || dialog == CheckInPrompt.Failed) onRefresh else onConfirm,
        onDismiss = onDismiss,
        content = texts.distanceLine?.let { line ->
            {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.md)).background(AppColor.ChipNeutralBg).padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PinIcon(AppColor.TextSecondary)
                    Spacer(Modifier.width(AppSpace.xl))
                    Text(line, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
                    if (texts.canRefresh) {
                        Box(Modifier.size(32.dp).clip(CircleShape).iconButton("거리 다시 재기", onRefresh), contentAlignment = Alignment.Center) {
                            RefreshIcon(AppColor.TextSecondary, size = 20.dp)
                        }
                    }
                }
                Spacer(Modifier.height(AppSpace.xl))
                texts.message?.let {
                    Text(it, fontSize = 15.sp, color = AppColor.TextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        },
    )
}

@Composable
private fun PinIcon(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val path = Path().apply {
            moveTo(w * .5f, w * .92f)
            cubicTo(w * .15f, w * .55f, w * .15f, w * .12f, w * .5f, w * .12f)
            cubicTo(w * .85f, w * .12f, w * .85f, w * .55f, w * .5f, w * .92f)
            close()
        }
        drawPath(path, color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, w * .1f, Offset(w * .5f, w * .45f), style = Stroke(1.8.dp.toPx()))
    }
}
