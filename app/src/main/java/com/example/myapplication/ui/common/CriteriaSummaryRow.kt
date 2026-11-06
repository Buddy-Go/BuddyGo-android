package com.example.myapplication.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 내 기록과 모임 기준을 비교한 결과 (service-rules 8장). 판정은 서버가 계산하고 앱은 보여 주기만 한다.
enum class CriteriaMatch { Match, Partial, Mismatch }

// 5i 종목 기준 요약 행. 누르면 기준 상세(바텀시트)를 연다.
// match가 null이면 판정 줄을 보여 주지 않는다. (비교할 수 있는 부위가 없을 때)
// statusText를 주지 않으면 판정별 기본 문구를 쓴다. 일부만 맞을 때는 범위 밖 부위 이름을 넣어 준다.
// bordered = false는 모임 상세의 정보 카드 안에서 쓰는 테두리 없는 모양이다.
@Composable
fun CriteriaSummaryRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    match: CriteriaMatch? = null,
    statusText: String? = null,
    bordered: Boolean = true,
) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (bordered) Modifier.clip(shape).border(1.dp, AppColor.Border, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = AppColor.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(summary, color = AppColor.TextPrimary, fontSize = 16.sp)
            if (match != null) {
                Spacer(Modifier.size(2.dp))
                val good = match == CriteriaMatch.Match
                val color = if (good) AppColor.AccentStrong else AppColor.TextSecondary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MatchMark(good, color)
                    Spacer(Modifier.width(AppSpace.md - 2.dp))
                    Text(
                        text = statusText ?: defaultStatus(match),
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = if (good) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.width(AppSpace.xl))
        Chevron()
    }
}

private fun defaultStatus(match: CriteriaMatch) = when (match) {
    CriteriaMatch.Match -> "내 기록 범위 안"
    CriteriaMatch.Partial -> "일부만 맞아요"
    CriteriaMatch.Mismatch -> "내 기록과 맞지 않아요"
}

// 맞음은 체크, 그 외는 줄표
@Composable
private fun MatchMark(good: Boolean, color: Color) {
    Canvas(Modifier.size(12.dp)) {
        val sw = 1.8.dp.toPx()
        if (good) {
            drawLine(color, Offset(size.width * .08f, size.height * .55f), Offset(size.width * .38f, size.height * .85f), sw, StrokeCap.Round)
            drawLine(color, Offset(size.width * .38f, size.height * .85f), Offset(size.width * .95f, size.height * .2f), sw, StrokeCap.Round)
        } else {
            drawLine(color, Offset(size.width * .1f, size.height * .5f), Offset(size.width * .9f, size.height * .5f), sw, StrokeCap.Round)
        }
    }
}

@Composable
private fun Chevron() {
    Canvas(Modifier.size(width = 8.dp, height = 14.dp)) {
        val sw = 1.8.dp.toPx()
        drawLine(AppColor.IconMuted, Offset(0f, 0f), Offset(size.width, size.height / 2), sw, StrokeCap.Round)
        drawLine(AppColor.IconMuted, Offset(size.width, size.height / 2), Offset(0f, size.height), sw, StrokeCap.Round)
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun CriteriaSummaryRowPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Column(Modifier.padding(AppSpace.xxl), verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            CriteriaSummaryRow("헬스 기준", "가슴 70–90kg · 팔 중급", {}, match = CriteriaMatch.Match)
            CriteriaSummaryRow("헬스 기준", "가슴 70–90kg · 팔 중급", {}, match = CriteriaMatch.Partial, statusText = "가슴은 범위 밖이에요")
            CriteriaSummaryRow("헬스 기준", "가슴 70–90kg · 팔 중급", {}, match = CriteriaMatch.Mismatch)
            CriteriaSummaryRow("헬스 기준", "가슴 70–90kg · 팔 중급", {})
            CriteriaSummaryRow("등산 기준", "취미", {}, match = CriteriaMatch.Match, statusText = "내 성향과 맞음", bordered = false)
        }
    }
}
