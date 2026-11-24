package com.example.myapplication.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 매너온도를 숫자 한 줄이 아니라 '게이지'로 보여 준다.
//  - 오른쪽에 큰 숫자, 아래에 0~100 막대. 채워진 색은 차가운 파랑 → 연두(시작점 36.5°) → 주황 → 빨강으로 이어진다
//  - 채워진 끝에 온도에 따라 표정이 바뀌는 얼굴이 붙고, 화면이 열릴 때 0에서 현재 온도까지 차오른다
//  - 막대 위 ▼는 모두가 시작하는 36.5°(시작점). ⓘ를 누르면 매너온도가 어떻게 오르내리는지 설명한다
@Composable
fun MannerGauge(temperature: Float, modifier: Modifier = Modifier) {
    var showInfo by remember { mutableStateOf(false) }
    val tier = mannerTier(temperature)
    val target = mannerFraction(temperature)

    // 처음 보일 때 0에서 현재 값까지 차오른다
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) { progress.animateTo(target, tween(durationMillis = 900, easing = FastOutSlowInEasing)) }

    val shape = RoundedCornerShape(AppRadius.lg)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColor.Surface)
            .border(1.dp, AppColor.Border, shape)
            .padding(horizontal = AppSpace.xxl, vertical = 22.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "매너온도 ${mannerNumber(temperature)}도, ${tier.label}, ${mannerDeltaText(temperature)}"
            },
    ) {
        // 제목 + 도움말 / 큰 숫자
        Row(verticalAlignment = Alignment.Bottom) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.sm))
                    .clickable(role = Role.Button, onClickLabel = "매너온도 설명 보기") { showInfo = true }
                    .padding(vertical = AppSpace.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
            ) {
                Text("매너온도", color = AppColor.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                HelpMark()
            }
            Spacer(Modifier.weight(1f))
            Text(mannerNumber(temperature), color = AppColor.TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "°C",
                color = AppColor.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 3.dp, bottom = 8.dp),
            )
        }

        GaugeBar(progress = progress.value, tier = tier, modifier = Modifier.padding(top = AppSpace.lg))

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = AppSpace.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TierPill(tier)
            Spacer(Modifier.weight(1f))
            Text(
                text = mannerDeltaText(temperature),
                color = AppColor.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    if (showInfo) {
        ConfirmDialog(
            title = "매너온도가 뭐예요?",
            message = "모두 ${mannerNumber(MANNER_BASE)}°C에서 시작해요.\n" +
                "모임을 잘 마치고 좋은 후기를 받으면 올라가고, 노쇼나 직전 취소가 잦으면 내려가요.",
            confirmText = "",
            dismissText = "닫기",
            onConfirm = {},
            onDismiss = { showInfo = false },
        )
    }
}

private val GaugeBarHeight = 20.dp
private val FaceSize = 44.dp
private val MarkerWidth = 96.dp

