package com.example.myapplication.ui.common

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

// 좌우로 밀어서 탭을 바꾸는 제스처.
// 왼쪽으로 밀면 다음 탭(onNext), 오른쪽으로 밀면 이전 탭(onPrevious).
// 자식(가로 스크롤 칩 줄 등)이 이미 소비한 드래그는 무시하고, 세로 스크롤과는 겹치지 않는다.
@Composable
fun Modifier.swipeTabs(onNext: (() -> Unit)?, onPrevious: (() -> Unit)?): Modifier {
    val next = rememberUpdatedState(onNext)
    val previous = rememberUpdatedState(onPrevious)
    val threshold = with(LocalDensity.current) { 60.dp.toPx() }
    return pointerInput(Unit) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragCancel = { total = 0f },
            onDragEnd = {
                if (total <= -threshold) next.value?.invoke()
                else if (total >= threshold) previous.value?.invoke()
                total = 0f
            },
        ) { _, dragAmount -> total += dragAmount }
    }
}
