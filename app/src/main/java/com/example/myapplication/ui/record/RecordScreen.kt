package com.example.myapplication.ui.record

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

private enum class Guide { OneRm, PullUp }

fun bodyPartLabel(part: BodyPart): String = when (part) {
    BodyPart.CHEST -> "가슴"
    BodyPart.BACK -> "등"
    BodyPart.LEGS -> "하체"
    BodyPart.SHOULDER -> "어깨"
    BodyPart.ARM -> "팔"
}

fun skillLevelLabel(level: SkillLevel): String = when (level) {
    SkillLevel.BEGINNER -> "초급"
    SkillLevel.INTERMEDIATE -> "중급"
    SkillLevel.ADVANCED -> "고급"
}

// 2c 종목 기록 · 헬스 (2c, 2c-2, 2c-5). 러닝(2d)·등산(2e)은 별도 PR에서 이 화면에 추가한다.
@Composable
fun RecordScreen(
    onBack: () -> Unit = {},
    viewModel: RecordViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    HealthRecordContent(
        state = state,
        onBack = onBack,
        onTogglePart = viewModel::onTogglePart,
        onStepBench = viewModel::onStepBench,
        onStepPullUp = viewModel::onStepPullUp,
        onStepSquat = viewModel::onStepSquat,
        onStepOverhead = viewModel::onStepOverhead,
        onSelectArm = viewModel::onSelectArm,
        onSave = viewModel::onSave,
    )
}

@Composable
private fun HealthRecordContent(
    state: RecordState,
    onBack: () -> Unit,
    onTogglePart: (BodyPart) -> Unit,
    onStepBench: (Int) -> Unit,
    onStepPullUp: (Int) -> Unit,
    onStepSquat: (Int) -> Unit,
    onStepOverhead: (Int) -> Unit,
    onSelectArm: (SkillLevel) -> Unit,
    onSave: () -> Unit,
) {
    var guide by remember { mutableStateOf<Guide?>(null) }
    Column(Modifier.fillMaxSize().background(AppColor.Surface)) {
        SignUpHeader(step = 2, total = 3, onBack = onBack, title = "헬스 기록")
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpace.x4),
        ) {
            Spacer(Modifier.height(AppSpace.x4))
            Text("어떤 부위를\n주로 하세요?", color = AppColor.TextPrimary, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(AppSpace.xl))
            Text("정확하지 않아도 괜찮아요. 비슷한 수준의 메이트를 찾는 데만 쓰여요", color = AppColor.TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(AppSpace.x4))
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                BodyPart.entries.forEach { part -> PartChip(bodyPartLabel(part), part in state.parts) { onTogglePart(part) } }
            }

            state.selectedParts.forEach { part ->
                Spacer(Modifier.height(AppSpace.x4))
                when (part) {
                    BodyPart.CHEST -> WeightSection(part, state, "벤치프레스", state.benchPressKg, onStepBench) { guide = Guide.OneRm }
                    BodyPart.LEGS -> WeightSection(part, state, "스쿼트", state.squatKg, onStepSquat) { guide = Guide.OneRm }
                    BodyPart.SHOULDER -> WeightSection(part, state, "오버헤드 프레스", state.overheadPressKg, onStepOverhead) { guide = Guide.OneRm }
                    BodyPart.BACK -> PullUpSection(state.pullUpCount, onStepPullUp) { guide = Guide.PullUp }
                    BodyPart.ARM -> ArmSection(state.armLevel, onSelectArm)
                }
            }

            if (state.parts.isNotEmpty()) {
                Spacer(Modifier.height(AppSpace.x4))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfoIcon(AppColor.IconMuted)
                    Spacer(Modifier.width(AppSpace.xl))
                    Text("입력한 무게는 메이트 추천과 모임 필터(비슷한 중량)에 쓰여요", color = AppColor.TextSecondary, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(AppSpace.x5))
        }
        HorizontalDivider(color = AppColor.Border)
        Row(Modifier.fillMaxWidth().padding(AppSpace.x4), horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            // 건너뛰기: 이 화면에서 바꾼 내용을 저장하지 않고 돌아간다.
            ActionButton(text = "건너뛰기", onClick = onBack, outlined = true, modifier = Modifier.width(100.dp))
            ActionButton(text = "저장", onClick = onSave, modifier = Modifier.weight(1f))
        }
    }
    when (guide) {
        Guide.OneRm -> OneRmGuideBottomSheet(onDismiss = { guide = null })
        Guide.PullUp -> PullUpGuideBottomSheet(onDismiss = { guide = null })
        null -> Unit
    }
}

@Composable
private fun PartChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.pill)
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) AppColor.AccentSoft else AppColor.Surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.xxl - 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) AppColor.AccentStrong else AppColor.TextPrimary, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// 부위 제목: 초록 점 + 이름 + (회색 설명) + (i)
@Composable
private fun SectionTitle(name: String, sub: String? = null, onInfo: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(AppColor.Accent))
        Spacer(Modifier.width(AppSpace.xl))
        Text(name, color = AppColor.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (sub != null) {
            Spacer(Modifier.width(AppSpace.md))
            Text(sub, color = AppColor.TextSecondary, fontSize = 14.sp)
        }
        if (onInfo != null) {
            Spacer(Modifier.width(AppSpace.md))
            Box(Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onInfo), contentAlignment = Alignment.Center) { InfoIcon(AppColor.IconMuted) }
        }
    }
}

