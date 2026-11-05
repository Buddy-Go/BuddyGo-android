package com.example.myapplication.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 화면 아래의 큰 동작 버튼 (신청하기, 로그인 등).
// 16c 제출 중: loading = true이면 회색 배경에 로딩 링과 loadingText를 보여 주고 누를 수 없다.
// 16i 제출 실패: 버튼은 평소 모양 그대로 두고 text만 '다시 시도'로 바꾼다. 오류 문구는 InlineErrorBanner로 화면 위에 보여 준다.
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String = text,
) {
    val active = enabled && !loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(if (active) AppColor.Accent else AppColor.SkeletonBase)
            .clickable(enabled = active, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AppColor.TextDisabled,
                    trackColor = AppColor.Border,
                    strokeWidth = 2.5.dp,
                )
                Spacer(Modifier.width(AppSpace.md))
            }
            Text(
                text = if (loading) loadingText else text,
                color = if (active) AppColor.TextPrimary else AppColor.TextDisabled,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// 16i 입력·제출 실패 안내 띠. 화면 위쪽에 놓고, 복구는 ActionButton('다시 시도')이 맡는다.
@Composable
fun InlineErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.lg))
            .background(AppColor.DestructiveSoftBg)
            .padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val c = AppColor.DestructiveText
            val sw = 1.8.dp.toPx()
            drawCircle(c, size.width / 2 - sw / 2, style = Stroke(sw))
            drawLine(c, Offset(size.width / 2, size.height * .28f), Offset(size.width / 2, size.height * .58f), sw, StrokeCap.Round)
            drawCircle(c, sw * .7f, Offset(size.width / 2, size.height * .74f))
        }
        Spacer(Modifier.width(AppSpace.xl))
        Text(message, color = AppColor.DestructiveText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ActionButtonPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Column(Modifier.padding(AppSpace.x4), verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
            InlineErrorBanner("신청을 보내지 못했어요. 다시 시도해주세요.")
            ActionButton(text = "신청하기", onClick = {})
            ActionButton(text = "다시 시도", onClick = {})
            ActionButton(text = "신청하기", loadingText = "신청하는 중", loading = true, onClick = {})
            ActionButton(text = "신청하기", enabled = false, onClick = {})
        }
    }
}
