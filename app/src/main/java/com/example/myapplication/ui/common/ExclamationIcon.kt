package com.example.myapplication.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 동그라미 안의 느낌표. 오류·경고 안내에서 색만 바꿔 쓴다. (Material 아이콘 의존성이 없어 Canvas로 그림)
@Composable
fun ExclamationIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, strokeWidth: Dp = 1.8.dp) {
    Canvas(modifier.size(size)) {
        val sw = strokeWidth.toPx()
        drawCircle(color, this.size.width / 2 - sw / 2, style = Stroke(sw))
        drawLine(color, Offset(this.size.width / 2, this.size.height * .28f), Offset(this.size.width / 2, this.size.height * .58f), sw, StrokeCap.Round)
        drawCircle(color, sw * .7f, Offset(this.size.width / 2, this.size.height * .74f))
    }
}
