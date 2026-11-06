package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.explore.filter.filterPartOrder
import com.example.myapplication.ui.explore.filter.myValue
import com.example.myapplication.ui.explore.filter.paceText
import com.example.myapplication.ui.record.bodyPartLabel
import com.example.myapplication.ui.record.hikingStyleLabel
import com.example.myapplication.ui.record.skillLevelLabel
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 5f 등산 기준, 5h 헬스 기준 시트 (모임 상세 위에 뜬다). 호스트가 정한 기준을 읽기만 하고, 내 기록과 비교한 안내가 붙는다.
// 러닝은 디자인 프레임이 없어 같은 틀로 만들었다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriteriaBottomSheet(
    meeting: Meeting,
    myRecord: ExerciseRecord?,
    onAddRecord: (Sport) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.Surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = AppSpace.x4),
                verticalArrangement = Arrangement.spacedBy(AppSpace.xl),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                    Text(sportCriteriaTitle(meeting.sport), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text("호스트가 설정 · 읽기 전용", fontSize = 15.sp, color = AppColor.TextSecondary, modifier = Modifier.padding(bottom = 3.dp))
                }
                when (val c = meeting.criteria) {
                    is MeetingCriteria.Health -> HealthCriteria(c, myRecord) { onAddRecord(Sport.HEALTH) }
                    is MeetingCriteria.Hiking -> HikingCriteria(c.style, myRecord) { onAddRecord(Sport.HIKING) }
                    is MeetingCriteria.Running -> RunningCriteria(c, myRecord) { onAddRecord(Sport.RUNNING) }
                }
            }
            ActionButton(
                text = "확인", onClick = onDismiss,
                modifier = Modifier.padding(horizontal = AppSpace.x4).padding(top = AppSpace.xl, bottom = AppSpace.x4),
            )
        }
    }
}

// ---- 헬스 (5h) ----

@Composable
private fun HealthCriteria(c: MeetingCriteria.Health, myRecord: ExerciseRecord?, onAddRecord: () -> Unit) {
    Text(
        "부위 순서 고정 · " + filterPartOrder.joinToString(" · ") { bodyPartLabel(it) },
        fontSize = 15.sp, color = AppColor.TextSecondary,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        filterPartOrder.forEach { part -> ReadOnlyChip(bodyPartLabel(part), part in c.parts) }
    }
    filterPartOrder.filter { it in c.parts }.forEach { part ->
        if (part == BodyPart.ARM) ArmCard(c.hostRecord, myRecord, onAddRecord)
        else RangeCard(part, c.hostRecord, myRecord, onAddRecord)
    }
}

private fun partTitle(part: BodyPart): String = when (part) {
    BodyPart.CHEST -> "가슴 · 벤치프레스 1RM"
    BodyPart.BACK -> "등 · 맨몸 턱걸이"
    BodyPart.LEGS -> "하체 · 스쿼트 1RM"
    BodyPart.SHOULDER -> "어깨 · 오버헤드 프레스 1RM"
    BodyPart.ARM -> "팔 · 숙련도"
}

@Composable
private fun RangeCard(part: BodyPart, host: ExerciseRecord?, mine: ExerciseRecord?, onAddRecord: () -> Unit) {
    val range = hostRangeOf(part, host)
    val unit = unitOf(part)
    val myValue = myValue(part, mine)
    val verdict = verdictOf(part, host, mine)
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(partTitle(part), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                if (range == null) "비공개" else "${range.first} – ${range.last}$unit",
                fontSize = if (range == null) 15.sp else 22.sp, fontWeight = FontWeight.Medium,
                color = if (range == null) AppColor.TextSecondary else AppColor.TextPrimary,
            )
        }
        if (range != null) {
            val scale = barScaleOf(range, myValue, part)
            RangeBar(range, myValue, scale)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${scale.min}$unit", fontSize = 13.sp, color = AppColor.TextSecondary)
                Text("${scale.max}$unit", fontSize = 13.sp, color = AppColor.TextSecondary)
            }
        }
        NoteRow(
            text = partNote(part, host, mine),
            good = verdict == PartVerdict.INSIDE,
            action = if (verdict == PartVerdict.NO_MY_RECORD) ("기록 추가" to onAddRecord) else null,
        )
    }
}

