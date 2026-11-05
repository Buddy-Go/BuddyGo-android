package com.example.myapplication.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.navigation.Routes
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.MyApplicationTheme

// 하단 탭 바: 탐색·활동·개설·메이트·마이
// 선택된 탭은 채워진 라임 아이콘 + 굵은 진한 초록 라벨, 선택 안 된 탭은 회색 윤곽선 아이콘.
// currentRoute가 탭 5개 중 하나가 아니면 아무 탭도 선택되지 않는다.
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabClick: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    mateBadge: Boolean = false, // 메이트 신청이 와 있을 때 메이트 탭에 점 표시
) {
    // 배경은 시스템 내비게이션 영역(홈 막대)까지 이어지고, 탭은 그 위에 놓인다.
    Column(modifier.background(AppColor.Surface).navigationBarsPadding()) {
        HorizontalDivider(color = AppColor.Border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabClick(tab.route) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TabIcon(tab, selected, showBadge = tab.route == Routes.MATE && mateBadge)
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            selected -> AppColor.AccentStrong
                            tab.route == Routes.MEETING_CREATE -> AppColor.TextPrimary
                            else -> AppColor.TextSecondary
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

private class Tab(val route: String, val label: String, val draw: DrawScope.(selected: Boolean) -> Unit)

private val tabs = listOf(
    Tab(Routes.EXPLORE, "탐색") { drawPin(it) },
    Tab(Routes.ACTIVITY, "활동") { drawCalendar(it) },
    Tab(Routes.MEETING_CREATE, "개설") { drawPlus(it) },
    Tab(Routes.MATE, "메이트") { drawPeople(it) },
    Tab(Routes.MYPAGE, "마이") { drawPerson(it) },
)

@Composable
private fun TabIcon(tab: Tab, selected: Boolean, showBadge: Boolean) {
    if (tab.route == Routes.MEETING_CREATE) {
        // 개설은 알약 배경. 선택되면 라임색, 아니면 연한 회색.
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) AppColor.Accent else AppColor.SurfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(20.dp)) { tab.draw(this, selected) }
        }
    } else {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(26.dp)) { tab.draw(this, selected) }
            if (showBadge) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppColor.Accent),
                )
            }
        }
    }
}

// ---- 아이콘 (Material 아이콘 의존성이 없어 Canvas로 그림. SVG 에셋이 생기면 교체) ----

private fun DrawScope.iconColor(selected: Boolean) = if (selected) AppColor.Accent else AppColor.IconMuted
private fun DrawScope.strokeWidth() = 2.dp.toPx()

private fun DrawScope.drawPin(selected: Boolean) {
    val w = size.width
    val h = size.height
    val c = iconColor(selected)
    val path = Path().apply {
        moveTo(w * .5f, h * .95f)
        cubicTo(w * .22f, h * .68f, w * .18f, h * .52f, w * .18f, h * .40f)
        cubicTo(w * .18f, h * .22f, w * .32f, h * .08f, w * .5f, h * .08f)
        cubicTo(w * .68f, h * .08f, w * .82f, h * .22f, w * .82f, h * .40f)
        cubicTo(w * .82f, h * .52f, w * .78f, h * .68f, w * .5f, h * .95f)
        close()
    }
    if (selected) {
        drawPath(path, c)
        drawCircle(Color.White, w * .11f, Offset(w * .5f, h * .40f))
    } else {
        drawPath(path, c, style = Stroke(strokeWidth()))
        drawCircle(c, w * .11f, Offset(w * .5f, h * .40f), style = Stroke(strokeWidth()))
    }
}

private fun DrawScope.drawCalendar(selected: Boolean) {
    val w = size.width
    val h = size.height
    val c = iconColor(selected)
    val sw = strokeWidth()
    val topLeft = Offset(w * .12f, h * .18f)
    val boxSize = Size(w * .76f, h * .70f)
    val radius = CornerRadius(w * .12f)
    if (selected) {
        drawRoundRect(c, topLeft, boxSize, radius)
        drawLine(Color.White, Offset(w * .32f, h * .56f), Offset(w * .45f, h * .69f), sw, StrokeCap.Round)
        drawLine(Color.White, Offset(w * .45f, h * .69f), Offset(w * .69f, h * .45f), sw, StrokeCap.Round)
    } else {
        drawRoundRect(c, topLeft, boxSize, radius, style = Stroke(sw))
        drawLine(c, Offset(w * .32f, h * .56f), Offset(w * .45f, h * .69f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * .45f, h * .69f), Offset(w * .69f, h * .45f), sw, StrokeCap.Round)
    }
    // 위쪽 고리 두 개
    drawLine(c, Offset(w * .33f, h * .08f), Offset(w * .33f, h * .26f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * .67f, h * .08f), Offset(w * .67f, h * .26f), sw, StrokeCap.Round)
}

private fun DrawScope.drawPlus(selected: Boolean) {
    val w = size.width
    val h = size.height
    // 개설 아이콘은 알약 위에 그려지므로 선택(라임 배경)이면 흰색, 아니면 회색
    val c = if (selected) Color.White else AppColor.IconMuted
    val sw = 2.dp.toPx()
    drawLine(c, Offset(w * .5f, h * .15f), Offset(w * .5f, h * .85f), sw, StrokeCap.Round)
    drawLine(c, Offset(w * .15f, h * .5f), Offset(w * .85f, h * .5f), sw, StrokeCap.Round)
}

// 사람 한 명: 머리(원) + 어깨(위쪽 반원)
private fun DrawScope.person(c: Color, selected: Boolean, cx: Float, headY: Float, headR: Float, bodyW: Float, baseY: Float) {
    val sw = strokeWidth()
    val bodyTop = headY + headR + size.height * .06f
    val bodyRect = Offset(cx - bodyW / 2, bodyTop) to Size(bodyW, (baseY - bodyTop) * 2)
    if (selected) {
        drawCircle(c, headR, Offset(cx, headY))
        drawArc(c, 180f, 180f, true, bodyRect.first, bodyRect.second)
    } else {
        drawCircle(c, headR, Offset(cx, headY), style = Stroke(sw))
        drawArc(c, 180f, 180f, false, bodyRect.first, bodyRect.second, style = Stroke(sw))
    }
}

private fun DrawScope.drawPerson(selected: Boolean) {
    val w = size.width
    val h = size.height
    person(iconColor(selected), selected, w * .5f, h * .30f, w * .18f, w * .66f, h * .90f)
}

private fun DrawScope.drawPeople(selected: Boolean) {
    val w = size.width
    val h = size.height
    val c = iconColor(selected)
    person(c, selected, w * .66f, h * .27f, w * .14f, w * .46f, h * .66f)
    person(c, selected, w * .36f, h * .38f, w * .17f, w * .56f, h * .90f)
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun BottomNavBarPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Column {
            BottomNavBar(currentRoute = Routes.EXPLORE, onTabClick = {})
            BottomNavBar(currentRoute = Routes.ACTIVITY, onTabClick = {})
            BottomNavBar(currentRoute = Routes.MEETING_CREATE, onTabClick = {})
            BottomNavBar(currentRoute = Routes.MATE, onTabClick = {}, mateBadge = true)
            BottomNavBar(currentRoute = Routes.MYPAGE, onTabClick = {})
        }
    }
}
