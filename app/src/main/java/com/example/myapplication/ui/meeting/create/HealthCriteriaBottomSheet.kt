package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 헬스 기준 시트 ← 10d-After 기록 표시, 10d-2 기록 숨김, 10d 보조(기록 없음·일부만 있음)
// 고르는 동안의 값은 시트 안에만 두고, '적용'을 눌러야 화면에 반영된다.
@Composable
fun HealthCriteriaBottomSheet(
    initial: HealthCriteria,
    onApply: (parts: List<BodyPart>, showMyRecord: Boolean) -> Unit,
    onDismiss: () -> Unit,
    // 기록 입력·수정하러 프로필 기록 화면으로 간다. 고른 부위는 넘겨서 돌아와도 유지한다.
    onEditRecord: (parts: List<BodyPart>, showMyRecord: Boolean) -> Unit = { _, _ -> },
) {
    var parts by remember { mutableStateOf(initial.parts.toSet()) }
    var showMyRecord by remember { mutableStateOf(initial.showMyRecord) }

    CreateSheetFrame(onDismiss = onDismiss) {
        HealthCriteriaSheetBody(
            parts = parts,
            showMyRecord = showMyRecord,
            myRecord = initial.myRecord,
            onPartToggle = { part -> parts = if (part in parts) parts - part else parts + part },
            onShowMyRecordToggle = { showMyRecord = !showMyRecord },
            onApply = { onApply(HEALTH_PART_ORDER.filter { it in parts }, showMyRecord) },
            onEditRecord = { onEditRecord(HEALTH_PART_ORDER.filter { it in parts }, showMyRecord) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HealthCriteriaSheetBody(
    parts: Set<BodyPart>,
    showMyRecord: Boolean,
    myRecord: ExerciseRecord,
    onPartToggle: (BodyPart) -> Unit,
    onShowMyRecordToggle: () -> Unit,
    onApply: () -> Unit,
    onEditRecord: () -> Unit = {},
) {
    val hasAnyRecord = HEALTH_PART_ORDER.any { myRecord.textFor(it) != null }
    // 기록이 하나도 없으면 토글을 끈 채로 비활성 (10d 보조)
    val recordOn = showMyRecord && hasAnyRecord
    val shownRecords = HEALTH_PART_ORDER.filter { it in parts && myRecord.textFor(it) != null }
    // 고른 부위 중 기록이 없는 부위. 무게를 입력하도록 이끈다.
    val missingRecords = HEALTH_PART_ORDER.filter { it in parts && myRecord.textFor(it) == null }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
        SheetTitle(title = "헬스 기준", subtitle = "모임에서 할 운동과 내 기록을 알려주세요")
        SheetSectionLabel("운동 부위 · 여러 개 선택 가능")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
            verticalArrangement = Arrangement.spacedBy(AppSpace.md),
        ) {
            HEALTH_PART_ORDER.forEach { part ->
                PartChip(label = part.label, selected = part in parts, onClick = { onPartToggle(part) })
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColor.DividerSoft),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpace.xxs),
            ) {
                Text(
                    text = "내 기록 표시하기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasAnyRecord) AppColor.TextPrimary else AppColor.TextDisabled,
                )
                Text(
                    text = "찾는 사람이 수준을 참고할 수 있어요. 참여 조건은 아니에요\n검색 조건에도 이용돼요",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColor.TextSecondary,
                )
            }
            PillToggle(checked = recordOn, enabled = hasAnyRecord, onToggle = onShowMyRecordToggle)
        }

        when {
            !hasAnyRecord -> RecordLinkRow(
                hint = "기록(무게)을 입력하면 비슷한 수준의 메이트가 찾기 쉬워져요",
                linkText = "기록 입력하기",
                onClick = onEditRecord,
            )
            recordOn && shownRecords.isNotEmpty() -> {
                Column {
                    shownRecords.forEachIndexed { index, part ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(AppColor.DividerSoft),
                            )
                        }
                        RecordRow(label = "${part.label} · ${part.recordLabel}", value = myRecord.textFor(part).orEmpty())
                    }
                }
                HintText("프로필 기록이에요. 모임을 연 뒤 기록을 바꿔도 이 모임에는 그대로 보여요")
            }
        }
        if (hasAnyRecord) {
            RecordLinkRow(
                hint = if (missingRecords.isNotEmpty()) "${missingRecords.joinToString(" · ") { it.label }} 기록이 아직 없어요" else "무게가 바뀌었다면 기록을 고쳐 주세요",
                linkText = if (missingRecords.isNotEmpty()) "기록 입력하기" else "기록 수정하기",
                onClick = onEditRecord,
            )
        }

        if (parts.isEmpty()) {
            // 4d-3과 같은 안내. 부위는 1개 이상 필수 (service-rules 4장)
            Text(
                text = "운동 부위를 먼저 선택해주세요",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextBody,
            )
        }
        SheetOutlineButton(text = "적용", enabled = parts.isNotEmpty(), onClick = onApply)
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
            .border(1.dp, if (selected) AppColor.Accent else AppColor.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) AppColor.AccentStrong else AppColor.TextBody,
        )
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.TextBody,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
    }
}

// 기록 입력·수정으로 이끄는 한 줄: 왼쪽 안내, 오른쪽 '기록 입력하기 >'
@Composable
private fun RecordLinkRow(hint: String, linkText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
    ) {
        Text(
            text = hint,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            color = AppColor.TextSecondary,
        )
        Text(
            text = "$linkText >",
            modifier = Modifier
                .clip(RoundedCornerShape(AppRadius.pill))
                .background(AppColor.AccentSoft)
                .clickable(onClick = onClick)
                .padding(horizontal = AppSpace.xl, vertical = AppSpace.md),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.AccentStrong,
        )
    }
}

@Composable
private fun HintText(text: String) {
    Text(text = text, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
}
