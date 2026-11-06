package com.example.myapplication.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 화면 전체를 덮는 오류 상태. 뒤로 가기 화살표는 화면이 그린다.
// 16f NETWORK: 다시 시도만. 16g SERVER: 다시 시도 + 홈으로 (onHome이 null이면 홈으로는 숨김)
@Composable
fun FullScreenError(
    type: ErrorType,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onHome: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .padding(horizontal = AppSpace.x5 + AppSpace.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (type) {
            ErrorType.NETWORK -> WifiOffIcon()
            ErrorType.SERVER -> ExclamationIcon(AppColor.TextDisabled, size = 38.dp, strokeWidth = 2.5.dp)
        }
        Spacer(Modifier.height(AppSpace.x5))
        Text(
            text = if (type == ErrorType.NETWORK) "네트워크에 연결할 수 없어요" else "일시적인 오류가 발생했어요",
            color = AppColor.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpace.xl))
        Text(
            text = if (type == ErrorType.NETWORK) "인터넷 연결을 확인하고\n다시 시도해주세요" else "잠시 후 다시 시도해주세요",
            color = AppColor.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpace.x5 + AppSpace.md))
        ActionButton(text = "다시 시도", onClick = onRetry)
        if (type == ErrorType.SERVER && onHome != null) {
            Spacer(Modifier.height(AppSpace.xl))
            ActionButton(text = "홈으로", onClick = onHome, outlined = true)
        }
    }
}

// 끊어진 와이파이 아이콘 (Material 아이콘 의존성이 없어 Canvas로 그림)
@Composable
private fun WifiOffIcon() {
    Canvas(Modifier.size(38.dp)) {
        val c = AppColor.TextDisabled
        val sw = 2.5.dp.toPx()
        val w = size.width
        val h = size.height
        val center = Offset(w * .5f, h * .82f)
        listOf(.22f, .42f, .62f).forEach { r ->
            val radius = w * r
            drawArc(
                c, 225f, 90f, false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        drawCircle(c, sw * .8f, center)
        // 대각선
        drawLine(c, Offset(w * .12f, h * .12f), Offset(w * .88f, h * .88f), sw, StrokeCap.Round)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FullScreenErrorNetworkPreview() {
    MyApplicationTheme(dynamicColor = false) { FullScreenError(ErrorType.NETWORK, onRetry = {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun FullScreenErrorServerPreview() {
    MyApplicationTheme(dynamicColor = false) { FullScreenError(ErrorType.SERVER, onRetry = {}, onHome = {}) }
}
