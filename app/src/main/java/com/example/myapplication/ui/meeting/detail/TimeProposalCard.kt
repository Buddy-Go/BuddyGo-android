package com.example.myapplication.ui.meeting.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.TimeProposal
import com.example.myapplication.data.model.TimeProposalStatus
import com.example.myapplication.data.model.TimeProposalVote
import com.example.myapplication.data.model.VoteDecision
import com.example.myapplication.ui.common.ConfirmDialog
import com.example.myapplication.ui.meeting.create.CreateSheetFrame
import com.example.myapplication.ui.meeting.create.SheetSectionLabel
import com.example.myapplication.ui.meeting.create.SheetTitle
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 시작 시각 변경 제안 (서버 PR #43). 모임 상세 위쪽의 진행 중 제안 카드, 시각 선택 시트, 거절·철회 확인창.

// ---------- 진행 중인 제안 카드 ----------

// 처음에는 펼쳐 두고, 머리 줄을 누르면 접고 펼친다. 접으면 제목과 한 줄 요약만 남는다.
@Composable
fun TimeProposalCard(
    view: TimeProposalCardView,
    submitting: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggle: () -> Unit = {},
) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColor.AccentSoft)
            .border(1.dp, AppColor.Accent, shape)
            .animateContentSize(),
    ) {
        // 머리: 시계 아이콘 + 제목 (+ 접었을 때 요약) + 접기/펼치기 화살표. 줄 전체가 눌린다.
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = if (expanded) "제안 접기" else "제안 펼치기", onClick = onToggle)
                .padding(start = AppSpace.xxl, end = AppSpace.xl, top = AppSpace.xxl, bottom = if (expanded) AppSpace.xl else AppSpace.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 아이콘은 제목 줄에 맞춘다 (접었을 때 요약 줄이 붙어도 가운데로 내려오지 않게)
            Box(Modifier.align(Alignment.Top).height(21.dp), contentAlignment = Alignment.Center) { ClockIcon(AppColor.AccentStrong, 18.dp) }
            Spacer(Modifier.width(AppSpace.sm))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpace.xxs)) {
                Text(view.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                if (!expanded) Text(view.summary, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
            }
            Spacer(Modifier.width(AppSpace.md))
            com.example.myapplication.ui.explore.filter.ChevronRightIcon(
                AppColor.TextSecondary,
                modifier = Modifier.rotate(if (expanded) -90f else 90f),
            )
        }
        if (expanded) Column(
            Modifier.padding(start = AppSpace.xxl, end = AppSpace.xxl, bottom = AppSpace.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xl),
        ) {
        // 기존 → 제안 시각
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.md)).background(AppColor.Surface).padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    view.fromText, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AppColor.TextDisabled,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                )
                Spacer(Modifier.width(AppSpace.md))
                ArrowIcon(AppColor.TextSecondary)
                Spacer(Modifier.width(AppSpace.md))
                Text(view.toText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            }
            Text("시작 시각 ${view.shiftText} · 끝나는 시각도 함께 바뀌어요", fontSize = 12.sp, color = AppColor.TextSecondary)
        }

        // 응답 현황(몇 명이 수락했는지만)과 안내를 한 묶음으로
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
            ProgressLine(view.progressText, view.deadlineText)
            if (view.note != null) {
                Text(view.note, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
            }
        }

        when (view.actions) {
            ProposalCardActions.RESPOND -> Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                CardButton("거절", primary = false, enabled = !submitting, onClick = onReject, modifier = Modifier.weight(1f))
                CardButton("수락", primary = true, enabled = !submitting, onClick = onAccept, modifier = Modifier.weight(1f))
            }
            ProposalCardActions.WITHDRAW ->
                CardButton("제안 철회", primary = false, enabled = !submitting, onClick = onWithdraw, modifier = Modifier.fillMaxWidth())
            ProposalCardActions.NONE -> Unit
        }
        }
    }
}

// "1/2명 수락 · 오후 6:50까지 응답"
@Composable
private fun ProgressLine(progressText: String, deadlineText: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(progressText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColor.AccentStrong)
        Text(" · $deadlineText", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
    }
}

