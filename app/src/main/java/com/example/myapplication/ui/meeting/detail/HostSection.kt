package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 모임 상세의 호스트 영역 ← 5a-H-After 모집중, 5a-H2-After 신청자 없음, 5a-H4-After 공지 2건, 5a-H5 마감됨 (문진곤)
// 화면 본체 MeetingDetailScreen(문찬주)이 isHost일 때 아래 부품을 제자리에 끼운다.
//   상단 바 오른쪽        → HostMoreMenu
//   제목 아래             → HostRosterSection (참가자 화면의 '참여자 n / m' 자리)
//   정보 카드 아래        → HostSelfRow (참가자 화면의 호스트 카드 자리)
//   그 아래              → HostNoticeSection
//   맨 아래 고정 버튼      → HostBottomBar (참가자 화면의 '참여 신청하기' 자리)
// 상태 칩·정보 카드·모임 소개는 참가자 화면과 같아서 본체가 그린다.

// ---------- 상단 바 ⋯ 메뉴 ----------

@Composable
fun HostMoreMenu(
    actions: List<HostAction>,
    onAction: (HostAction) -> Unit,
) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        Icon(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = "모임 더보기",
            tint = AppColor.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(AppRadius.pill))
                .clickable { expanded = true },
        )
        // 메뉴 모양 디자인이 없어서 Material 기본 메뉴를 쓴다 (팀에 질문 대상)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppColor.Surface,
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = action.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (action == HostAction.EDIT) AppColor.TextPrimary else AppColor.DestructiveText,
                        )
                    },
                    onClick = {
                        expanded = false
                        onAction(action)
                    },
                )
            }
        }
    }
}

// ---------- 확정 명단 ----------

@Composable
fun HostRosterSection(state: HostSectionState, onRosterClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
        Text(text = state.confirmedLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        // 진행 막대
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(AppRadius.pill))
                .background(AppColor.DisabledBg),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(AppColor.Accent),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRosterClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
        ) {
            AvatarStack(state.confirmedNicknames)
            Text(
                text = state.rosterSummary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextSecondary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = "확정 명단 보기",
                tint = AppColor.IconMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = state.rosterHint,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = AppColor.TextSecondary,
        )
    }
}

// 겹친 이니셜 원 (28dp, 8dp씩 겹침)
@Composable
private fun AvatarStack(nicknames: List<String>) {
    val backgrounds = listOf(AppColor.AvatarBg, AppColor.Grabber, AppColor.ChipNeutralBg)
    Row {
        nicknames.forEachIndexed { index, name ->
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp * index)
                    .size(32.dp)
                    .background(AppColor.Surface, RoundedCornerShape(AppRadius.pill)) // 2dp 흰 테두리
                    .padding(2.dp)
                    .background(backgrounds[index % backgrounds.size], RoundedCornerShape(AppRadius.pill)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = name.take(1), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColor.TextBody)
            }
        }
    }
}

// ---------- '내가 호스트' 행 ----------

@Composable
fun HostSelfRow(nickname: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(AppColor.AvatarBg, RoundedCornerShape(AppRadius.pill)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = nickname.take(1), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColor.TextBody)
        }
        Text(text = nickname, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Box(
            modifier = Modifier
                .height(20.dp)
                .background(AppColor.ChipNeutralBg, RoundedCornerShape(AppRadius.xs))
                .padding(horizontal = AppSpace.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "내가 호스트", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColor.TextSecondary)
        }
    }
}

// ---------- 공지 ----------

