package com.example.myapplication.ui.record

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
import com.example.myapplication.data.model.Sport

// 종목 아이콘 (Material 아이콘 의존성이 없어 Canvas로 그림. 탐색에서도 필요해지면 ui/common으로 옮긴다)
@Composable
fun SportIcon(sport: Sport, color: Color, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = 2.2.dp.toPx()
        when (sport) {
            Sport.HEALTH -> {
                fun bar(x: Float, y: Float, bw: Float, bh: Float) =
                    drawRoundRect(color, Offset(w * x, h * y), Size(w * bw, h * bh), CornerRadius(w * .05f))
                bar(0.04f, 0.38f, 0.08f, 0.24f)
                bar(0.16f, 0.24f, 0.12f, 0.52f)
                bar(0.28f, 0.46f, 0.44f, 0.08f)
                bar(0.72f, 0.24f, 0.12f, 0.52f)
                bar(0.88f, 0.38f, 0.08f, 0.24f)
            }
            Sport.RUNNING -> {
                drawCircle(color, w * .09f, Offset(w * .62f, h * .16f))
                val body = Path().apply {
                    moveTo(w * .58f, h * .3f); lineTo(w * .46f, h * .55f)           // 몸통
                    moveTo(w * .56f, h * .36f); lineTo(w * .78f, h * .46f)           // 앞 팔
                    moveTo(w * .54f, h * .38f); lineTo(w * .34f, h * .34f)           // 뒷 팔
                    moveTo(w * .46f, h * .55f); lineTo(w * .62f, h * .74f); lineTo(w * .52f, h * .94f) // 앞 다리
                    moveTo(w * .46f, h * .55f); lineTo(w * .3f, h * .68f); lineTo(w * .16f, h * .62f)  // 뒷 다리
                }
                drawPath(body, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Sport.HIKING -> {
                val m = Path().apply {
                    moveTo(w * .04f, h * .86f); lineTo(w * .34f, h * .3f); lineTo(w * .5f, h * .56f)
                    lineTo(w * .62f, h * .4f); lineTo(w * .96f, h * .86f); close()
                }
                drawPath(m, color, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
