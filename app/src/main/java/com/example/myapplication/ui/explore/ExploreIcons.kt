package com.example.myapplication.ui.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 하트. filled면 채우고, 아니면 윤곽선만 그린다.
@Composable
fun HeartIcon(filled: Boolean, color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * .5f, h * .88f)
            cubicTo(w * .05f, h * .56f, w * .02f, h * .22f, w * .26f, h * .16f)
            cubicTo(w * .40f, h * .12f, w * .5f, h * .24f, w * .5f, h * .30f)
            cubicTo(w * .5f, h * .24f, w * .60f, h * .12f, w * .74f, h * .16f)
            cubicTo(w * .98f, h * .22f, w * .95f, h * .56f, w * .5f, h * .88f)
            close()
        }
        if (filled) drawPath(path, color)
        else drawPath(path, color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// 온도계 (매너온도 옆)
@Composable
fun ThermometerIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = 1.5.dp.toPx()
        drawRoundRect(
            color, Offset(w * .38f, h * .06f), Size(w * .24f, h * .62f),
            CornerRadius(w * .12f), style = Stroke(sw),
        )
        drawCircle(color, w * .2f, Offset(w * .5f, h * .8f))
    }
}