@Composable
private fun CardButton(text: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        modifier
            .height(46.dp)
            .clip(shape)
            .background(if (primary) AppColor.Accent else AppColor.Surface)
            .then(if (!primary) Modifier.border(1.dp, AppColor.Border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (enabled) AppColor.TextPrimary else AppColor.TextDisabled)
    }
}

// ---------- 정보 카드 아래 진입점 ----------

// "🕒 시작 시각을 바꿔야 하나요?   변경 제안 >" — 제안할 수 있을 때만 정보 카드 바로 아래에 둔다.
@Composable
fun ProposeTimeRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppColor.SurfaceMuted)
            .clickable(onClickLabel = "시간 변경 제안", onClick = onClick)
            .padding(horizontal = AppSpace.xxl, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClockIcon(AppColor.TextSecondary, 16.dp)
        Spacer(Modifier.width(AppSpace.md))
        Text("시작 시각을 바꿔야 하나요?", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary, modifier = Modifier.weight(1f))
        Text("변경 제안", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColor.AccentStrong)
        com.example.myapplication.ui.explore.filter.ChevronRightIcon(AppColor.AccentStrong, size = 16.dp)
    }
}

// ---------- 시각 선택 시트 ----------

@Composable
fun TimeProposalBottomSheet(
    meeting: Meeting,
    nowMillis: Long,
    submitting: Boolean,
    onSubmit: (startAtMillis: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf<Long?>(null) }
    CreateSheetFrame(onDismiss = onDismiss) {
        TimeProposalSheetBody(
            meeting = meeting,
            nowMillis = nowMillis,
            selected = selected,
            submitting = submitting,
            onSelect = { selected = it },
            onSubmit = { selected?.let(onSubmit) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimeProposalSheetBody(
    meeting: Meeting,
    nowMillis: Long,
    selected: Long?,
    submitting: Boolean,
    onSelect: (Long) -> Unit,
    onSubmit: () -> Unit,
) {
    val options = timeProposalOptions(meeting, nowMillis)
    // 시간이 흘러 고른 시각이 꺼졌으면 고르지 않은 것으로 본다.
    val chosen = options.find { it.startAtMillis == selected && it.enabled }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.x4)) {
        SheetTitle("시간 변경 제안", "모두 수락하면 바뀌어요")

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.md)).background(AppColor.SurfaceMuted).padding(AppSpace.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("기존 시작", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary, modifier = Modifier.weight(1f))
            Text(formatDetailDateTime(meeting.startAtMillis, 0), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }

        listOf("앞당기기" to options.filter { it.shiftMinutes < 0 }, "미루기" to options.filter { it.shiftMinutes > 0 }).forEach { (label, group) ->
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                SheetSectionLabel(label)
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpace.md),
                    maxItemsInEachRow = 3,
                ) {
                    group.forEach { option ->
                        TimeOptionChip(
                            option = option,
                            baseMillis = meeting.startAtMillis,
                            selected = option == chosen,
                            onClick = { onSelect(option.startAtMillis) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (options.any { !it.enabled }) {
            Text(timeProposalLeadNote(meeting), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
        }

        // 고른 시각의 결과 안내
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.md)).background(AppColor.SurfaceMuted).padding(AppSpace.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xs),
        ) {
            val members = meeting.confirmedCount
            Bullet("확정된 ${members}명이 모두 수락해야 바뀌고, 한 명이라도 거절하면 무산돼요")
            if (chosen != null) {
                val expires = timeProposalExpiresAtMillis(meeting, chosen.startAtMillis)
                Bullet("${formatClock(expires, sameDayAs = nowMillis)}까지 모두 수락하지 않으면 무산돼요")
            }
            Bullet("진행 시간은 그대로라 끝나는 시각도 함께 바뀌어요")
        }

        SubmitButton(
            text = if (submitting) "제안하는 중…" else chosen?.let { "${formatClock(it.startAtMillis, sameDayAs = meeting.startAtMillis)}로 제안하기" } ?: "시각을 골라 주세요",
            enabled = chosen != null && !submitting,
            onClick = onSubmit,
        )
    }
}

@Composable
private fun TimeOptionChip(option: TimeOption, baseMillis: Long, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppRadius.md)
    val bg = when {
        selected -> AppColor.AccentSoft
        !option.enabled -> AppColor.ChipNeutralBg
        else -> AppColor.Surface
    }
    val border = when {
        selected -> AppColor.Accent
        !option.enabled -> Color.Transparent
        else -> AppColor.Border
    }
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .clickable(enabled = option.enabled, onClick = onClick)
            .padding(vertical = AppSpace.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpace.xxs),
    ) {
        Text(
            formatClock(option.startAtMillis, sameDayAs = baseMillis),
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = if (option.enabled) AppColor.TextPrimary else AppColor.TextDisabled,
        )
        Text(
            formatShift(option.shiftMinutes), fontSize = 11.sp, fontWeight = FontWeight.Medium,
            color = when {
                selected -> AppColor.AccentStrong
                option.enabled -> AppColor.TextSecondary
                else -> AppColor.TextDisabled
            },
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•", fontSize = 13.sp, lineHeight = 19.sp, color = AppColor.TextSecondary, modifier = Modifier.width(12.dp))
        Text(text, fontSize = 13.sp, lineHeight = 19.sp, color = AppColor.TextSecondary)
    }
}

@Composable
private fun SubmitButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(if (enabled) AppColor.Accent else AppColor.DisabledBg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (enabled) AppColor.OnAccent else AppColor.TextDisabled)
    }
}

