package com.example.myapplication.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius

// 로딩 중 자리를 채우는 회색 블록 (C-26). 밝은 띠가 좌우로 지나간다.
// 크기는 modifier로 정한다. 예: SkeletonBlock(Modifier.width(120.dp).height(16.dp))
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.sm),
) {
    val progress by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "skeletonProgress",
    )
    Box(
        modifier
            .clip(shape)
            .drawBehind {
                val band = size.width * 0.6f
                val center = -band + (size.width + 2 * band) * progress
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(AppColor.SkeletonBase, AppColor.SkeletonShimmer, AppColor.SkeletonBase),
                        startX = center - band / 2,
                        endX = center + band / 2,
                    ),
                )
            },
    )
}

// 원형 블록 (아바타 자리)
@Composable
fun SkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    SkeletonBlock(modifier.size(size), CircleShape)
}