// 막대 + 시작점 표시 + 끝의 얼굴
@Composable
private fun GaugeBar(progress: Float, tier: MannerTier, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val trackWidth = maxWidth
        val baseX = trackWidth * mannerFraction(MANNER_BASE)

        Column {
            // 시작점 표시 (▼ 위에 글자)
            Box(Modifier.fillMaxWidth().height(34.dp)) {
                Column(
                    modifier = Modifier.width(MarkerWidth).offset(x = baseX - MarkerWidth / 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("시작점 ${mannerNumber(MANNER_BASE)}°", color = AppColor.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Canvas(Modifier.padding(top = 2.dp).size(width = 10.dp, height = 6.dp)) {
                        val p = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }
                        drawPath(p, Color(0xFF8E8E93))
                    }
                }
            }

            // 막대와 얼굴
            Box(Modifier.fillMaxWidth().height(FaceSize), contentAlignment = Alignment.CenterStart) {
                Canvas(Modifier.fillMaxWidth().height(GaugeBarHeight)) {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(AppColor.ChipNeutralBg, size = size, cornerRadius = r)               // 바탕 막대
                    val fillWidth = (size.width * progress).coerceAtLeast(size.height)
                    // 채운 색은 막대 전체 기준의 그라데이션이라, 온도가 높을수록 더 먼 색까지 보인다
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            0f to GaugeCold, 0.2f to GaugeCool, 0.365f to AppColor.Accent,
                            0.55f to GaugeAmber, 0.75f to GaugeOrange, 1f to GaugeRed,
                            startX = 0f, endX = size.width,
                        ),
                        size = Size(fillWidth, size.height),
                        cornerRadius = r,
                    )
                    // 시작점(36.5°) 눈금
                    val baseX = size.width * mannerFraction(MANNER_BASE)
                    drawLine(Color(0x33000000), Offset(baseX, 3.dp.toPx()), Offset(baseX, size.height - 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                }
                // 얼굴은 채워진 끝에 붙고, 양 끝에서는 막대 밖으로 나가지 않는다
                val faceX = (trackWidth * progress - FaceSize / 2).coerceIn(0.dp, trackWidth - FaceSize)
                MannerFace(tier = tier, size = FaceSize, modifier = Modifier.offset(x = faceX))
            }
        }
    }
}

// 온도에 따라 표정과 얼굴색이 바뀌는 얼굴
@Composable
private fun MannerFace(tier: MannerTier, size: Dp, modifier: Modifier = Modifier) {
    val face = tierColor(tier)
    val ink = Color(0xFF27272A)
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(Color(0x26000000), r, Offset(r, r + 1.5.dp.toPx()))   // 살짝 뜬 그림자
        drawCircle(Color.White, r)                                   // 흰 테두리
        drawCircle(face, r - 3.dp.toPx())
        val eyeY = r * 0.84f
        val eyeDx = r * 0.36f
        val eyeR = r * 0.115f
        drawCircle(ink, eyeR, Offset(r - eyeDx, eyeY))
        drawCircle(ink, eyeR, Offset(r + eyeDx, eyeY))
        // 입: 양 끝을 잇는 곡선. 값이 +면 웃고 -면 찡그린다
        val mouthY = r * 1.20f
        val half = r * 0.40f
        val bend = tier.smile * r * 0.55f
        val mouth = Path().apply {
            moveTo(r - half, mouthY - bend * 0.15f)
            quadraticBezierTo(r, mouthY + bend, r + half, mouthY - bend * 0.15f)
        }
        drawPath(mouth, ink, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
        if (tier.blush) {
            val cheek = Color(0x66FF4D4F)
            drawCircle(cheek, r * 0.15f, Offset(r - r * 0.62f, r * 1.08f))
            drawCircle(cheek, r * 0.15f, Offset(r + r * 0.62f, r * 1.08f))
        }
    }
}

// ⓘ 도움말 표시 (원 + 물음표)
@Composable
private fun HelpMark() {
    Box(
        Modifier.size(18.dp).clip(CircleShape).border(1.5.dp, AppColor.IconMuted, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("?", color = AppColor.IconMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// 단계별 색 (얼굴, 알약 바탕)
private fun tierColor(tier: MannerTier): Color = when (tier) {
    MannerTier.FREEZING -> GaugeCold
    MannerTier.COOL -> GaugeCool
    MannerTier.WARM -> AppColor.Accent
    MannerTier.TOASTY -> GaugeAmber
    MannerTier.HOT -> GaugeOrange
}

// '따뜻해요' 같은 단계 이름을 단계 색으로 옅게 칠한 알약에 담는다
@Composable
private fun TierPill(tier: MannerTier) {
    Box(
        Modifier
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(tierColor(tier).copy(alpha = 0.22f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(tier.label, color = Color(0xFF3F3F46), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private val GaugeCold = Color(0xFF4D8DFF)
private val GaugeCool = Color(0xFF3FC1B0)
private val GaugeAmber = Color(0xFFFFB020)
private val GaugeOrange = Color(0xFFFF7A2F)
private val GaugeRed = Color(0xFFFF4D4F)