// ---------- 거절·철회 확인창 ----------

enum class ProposalConfirm { REJECT, WITHDRAW }

@Composable
fun TimeProposalConfirmDialog(kind: ProposalConfirm, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (kind) {
        ProposalConfirm.REJECT -> ConfirmDialog(
            title = "제안을 거절할까요?",
            message = "한 명이라도 거절하면 제안은 바로 무산되고, 시작 시각은 그대로예요.",
            confirmText = "거절",
            dismissText = "취소",
            destructive = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        ProposalConfirm.WITHDRAW -> ConfirmDialog(
            title = "제안을 철회할까요?",
            message = "이미 수락한 메이트에게도 철회 알림이 가요.",
            confirmText = "철회",
            dismissText = "취소",
            destructive = true,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

// ---------- 아이콘 ----------

@Composable
private fun ClockIcon(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val sw = 1.8.dp.toPx()
        drawCircle(color, radius = w / 2 - sw / 2, style = Stroke(sw))
        drawLine(color, Offset(w / 2, w / 2), Offset(w / 2, w * .26f), sw, StrokeCap.Round)
        drawLine(color, Offset(w / 2, w / 2), Offset(w * .68f, w * .6f), sw, StrokeCap.Round)
    }
}

@Composable
private fun ArrowIcon(color: Color) {
    Canvas(Modifier.size(16.dp)) {
        val w = size.width
        val sw = 1.8.dp.toPx()
        drawLine(color, Offset(w * .1f, w / 2), Offset(w * .9f, w / 2), sw, StrokeCap.Round)
        drawLine(color, Offset(w * .6f, w * .2f), Offset(w * .9f, w / 2), sw, StrokeCap.Round)
        drawLine(color, Offset(w * .6f, w * .8f), Offset(w * .9f, w / 2), sw, StrokeCap.Round)
    }
}

@Composable
private fun CheckMark(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val sw = 1.8.dp.toPx()
        drawLine(color, Offset(w * .15f, w * .55f), Offset(w * .4f, w * .8f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * .4f, w * .8f), Offset(w * .88f, w * .25f), sw, StrokeCap.Round)
    }
}

// ---------- 미리보기 ----------
// 기기에서 보려면: adb shell am start -n com.example.myapplication/androidx.compose.ui.tooling.PreviewActivity \
//   --es composable com.example.myapplication.ui.meeting.detail.TimeProposalCardKt.<미리보기 함수 이름>

private const val PREVIEW_MIN = 60_000L
private val previewStart = java.util.Calendar.getInstance().apply {
    add(java.util.Calendar.DAY_OF_YEAR, 1); set(java.util.Calendar.HOUR_OF_DAY, 19); set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
}.timeInMillis

private val previewProposal = TimeProposal(
    id = "7", status = TimeProposalStatus.PENDING, proposerId = "u2", proposerNickname = "서연",
    fromStartAtMillis = previewStart, toStartAtMillis = previewStart + 30 * PREVIEW_MIN, expiresAtMillis = previewStart - 10 * PREVIEW_MIN,
    votes = listOf(
        TimeProposalVote("u3", "지훈", isHost = true, decision = VoteDecision.ACCEPTED),
        TimeProposalVote("u2", "서연", isHost = false, decision = VoteDecision.ACCEPTED),
        TimeProposalVote("u1", "민준", isHost = false, decision = VoteDecision.PENDING),
    ),
)

private val previewMeeting = Meeting(
    id = "m3", title = "하체 데이", hostId = "u3", hostNickname = "지훈", startAtMillis = previewStart, durationMinutes = 90,
    capacity = 3, confirmedIds = listOf("u3", "u2", "u1"), confirmedCount = 3, status = MeetingStatus.CLOSED,
)

@Composable
private fun PreviewBox(content: @Composable () -> Unit) {
    MyApplicationTheme(dynamicColor = false) {
        Box(Modifier.background(AppColor.Surface).padding(AppSpace.x4)) { content() }
    }
}

@Preview(name = "제안 카드 - 내가 응답할 차례", showBackground = true, widthDp = 390)
@Composable
private fun PreviewCardRespond() = PreviewBox {
    TimeProposalCard(timeProposalCardOf(previewProposal, "u1", previewStart - 3 * 60 * PREVIEW_MIN), false, {}, {}, {})
}

@Preview(name = "제안 카드 - 접힘", showBackground = true, widthDp = 390)
@Composable
private fun PreviewCardCollapsed() = PreviewBox {
    TimeProposalCard(timeProposalCardOf(previewProposal, "u1", previewStart - 3 * 60 * PREVIEW_MIN), false, {}, {}, {}, expanded = false)
}

@Preview(name = "제안 카드 - 내가 제안함", showBackground = true, widthDp = 390)
@Composable
private fun PreviewCardProposer() = PreviewBox {
    TimeProposalCard(timeProposalCardOf(previewProposal, "u2", previewStart - 3 * 60 * PREVIEW_MIN), false, {}, {}, {})
}

@Preview(name = "제안 카드 - 수락하고 기다림", showBackground = true, widthDp = 390)
@Composable
private fun PreviewCardWaiting() = PreviewBox {
    TimeProposalCard(timeProposalCardOf(previewProposal, "u3", previewStart - 3 * 60 * PREVIEW_MIN), false, {}, {}, {})
}

@Preview(name = "제안 카드 - 응답 기한 지남", showBackground = true, widthDp = 390)
@Composable
private fun PreviewCardExpired() = PreviewBox {
    TimeProposalCard(timeProposalCardOf(previewProposal, "u1", previewStart - 5 * PREVIEW_MIN), false, {}, {}, {})
}

@Preview(name = "시트 - 마감 모임, 1시간 전", showBackground = true, widthDp = 390)
@Composable
private fun PreviewSheetClosed() = PreviewBox {
    TimeProposalSheetBody(previewMeeting, previewStart - 55 * PREVIEW_MIN, selected = previewStart + 30 * PREVIEW_MIN, submitting = false, onSelect = {}, onSubmit = {})
}

@Preview(name = "시트 - 여유 있음, 선택 전", showBackground = true, widthDp = 390)
@Composable
private fun PreviewSheetFree() = PreviewBox {
    TimeProposalSheetBody(previewMeeting, previewStart - 6 * 60 * PREVIEW_MIN, selected = null, submitting = false, onSelect = {}, onSubmit = {})
}

@Preview(name = "확인창 - 거절", showBackground = true, widthDp = 390, heightDp = 400)
@Composable
private fun PreviewRejectDialog() = MyApplicationTheme(dynamicColor = false) {
    TimeProposalConfirmDialog(ProposalConfirm.REJECT, {}, {})
}
