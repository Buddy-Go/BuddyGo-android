package com.example.myapplication.ui.explore.filter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = 2.2.dp.toPx()
        drawLine(color, Offset(w * .2f, w * .2f), Offset(w * .8f, w * .8f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * .8f, w * .2f), Offset(w * .2f, w * .8f), sw, StrokeCap.Round)
    }
}

@Composable
fun CalendarIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = 1.8.dp.toPx()
        drawRoundRect(color, Offset(w * .12f, w * .2f), Size(w * .76f, w * .66f), CornerRadius(w * .12f), style = Stroke(sw))
        drawLine(color, Offset(w * .12f, w * .42f), Offset(w * .88f, w * .42f), sw)
        drawLine(color, Offset(w * .32f, w * .1f), Offset(w * .32f, w * .28f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * .68f, w * .1f), Offset(w * .68f, w * .28f), sw, StrokeCap.Round)
    }
}

@Composable
fun ClockIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = 1.8.dp.toPx()
        drawCircle(color, w * .38f, Offset(w * .5f, w * .5f), style = Stroke(sw))
        val hands = Path().apply { moveTo(w * .5f, w * .3f); lineTo(w * .5f, w * .5f); lineTo(w * .64f, w * .58f) }
        drawPath(hands, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChevronRightIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply { moveTo(w * .38f, w * .22f); lineTo(w * .66f, w * .5f); lineTo(w * .38f, w * .78f) }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
