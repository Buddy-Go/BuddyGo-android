package com.example.myapplication.ui.explore.filter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.explore.RefreshIcon
import com.example.myapplication.ui.record.bodyPartLabel
import com.example.myapplication.ui.record.hikingStyleLabel
import com.example.myapplication.ui.record.skillLevelLabel
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 4d 기준 시트 (조건 설정 위에 뜬다). 종목에 따라 내용이 다르다.
//  - 헬스: 4d-After 부위·기준 설정·부위별 수준, 4d-2 기준 꺼짐, 4d-3 부위 미선택
//  - 러닝·등산: 디자인에 시트 프레임이 없어 같은 틀(기준 설정 스위치 + 값)로 만들었다.
// '적용'을 누르면 닫고, 밖을 누르거나 내리면 열기 전 값으로 돌아간다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthFilterBottomSheet(
    state: FilterState,
    onTogglePart: (BodyPart) -> Unit,
    onSetCriteriaOn: (Boolean) -> Unit,
    onResetToMine: () -> Unit,
    onSetRange: (BodyPart, IntRange) -> Unit,
    onSetArmLevel: (SkillLevel) -> Unit,
    onSetPaceRange: (IntRange) -> Unit,
    onSetHikingStyle: (HikingStyle) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.Surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = AppSpace.x4),
                verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
            ) {
                Text("기준", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                when (state.draft.sport) {
                    Sport.HEALTH -> HealthContent(state, onTogglePart, onSetCriteriaOn, onResetToMine, onSetRange, onSetArmLevel)
                    Sport.RUNNING -> RunningContent(state, onSetCriteriaOn, onSetPaceRange)
                    Sport.HIKING -> HikingContent(state, onSetCriteriaOn, onSetHikingStyle)
                    null -> Unit // 종목을 고르기 전에는 기준 줄이 비활성이라 열리지 않는다.
                }
                Spacer(Modifier.height(AppSpace.md))
            }
            ActionButton(
                text = "적용", onClick = onApply,
                modifier = Modifier.padding(horizontal = AppSpace.x4).padding(top = AppSpace.xl, bottom = AppSpace.x4),
            )
        }
    }
}

// ---- 헬스 ----

@Composable
private fun HealthContent(
    state: FilterState,
    onTogglePart: (BodyPart) -> Unit,
    onSetCriteriaOn: (Boolean) -> Unit,
    onResetToMine: () -> Unit,
    onSetRange: (BodyPart, IntRange) -> Unit,
    onSetArmLevel: (SkillLevel) -> Unit,
) {
    val health = state.draft.health
    Text("운동 부위 · 여러 개 선택 가능", fontSize = 15.sp, color = AppColor.TextSecondary)
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        filterPartOrder.forEach { part ->
            ChoiceChip(bodyPartLabel(part), selected = part in health.parts, onClick = { onTogglePart(part) })
        }
    }
    HorizontalDivider(color = AppColor.Border)

    val hasParts = health.parts.isNotEmpty()
    SwitchRow(
        title = "기준 설정",
        subtitle = if (!health.criteriaOn) "켜면 선택한 부위별로 수준을 정할 수 있어요" else null,
        checked = health.criteriaOn,
        enabled = hasParts,
        onCheckedChange = onSetCriteriaOn,
    ) {
        // 부위를 고르고 기준 설정이 켜져 있을 때만 쓸 수 있다.
        ResetLink(enabled = hasParts && health.criteriaOn, onClick = onResetToMine)
    }
    if (!hasParts) {
        Text("운동 부위를 먼저 선택해주세요", fontSize = 15.sp, color = AppColor.TextSecondary)
        return
    }
    if (!health.criteriaOn) return

    filterPartOrder.filter { it in health.parts }.forEach { part ->
        HorizontalDivider(color = AppColor.Border)
        if (part == BodyPart.ARM) {
            ArmLevelSection(health.armLevel, onSetArmLevel)
        } else {
            PartRangeSection(part, health.ranges[part], myValue(part, state.myRecord)) { onSetRange(part, it) }
        }
    }
}

