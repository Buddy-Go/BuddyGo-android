package com.example.myapplication.ui.explore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.example.myapplication.data.model.Sport

// 카카오맵 라벨은 Compose 화면이 아니라 비트맵을 받는다. 그래서 핀(3-4-After)을 Canvas로 직접 그린다.
// 모양과 색은 MapPin(Compose)과 같고, 색은 pinBackgroundColor·pinContentColor를 같이 쓴다.
// 비트맵은 기기 해상도에 맞춰 그리므로 라벨에서는 크기 보정(dp scale)을 꺼야 한다.
// 앵커는 아래 가운데(0.5, 1)로 둔다. 핀 끝이 좌표를 가리킨다.
internal object PinBitmap {
    private const val MARGIN = 6f // 하트 배지가 몸통 밖으로 나오는 만큼 위·좌우에 둔 여백(dp)

    fun create(context: Context, style: PinStyle, count: Int, hasFavorite: Boolean): Bitmap {
        val d = context.resources.displayMetrics.density
        val multiple = count > 1
        val label = "+$count"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinContentColor(style).toArgb()
            textSize = 14f * d
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val iconSize = (if (multiple) 16f else 20f) * d
        val bodyH = (if (multiple) 32f else 44f) * d
        val bodyW = if (multiple) (14f * d + iconSize + 8f * d + 1f * d + 8f * d + textPaint.measureText(label) + 14f * d)
        else 44f * d
        val tailW = 10f * d
        val tailH = 6f * d
        val margin = MARGIN * d

        val bitmap = createBitmap(
            (bodyW + margin * 2).toInt(), (margin + bodyH + tailH).toInt(), Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pinBackgroundColor(style).toArgb() }

        val body = RectF(margin, margin, margin + bodyW, margin + bodyH)
        canvas.drawRoundRect(body, bodyH / 2f, bodyH / 2f, fill)
        val cx = body.centerX()
        val tail = Path().apply {
            moveTo(cx - tailW / 2f, body.bottom - 1f * d)
            lineTo(cx + tailW / 2f, body.bottom - 1f * d)
            lineTo(cx, body.bottom + tailH)
            close()
        }
        canvas.drawPath(tail, fill)

        val contentColor = pinContentColor(style).toArgb()
        val sport = style.sport ?: Sport.HEALTH
        if (multiple) {
            val iconLeft = body.left + 14f * d
            drawSportIcon(canvas, sport, iconLeft, body.centerY() - iconSize / 2f, iconSize, contentColor, d)
            val dividerX = iconLeft + iconSize + 8f * d
            val divider = Paint().apply { color = (contentColor and 0x00FFFFFF) or (0x80 shl 24); strokeWidth = 1f * d }
            canvas.drawLine(dividerX, body.centerY() - 7f * d, dividerX, body.centerY() + 7f * d, divider)
            val baseline = body.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(label, dividerX + 9f * d, baseline, textPaint)
        } else {
            drawSportIcon(canvas, sport, body.centerX() - iconSize / 2f, body.centerY() - iconSize / 2f, iconSize, contentColor, d)
        }

        if (hasFavorite) {
            val r = 9f * d
            val bx = body.right - r + 2f * d
            val by = body.top + r - 4f * d
            canvas.drawCircle(bx, by, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
            drawHeart(canvas, bx, by, 11f * d, 0xFFE5383B.toInt())
        }
        return bitmap
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val left = cx - size / 2f
        val top = cy - size / 2f
        val w = size
        val h = size
        val path = Path().apply {
            moveTo(left + w * .5f, top + h * .88f)
            cubicTo(left + w * .05f, top + h * .56f, left + w * .02f, top + h * .22f, left + w * .26f, top + h * .16f)
            cubicTo(left + w * .40f, top + h * .12f, left + w * .5f, top + h * .24f, left + w * .5f, top + h * .30f)
            cubicTo(left + w * .5f, top + h * .24f, left + w * .60f, top + h * .12f, left + w * .74f, top + h * .16f)
            cubicTo(left + w * .98f, top + h * .22f, left + w * .95f, top + h * .56f, left + w * .5f, top + h * .88f)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    // SportIcon(Compose)과 같은 좌표로 그린다.
    private fun drawSportIcon(canvas: Canvas, sport: Sport, left: Float, top: Float, size: Float, color: Int, d: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.09f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        fun x(f: Float) = left + size * f
        fun y(f: Float) = top + size * f
        when (sport) {
            Sport.HEALTH -> {
                fun bar(bx: Float, by: Float, bw: Float, bh: Float) =
                    canvas.drawRoundRect(RectF(x(bx), y(by), x(bx + bw), y(by + bh)), size * .05f, size * .05f, fill)
                bar(0.04f, 0.38f, 0.08f, 0.24f)
                bar(0.16f, 0.24f, 0.12f, 0.52f)
                bar(0.28f, 0.46f, 0.44f, 0.08f)
                bar(0.72f, 0.24f, 0.12f, 0.52f)
                bar(0.88f, 0.38f, 0.08f, 0.24f)
            }
            Sport.RUNNING -> {
                canvas.drawCircle(x(.62f), y(.16f), size * .09f, fill)
                val body = Path().apply {
                    moveTo(x(.58f), y(.3f)); lineTo(x(.46f), y(.55f))
                    moveTo(x(.56f), y(.36f)); lineTo(x(.78f), y(.46f))
                    moveTo(x(.54f), y(.38f)); lineTo(x(.34f), y(.34f))
                    moveTo(x(.46f), y(.55f)); lineTo(x(.62f), y(.74f)); lineTo(x(.52f), y(.94f))
                    moveTo(x(.46f), y(.55f)); lineTo(x(.3f), y(.68f)); lineTo(x(.16f), y(.62f))
                }
                canvas.drawPath(body, stroke)
            }
            Sport.HIKING -> {
                val m = Path().apply {
                    moveTo(x(.04f), y(.86f)); lineTo(x(.34f), y(.3f)); lineTo(x(.5f), y(.56f))
                    lineTo(x(.62f), y(.4f)); lineTo(x(.96f), y(.86f)); close()
                }
                canvas.drawPath(m, stroke)
            }
        }
    }
}
