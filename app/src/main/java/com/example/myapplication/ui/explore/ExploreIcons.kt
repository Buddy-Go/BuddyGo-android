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

// 돋보기
@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = 2.dp.toPx()
        drawCircle(color, w * .34f, Offset(w * .44f, w * .44f), style = Stroke(sw))
        drawLine(color, Offset(w * .68f, w * .68f), Offset(w * .9f, w * .9f), sw, StrokeCap.Round)
    }
}

// 필터(가로줄 셋, 아래로 갈수록 짧아진다)
@Composable
fun FilterIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = 2.dp.toPx()
        drawLine(color, Offset(w * .12f, w * .26f), Offset(w * .88f, w * .26f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * .26f, w * .5f), Offset(w * .74f, w * .5f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * .4f, w * .74f), Offset(w * .6f, w * .74f), sw, StrokeCap.Round)
    }
}

// 종
@Composable
fun BellIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = 1.9.dp.toPx()
        val body = Path().apply {
            moveTo(w * .2f, w * .72f)
            cubicTo(w * .3f, w * .62f, w * .28f, w * .52f, w * .28f, w * .42f)
            cubicTo(w * .28f, w * .24f, w * .38f, w * .14f, w * .5f, w * .14f)
            cubicTo(w * .62f, w * .14f, w * .72f, w * .24f, w * .72f, w * .42f)
            cubicTo(w * .72f, w * .52f, w * .7f, w * .62f, w * .8f, w * .72f)
            close()
        }
        drawPath(body, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(w * .42f, w * .84f), Offset(w * .58f, w * .84f), sw, StrokeCap.Round)
    }
}

// 되돌리기 화살표 ('현 지도에서 검색')
@Composable
fun RefreshIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = 2.dp.toPx()
        drawArc(
            color, startAngle = -60f, sweepAngle = 280f, useCenter = false,
            topLeft = Offset(w * .16f, w * .16f), size = Size(w * .68f, w * .68f),
            style = Stroke(sw, cap = StrokeCap.Round),
        )
        val arrow = Path().apply {
            moveTo(w * .12f, w * .12f); lineTo(w * .12f, w * .42f); lineTo(w * .42f, w * .3f)
        }
        drawPath(arrow, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