private fun partTitle(part: BodyPart): String = when (part) {
    BodyPart.CHEST -> "가슴 · 벤치프레스 1RM"
    BodyPart.BACK -> "등 · 맨몸 턱걸이"
    BodyPart.LEGS -> "하체 · 스쿼트 1RM"
    BodyPart.SHOULDER -> "어깨 · 오버헤드 프레스 1RM"
    BodyPart.ARM -> "팔 · 숙련도"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartRangeSection(part: BodyPart, range: IntRange?, mine: Int?, onChange: (IntRange) -> Unit) {
    val unit = if (part == BodyPart.BACK) "회" else "kg"
    val max = sliderMax(part)
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(partTitle(part), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            // 내 기록이 없는 부위는 범위를 직접 정하기 전까지 '직접 정하기'로 보인다.
            Text(
                if (range == null) "직접 정하기" else "${range.first} – ${range.last}$unit",
                fontSize = if (range == null) 14.sp else 20.sp, fontWeight = FontWeight.Bold,
                color = if (range == null) AppColor.TextSecondary else AppColor.TextPrimary,
            )
        }
        RangeSlider(
            value = (range?.first ?: 0).toFloat()..(range?.last ?: max).toFloat(),
            onValueChange = { v ->
                // 중량은 5kg 눈금, 턱걸이는 1회 단위로 맞춘다.
                val a = Math.round(v.start); val b = Math.round(v.endInclusive)
                onChange(if (part == BodyPart.BACK) a..b else snapWeight(a)..snapWeight(b))
            },
            valueRange = 0f..max.toFloat(),
            steps = if (part == BodyPart.BACK) max - 1 else max / 5 - 1,
            colors = sliderColors(),
            startThumb = { RangeThumb() },
            endThumb = { RangeThumb() },
        )
        Text(
            if (mine != null) "내 기록 $mine$unit · 비슷한 ${if (part == BodyPart.BACK) "횟수" else "중량대"}만 보기" else "내 기록이 없어요 · 직접 정해 주세요",
            fontSize = 14.sp, color = AppColor.TextSecondary,
        )
    }
}

@Composable
private fun ArmLevelSection(level: SkillLevel?, onChange: (SkillLevel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(partTitle(BodyPart.ARM), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text("중량 기준 없음", fontSize = 14.sp, color = AppColor.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            SkillLevel.entries.forEach { skill ->
                OptionCard(skillLevelLabel(skill), selected = level == skill, onClick = { onChange(skill) }, modifier = Modifier.weight(1f))
            }
        }
        if (level == SkillLevel.INTERMEDIATE) {
            Text("덤벨컬·케이블 푸시다운 등 기본 팔 운동을 혼자 진행할 수 있는 수준", fontSize = 14.sp, color = AppColor.TextSecondary)
        }
    }
}

// ---- 러닝 ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunningContent(state: FilterState, onSetCriteriaOn: (Boolean) -> Unit, onSetPaceRange: (IntRange) -> Unit) {
    val running = state.draft.running
    Text("러닝 · 평균 페이스", fontSize = 15.sp, color = AppColor.TextSecondary)
    SwitchRow(
        title = "기준 설정",
        subtitle = if (!running.criteriaOn) "켜면 비슷한 페이스의 모임만 볼 수 있어요" else null,
        checked = running.criteriaOn, enabled = true, onCheckedChange = onSetCriteriaOn,
    )
    if (!running.criteriaOn) return
    HorizontalDivider(color = AppColor.Border)
    val range = running.paceRangeSec
    val mine = state.myRecord?.runningPaceSec
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("평균 페이스", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                if (range == null) "직접 정하기" else "${paceText(range.first)} – ${paceText(range.last)}",
                fontSize = if (range == null) 14.sp else 20.sp, fontWeight = FontWeight.Bold,
                color = if (range == null) AppColor.TextSecondary else AppColor.TextPrimary,
            )
        }
        val min = CriteriaDefaults.PACE_MIN_SEC
        val max = CriteriaDefaults.PACE_MAX_SEC
        RangeSlider(
            value = (range?.first ?: min).toFloat()..(range?.last ?: max).toFloat(),
            onValueChange = { onSetPaceRange(it.start.toInt()..it.endInclusive.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min) / CriteriaDefaults.PACE_STEP_SEC - 1,
            colors = sliderColors(),
            startThumb = { RangeThumb() },
            endThumb = { RangeThumb() },
        )
        Text(
            if (mine != null) "내 페이스 ${paceText(mine)} / km · 비슷한 페이스만 보기" else "내 기록이 없어요 · 직접 정해 주세요",
            fontSize = 14.sp, color = AppColor.TextSecondary,
        )
    }
}