@Composable
fun HostNoticeSection(
    state: HostSectionState,
    onWriteClick: () -> Unit, // 공지 없을 때 '작성 >' → 8a
    onAllClick: () -> Unit, // 공지 있을 때 '전체 >' → 8d
    onNoticeClick: (noticeId: String) -> Unit, // 미리보기 카드 → 8f
) {
    val notice = state.latestNotice
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "공지",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.noticeActionLabel} >",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.TextPrimary,
                modifier = Modifier.clickable(onClick = if (notice == null) onWriteClick else onAllClick),
            )
        }
        if (notice == null) {
            Text(text = "아직 작성한 공지가 없어요", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(AppColor.AccentSoft)
                    .clickable { onNoticeClick(notice.noticeId) }
                    .padding(AppSpace.xxl),
                verticalArrangement = Arrangement.spacedBy(AppSpace.sm),
            ) {
                Text(text = notice.timeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextSecondary)
                Text(text = notice.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text(
                    text = notice.body,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColor.TextPrimary,
                )
            }
        }
    }
}

// ---------- 하단 '참가자 관리' ----------

@Composable
fun HostBottomBar(state: HostSectionState, onManageClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.md)
    val highlight = state.highlightManageButton
    Column(modifier = Modifier.background(AppColor.Surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xl, bottom = AppSpace.xxl)
                .height(54.dp)
                .clip(shape)
                .background(if (highlight) AppColor.Accent else AppColor.Surface)
                .then(if (highlight) Modifier else Modifier.border(1.dp, AppColor.Border, shape))
                .clickable(onClick = onManageClick),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.md, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "참가자 관리",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) AppColor.OnAccent else AppColor.TextPrimary,
            )
            if (highlight) {
                // 대기 중인 신청 수
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(AppColor.Surface, RoundedCornerShape(AppRadius.pill)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.pendingApplicantCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColor.TextPrimary,
                    )
                }
            }
        }
    }
}

// ---------- 미리보기: 호스트 부품만 쌓아서 본다 (본체 영역은 회색 자리 표시) ----------

internal val previewHostState = HostSectionState(
    hostNickname = "민준",
    confirmedNicknames = listOf("민준", "수아"),
    capacity = 3,
    isClosed = false,
    pendingApplicantCount = 2,
)

internal val previewNotice = HostNoticePreview(
    noticeId = "n1",
    timeText = "9/7 21:30",
    title = "집합 장소 변경",
    body = "비 예보가 있어서 정문 대신 후문 3층 프리웨이트 존에서 만나요.",
)

@Composable
private fun PreviewPage(state: HostSectionState) {
    Column(modifier = Modifier.background(AppColor.Surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
            horizontalArrangement = Arrangement.End,
        ) {
            HostMoreMenu(actions = state.menuActions, onAction = {})
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpace.x4),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
        ) {
            PlaceholderBlock("상태 칩 · 제목 (본체)", 70)
            HostRosterSection(state = state, onRosterClick = {})
            PlaceholderBlock("정보 카드 (본체)", 160)
            HostSelfRow(nickname = state.hostNickname)
            HostNoticeSection(state = state, onWriteClick = {}, onAllClick = {}, onNoticeClick = {})
            PlaceholderBlock("모임 소개 (본체)", 60)
        }
        HostBottomBar(state = state, onManageClick = {})
    }
}

@Composable
private fun PlaceholderBlock(label: String, heightDp: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .background(AppColor.SurfaceMuted, RoundedCornerShape(AppRadius.md)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 12.sp, color = AppColor.TextSecondary)
    }
}

@Preview(name = "5a-H 모집중 · 신청 2", widthDp = 360, heightDp = 800)
@Composable
private fun Preview5aH() = PreviewPage(previewHostState)

@Preview(name = "5a-H2 신청자 없음", widthDp = 360, heightDp = 800)
@Composable
private fun Preview5aH2() = PreviewPage(previewHostState.copy(pendingApplicantCount = 0))

@Preview(name = "5a-H4 공지 있음", widthDp = 360, heightDp = 800)
@Composable
private fun Preview5aH4() = PreviewPage(previewHostState.copy(latestNotice = previewNotice))

@Preview(name = "5a-H5 마감됨", widthDp = 360, heightDp = 800)
@Composable
private fun Preview5aH5() = PreviewPage(
    previewHostState.copy(
        confirmedNicknames = listOf("민준", "수아", "지호"),
        isClosed = true,
        pendingApplicantCount = 0,
        latestNotice = previewNotice,
    ),
)
