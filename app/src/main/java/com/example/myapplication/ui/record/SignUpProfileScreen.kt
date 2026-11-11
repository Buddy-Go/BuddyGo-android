package com.example.myapplication.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.SportSupport
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.common.InlineErrorBanner
import com.example.myapplication.ui.common.sportLabel
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 2b 회원가입 · 프로필 설정 (2 / 3)
// 디자인의 '활동 지역' 카드는 서비스 규칙(1장, 13장)에서 쓰임새가 정해지지 않아 제거하기로 해서 넣지 않았다.
@Composable
fun SignUpProfileScreen(
    onBack: () -> Unit = {},
    onEditRecord: (Sport) -> Unit = {},
    onComplete: () -> Unit = {},
    viewModel: SignUpProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.completed) { if (state.completed) onComplete() }
    SignUpProfileContent(
        state = state,
        onBack = onBack,
        onToggleSport = viewModel::onToggleSport,
        onEditRecord = onEditRecord,
        onSkip = { viewModel.onSubmit(skip = true) },
        onSubmit = { viewModel.onSubmit(skip = false) },
    )
}

@Composable
private fun SignUpProfileContent(
    state: SignUpProfileState,
    onBack: () -> Unit,
    onToggleSport: (Sport) -> Unit,
    onEditRecord: (Sport) -> Unit,
    onSkip: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppColor.Surface)) {
        SignUpHeader(step = 2, total = 3, onBack = onBack, title = "프로필 설정")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpace.x4),
        ) {
            if (state.error) {
                Spacer(Modifier.height(AppSpace.xl))
                InlineErrorBanner("가입하지 못했어요. 다시 시도해주세요.")
            }
            Spacer(Modifier.height(AppSpace.x4))
            Text("어떤 운동을\n좋아하세요?", color = AppColor.TextPrimary, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(AppSpace.xl))
            Text("나중에 언제든 수정할 수 있어요", color = AppColor.TextSecondary, fontSize = 15.sp)

            Spacer(Modifier.height(AppSpace.x5))
            Text("선호 종목 · 복수 선택", color = AppColor.TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(AppSpace.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                Sport.entries.forEach { sport ->
                    SportCard(
                        sport = sport,
                        selected = sport in state.sports,
                        enabled = SportSupport.isSupported(sport), // 서버가 받지 않는 종목은 선호 종목·기록이 저장되지 않아 막는다
                        onClick = { onToggleSport(sport) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.selectedSports.isNotEmpty()) {
                Spacer(Modifier.height(AppSpace.x5))
                Text("선택한 종목의 기록 입력", color = AppColor.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(AppSpace.xl))
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                    state.selectedSports.forEach { sport ->
                        RecordRow(
                            sport = sport,
                            done = state.isRecordDone(sport),
                            statusText = state.recordStatusText(sport),
                            onClick = { onEditRecord(sport) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(AppSpace.x5))
        }
        HorizontalDivider(color = AppColor.Border)
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpace.x4),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
        ) {
            ActionButton(
                text = "건너뛰기",
                onClick = onSkip,
                outlined = true,
                enabled = !state.submitting,
                modifier = Modifier.width(100.dp),
            )
            ActionButton(
                text = "가입 완료",
                onClick = onSubmit,
                loading = state.submitting,
                loadingText = "가입하는 중",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SportCard(sport: Sport, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(AppRadius.lg)
    val tint = if (selected) AppColor.AccentStrong else AppColor.IconMuted
    Column(
        modifier = modifier
            .height(94.dp)
            .clip(shape)
            .alpha(if (enabled) 1f else 0.45f)
            .background(if (selected) AppColor.AccentSoft else AppColor.Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SportIcon(sport, tint)
        Spacer(Modifier.height(if (enabled) AppSpace.xl else AppSpace.md))
        Text(
            text = sportLabel(sport),
            color = if (selected) AppColor.AccentStrong else AppColor.TextPrimary,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        if (!enabled) Text("준비 중", color = AppColor.TextSecondary, fontSize = 11.sp)
    }
}

// 입력 완료는 실선, 미입력은 점선 테두리
@Composable
private fun RecordRow(sport: Sport, done: Boolean, statusText: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .then(if (done) Modifier.border(1.dp, AppColor.Border, shape) else Modifier.dashedBorder(AppColor.Border, AppRadius.lg, 1.5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(AppColor.Accent))
        Spacer(Modifier.width(AppSpace.xl))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = AppColor.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)) { append(sportLabel(sport)) }
                withStyle(SpanStyle(color = AppColor.TextSecondary, fontSize = 15.sp)) { append(" · $statusText") }
            },
            modifier = Modifier.weight(1f),
        )
        Text(if (done) "수정" else "입력", color = AppColor.AccentStrong, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Modifier.dashedBorder(color: Color, radius: Dp, strokeWidth: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = strokeWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SignUpProfilePreview() {
    MyApplicationTheme(dynamicColor = false) {
        SignUpProfileContent(
            state = SignUpProfileState(
                sports = setOf(Sport.HEALTH, Sport.HIKING),
                record = ExerciseRecord(benchPressKg = 80),
            ),
            onBack = {}, onToggleSport = {}, onEditRecord = {}, onSkip = {}, onSubmit = {},
        )
    }
}