// ---- 등산 ----

@Composable
private fun HikingContent(state: FilterState, onSetCriteriaOn: (Boolean) -> Unit, onSetStyle: (HikingStyle) -> Unit) {
    val hiking = state.draft.hiking
    Text("등산 · 성향", fontSize = 15.sp, color = AppColor.TextSecondary)
    SwitchRow(
        title = "기준 설정",
        subtitle = if (!hiking.criteriaOn) "켜면 같은 성향의 모임만 볼 수 있어요" else null,
        checked = hiking.criteriaOn, enabled = true, onCheckedChange = onSetCriteriaOn,
    )
    if (!hiking.criteriaOn) return
    HorizontalDivider(color = AppColor.Border)
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
        HikingStyle.entries.forEach { style ->
            OptionCard(hikingStyleLabel(style), selected = hiking.style == style, onClick = { onSetStyle(style) }, modifier = Modifier.weight(1f))
        }
    }
}

// ---- 공용 조각 ----

// 슬라이더: 라임 막대, 눈금 없음, 손잡이는 흰색 바탕에 라임 테두리
@Composable
private fun sliderColors() = SliderDefaults.colors(
    activeTrackColor = AppColor.Accent, inactiveTrackColor = AppColor.ChipNeutralBg,
    activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
)

@Composable
private fun RangeThumb() {
    Box(Modifier.size(28.dp).background(AppColor.Surface, CircleShape).border(3.dp, AppColor.Accent, CircleShape))
}

@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    below: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (enabled) AppColor.TextPrimary else AppColor.TextDisabled)
            if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = if (enabled) AppColor.TextSecondary else AppColor.TextDisabled)
            below()
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppColor.Surface, checkedTrackColor = AppColor.Accent,
                uncheckedThumbColor = AppColor.Surface, uncheckedTrackColor = AppColor.ChipNeutralBg, uncheckedBorderColor = AppColor.ChipNeutralBg,
            ),
        )
    }
}

@Composable
private fun ResetLink(enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) AppColor.AccentStrong else AppColor.TextDisabled
    Row(
        Modifier.clip(RoundedCornerShape(AppRadius.sm)).clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RefreshIcon(color, size = 16.dp)
        Spacer(Modifier.width(AppSpace.md))
        Text("내 기준으로 설정", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(40.dp).clip(RoundedCornerShape(AppRadius.pill)).clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.pill),
        color = if (selected) AppColor.AccentSoft else AppColor.Surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text, fontSize = 16.sp, color = AppColor.TextPrimary)
        }
    }
}

@Composable
internal fun OptionCard(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(52.dp).clip(RoundedCornerShape(AppRadius.pill)).clickable(onClick = onClick),
        shape = RoundedCornerShape(AppRadius.pill),
        color = if (selected) AppColor.AccentSoft else AppColor.Surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border),
    ) {
        Box(contentAlignment = Alignment.Center) { Text(text, fontSize = 17.sp, color = AppColor.TextPrimary) }
    }
}