// 호스트 범위(라임 구간)와 내 기록(검은 눈금)을 한 줄 막대에 보여 준다. 읽기 전용이다.
@Composable
private fun RangeBar(range: IntRange, mine: Int?, scale: BarScale) {
    val span = (scale.max - scale.min).coerceAtLeast(1).toFloat()
    Canvas(Modifier.fillMaxWidth().height(24.dp)) {
        val h = 8.dp.toPx()
        val top = (size.height - h) / 2
        fun x(v: Int) = ((v - scale.min) / span).coerceIn(0f, 1f) * size.width
        drawRoundRect(AppColor.ChipNeutralBg, Offset(0f, top), androidx.compose.ui.geometry.Size(size.width, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
        drawRoundRect(
            AppColor.Accent, Offset(x(range.first), top),
            androidx.compose.ui.geometry.Size((x(range.last) - x(range.first)).coerceAtLeast(h), h), androidx.compose.ui.geometry.CornerRadius(h / 2),
        )
        if (mine != null) drawLine(AppColor.TextPrimary, Offset(x(mine), 2.dp.toPx()), Offset(x(mine), size.height - 2.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun ArmCard(host: ExerciseRecord?, mine: ExerciseRecord?, onAddRecord: () -> Unit) {
    val hostLevel = host?.armLevel
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(partTitle(BodyPart.ARM), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text(hostLevel?.let(::skillLevelLabel) ?: "비공개", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            SkillLevel.entries.forEach { level -> ReadOnlyOption(skillLevelLabel(level), level == hostLevel, Modifier.weight(1f)) }
        }
        if (hostLevel != null) Text(armLevelDescription(hostLevel), fontSize = 14.sp, color = AppColor.TextSecondary, lineHeight = 21.sp)
        NoteRow(
            text = armNote(host, mine),
            good = armMatches(host, mine) == true,
            action = if (mine?.armLevel == null) ("기록 추가" to onAddRecord) else null,
        )
    }
}

// ---- 등산 (5f) ----

@Composable
private fun HikingCriteria(host: HikingStyle, myRecord: ExerciseRecord?, onAddRecord: () -> Unit) {
    Text("등산 성향", fontSize = 15.sp, color = AppColor.TextSecondary)
    HikingStyle.entries.forEach { style ->
        val selected = style == host
        Surface(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(AppRadius.lg),
            color = if (selected) AppColor.AccentSoft else AppColor.Surface,
            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border),
        ) {
            Row(Modifier.padding(AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                    Text(hikingStyleLabel(style), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text(hikingStyleDescription(style), fontSize = 14.sp, color = AppColor.TextSecondary, lineHeight = 21.sp)
                }
                if (selected) CheckMark(AppColor.AccentStrong)
            }
        }
    }
    val note = hikingNote(host, myRecord)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.md)).background(AppColor.SurfaceMuted).padding(AppSpace.xxl)) {
        NoteRow(
            text = note ?: "내 등산 성향을 아직 안 적었어요",
            good = myRecord?.hikingStyle == host,
            action = if (note == null) ("기록 추가" to onAddRecord) else null,
            useDash = false,
        )
    }
}

// ---- 러닝 (디자인 프레임 없음) ----

@Composable
private fun RunningCriteria(c: MeetingCriteria.Running, myRecord: ExerciseRecord?, onAddRecord: () -> Unit) {
    val margin = com.example.myapplication.ui.explore.filter.CriteriaDefaults.PACE_MARGIN_SEC
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("거리", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text("${c.distanceKm.toString().removeSuffix(".0")}km", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
        }
    }
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("평균 페이스", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text("${paceText(c.hostPaceSec - margin)} – ${paceText(c.hostPaceSec + margin)}", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
        }
        NoteRow(
            text = runningNote(c.hostPaceSec, myRecord),
            good = myRecord?.runningPaceSec?.let { it in (c.hostPaceSec - margin)..(c.hostPaceSec + margin) } == true,
            action = if (myRecord?.runningPaceSec == null) ("기록 추가" to onAddRecord) else null,
        )
    }
}

// ---- 공용 조각 ----

@Composable
private fun CardBox(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.lg)).background(AppColor.SurfaceMuted).padding(AppSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpace.xl),
    ) { content() }
}

@Composable
private fun ReadOnlyChip(text: String, selected: Boolean) {
    Surface(
        modifier = Modifier.height(40.dp), shape = RoundedCornerShape(AppRadius.pill),
        color = if (selected) AppColor.AccentSoft else AppColor.Surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { Text(text, fontSize = 16.sp, color = AppColor.TextPrimary) }
    }
}

@Composable
private fun ReadOnlyOption(text: String, selected: Boolean, modifier: Modifier) {
    Surface(
        modifier = modifier.height(48.dp), shape = RoundedCornerShape(AppRadius.pill),
        color = if (selected) AppColor.AccentSoft else AppColor.Surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border),
    ) { Box(contentAlignment = Alignment.Center) { Text(text, fontSize = 16.sp, color = AppColor.TextPrimary) } }
}

// 내 기록과의 비교 한 줄. 맞으면 체크, 아니면 줄표(또는 안내 아이콘 없이 글자만)
@Composable
private fun NoteRow(text: String, good: Boolean, action: Pair<String, () -> Unit>?, useDash: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val color = if (good) AppColor.AccentStrong else AppColor.TextSecondary
        if (good) CheckMark(color) else if (useDash) Box(Modifier.width(10.dp).height(2.dp).background(color))
        if (good || useDash) Spacer(Modifier.width(AppSpace.md))
        Text(text, fontSize = 14.sp, color = color, fontWeight = if (good) FontWeight.Medium else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action.first, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary, textAlign = TextAlign.End,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(role = Role.Button, onClick = action.second).padding(AppSpace.md),
            )
        }
    }
}

@Composable
private fun CheckMark(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val sw = 2.dp.toPx()
        drawLine(color, Offset(size.width * .1f, size.height * .55f), Offset(size.width * .4f, size.height * .85f), sw, StrokeCap.Round)
        drawLine(color, Offset(size.width * .4f, size.height * .85f), Offset(size.width * .92f, size.height * .2f), sw, StrokeCap.Round)
    }
}
