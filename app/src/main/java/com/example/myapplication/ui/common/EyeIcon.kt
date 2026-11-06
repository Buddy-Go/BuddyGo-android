package com.example.myapplication.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AppColor

// 비밀번호 보기/숨기기 눈 아이콘. crossed = true이면 사선(숨김 상태).
// Material 아이콘 의존성이 없어 Canvas로 그림.
@Composable
fun EyeIcon(crossed: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val c = AppColor.IconMuted
        val sw = 2.dp.toPx()
        drawOval(c, Offset(sw, size.height * 0.2f), Size(size.width - 2 * sw, size.height * 0.6f), style = Stroke(sw))
        drawCircle(c, radius = size.width * 0.14f, style = Stroke(sw))
        if (crossed) {
            drawLine(
                c,
                Offset(size.width * 0.15f, size.height * 0.85f),
                Offset(size.width * 0.85f, size.height * 0.15f),
                strokeWidth = sw,
            )
        }
    }
}