@Composable
private fun WeightSection(part: BodyPart, state: RecordState, lift: String, kg: Int, onStep: (Int) -> Unit, onInfo: () -> Unit) {
    val first = part == state.firstOneRmPart
    Column {
        if (first) SectionTitle(bodyPartLabel(part), "1RM 기준", onInfo) else SectionTitle(bodyPartLabel(part))
        if (first) {
            Spacer(Modifier.height(AppSpace.md))
            Text("평소 8~10회 드는 무게 × 1.3 정도예요", color = AppColor.TextDisabled, fontSize = 13.sp)
        }
        Spacer(Modifier.height(AppSpace.xl))
        StepperCard(lift, kg, "kg", onMinus = { onStep(-1) }, onPlus = { onStep(1) })
    }
}

@Composable
private fun PullUpSection(count: Int, onStep: (Int) -> Unit, onInfo: () -> Unit) {
    Column {
        SectionTitle("등", "맨몸 턱걸이 기준", onInfo)
        Spacer(Modifier.height(AppSpace.xl))
        StepperCard("턱걸이", count, "회", onMinus = { onStep(-1) }, onPlus = { onStep(1) })
        Spacer(Modifier.height(AppSpace.md))
        Text("한 번에 연속으로 가능한 최대 개수예요", color = AppColor.TextDisabled, fontSize = 13.sp)
    }
}

@Composable
private fun StepperCard(name: String, value: Int, unit: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .border(1.dp, AppColor.Border, shape)
            .padding(horizontal = AppSpace.xxl - 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SportIcon(Sport.HEALTH, AppColor.AccentStrong, size = 20.dp)
        Spacer(Modifier.width(AppSpace.xl))
        Text(name, color = AppColor.TextPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
        StepButton(plus = false, onClick = onMinus)
        Box(Modifier.width(76.dp), contentAlignment = Alignment.Center) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppColor.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)) { append(value.toString()) }
                    withStyle(SpanStyle(color = AppColor.TextSecondary, fontSize = 14.sp)) { append(unit) }
                },
            )
        }
        StepButton(plus = true, onClick = onPlus)
    }
}

@Composable
private fun StepButton(plus: Boolean, onClick: () -> Unit) {
    val bg = if (plus) AppColor.AccentSoft else AppColor.SurfaceMuted
    val fg = if (plus) AppColor.AccentStrong else AppColor.TextSecondary
    Box(Modifier.size(40.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(16.dp)) {
            val sw = 2.2.dp.toPx()
            drawLine(fg, Offset(size.width * .1f, size.height / 2), Offset(size.width * .9f, size.height / 2), sw, StrokeCap.Round)
            if (plus) drawLine(fg, Offset(size.width / 2, size.height * .1f), Offset(size.width / 2, size.height * .9f), sw, StrokeCap.Round)
        }
    }
}

// 팔은 숫자가 아니라 숙련도(초급/중급/고급) 선택
@Composable
private fun ArmSection(selected: SkillLevel?, onSelect: (SkillLevel) -> Unit) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column {
        SectionTitle("팔", "숙련도 기준")
        Spacer(Modifier.height(AppSpace.xl))
        Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, AppColor.Border, shape).padding(AppSpace.xxl)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                SkillLevel.entries.forEach { level ->
                    val on = level == selected
                    val pill = RoundedCornerShape(AppRadius.pill)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(pill)
                            .background(if (on) AppColor.AccentSoft else AppColor.Surface)
                            .border(if (on) 1.5.dp else 1.dp, if (on) AppColor.Accent else AppColor.Border, pill)
                            .clickable { onSelect(level) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(skillLevelLabel(level), color = if (on) AppColor.AccentStrong else AppColor.TextPrimary, fontSize = 16.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            if (selected != null) {
                Spacer(Modifier.height(AppSpace.xl))
                Text(skillLevelDescription(selected), color = AppColor.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

// 고급 문구는 디자인(2c) 그대로, 초급·중급 문구는 같은 형식으로 정한 것이다.
fun skillLevelDescription(level: SkillLevel): String = when (level) {
    SkillLevel.BEGINNER -> "이두·삼두 운동을 시작했거나 기본 동작을 익히고 있는 수준"
    SkillLevel.INTERMEDIATE -> "이두·삼두 운동을 꾸준히 하며 기본 동작을 안정적으로 하는 수준"
    SkillLevel.ADVANCED -> "이두·삼두 운동을 꾸준히 하며 다양한 운동과 중량을 다루는 수준"
}

// 동그라미 안의 i
@Composable
fun InfoIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val sw = 1.6.dp.toPx()
        drawCircle(color, this.size.width / 2 - sw / 2, style = Stroke(sw))
        drawLine(color, Offset(this.size.width / 2, this.size.height * .46f), Offset(this.size.width / 2, this.size.height * .72f), sw, StrokeCap.Round)
        drawCircle(color, sw * .65f, Offset(this.size.width / 2, this.size.height * .30f))
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HealthRecordPreview() {
    MyApplicationTheme(dynamicColor = false) {
        HealthRecordContent(
            state = RecordState(
                parts = setOf(BodyPart.CHEST, BodyPart.BACK, BodyPart.ARM),
                benchPressKg = 80, pullUpCount = 10, armLevel = SkillLevel.ADVANCED,
            ),
            onBack = {}, onTogglePart = {}, onStepBench = {}, onStepPullUp = {}, onStepSquat = {}, onStepOverhead = {}, onSelectArm = {}, onSave = {},
        )
    }
}
