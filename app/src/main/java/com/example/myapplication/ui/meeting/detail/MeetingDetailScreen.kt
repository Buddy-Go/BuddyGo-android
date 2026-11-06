package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.Sport
import kotlinx.coroutines.launch
import com.example.myapplication.ui.common.CriteriaSummaryRow
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.common.SkeletonDetail
import com.example.myapplication.ui.common.ToastHost
import com.example.myapplication.ui.common.ToastState
import com.example.myapplication.ui.common.ToastType
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.HeartIcon
import com.example.myapplication.ui.explore.filter.ChevronLeftIcon
import com.example.myapplication.ui.explore.filter.ChevronRightIcon
import com.example.myapplication.ui.explore.filter.genderLabel
import com.example.myapplication.ui.explore.favoriteLabel
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.explore.sportLabel
import com.example.myapplication.ui.record.SportIcon
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 5a~5d, 5g, 5l 모임 상세 (참가자 시점). 상태와 내 역할에 따라 칩·참가자 영역·하단 버튼이 달라진다.
// 호스트 영역(5a-H 계열)은 문진곤이 `HostSection`으로 만들어 끼운다.
@Composable
fun MeetingDetailScreen(
    meetingId: String,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onCheckIn: () -> Unit,
    onReview: () -> Unit,
    onAddRecord: (Sport) -> Unit,
    justApplied: Boolean = false,       // 신청 화면에서 방금 신청을 마치고 돌아왔는지
    onAppliedHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MeetingDetailViewModel = viewModel(
        key = meetingId,
        factory = viewModelFactory { initializer { MeetingDetailViewModel(meetingId) } },
    ),
) {
    val state by viewModel.state.collectAsState()
    val toast = remember { ToastState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 저장 실패 같은 안내는 토스트로 한 번 보여 주고 지운다.
    LaunchedEffect(state.message) {
        state.message?.let {
            toast.show(it, ToastType.Error)
            viewModel.onMessageShown()
        }
    }

    // 신청을 마치고 돌아오면 모임 정보를 새로 받아 '신청 검토중'으로 바꾸고 알려 준다.
    LaunchedEffect(justApplied) {
        if (justApplied) {
            viewModel.onApplied()
            toast.show("신청했어요. 호스트가 수락하면 알려드릴게요", ToastType.Success)
            onAppliedHandled()
        }
    }

    Box(modifier.fillMaxSize().background(AppColor.Surface)) {
        when (val loaded = state.meeting) {
            UiState.Loading -> Column(Modifier.statusBarsPadding()) {
                BackBar(onBack)
                SkeletonDetail()
            }
            is UiState.Error -> FullScreenError(loaded.type, onRetry = viewModel::load)
            is UiState.Success -> DetailContent(
                state = state,
                meeting = loaded.data,
                onBack = onBack,
                onToggleFavorite = viewModel::onToggleFavorite,
                onPlaceClick = { viewModel.onOpenSheet(DetailSheet.PLACE) },
                onCriteriaClick = { viewModel.onOpenSheet(DetailSheet.CRITERIA) },
                onAction = { action ->
                    when (action) {
                        DetailAction.APPLY -> onApply()
                        DetailAction.CANCEL_APPLICATION -> viewModel.onCancelApplication()
                        DetailAction.CHECK_IN -> onCheckIn()
                        DetailAction.REVIEW -> onReview()
                        DetailAction.NONE -> Unit
                    }
                },
            )
        }
        // 5e 장소 시트, 5f·5h 기준 시트
        val sheetMeeting = state.loaded
        if (sheetMeeting != null) when (state.openSheet) {
            DetailSheet.PLACE -> PlaceBottomSheet(
                meeting = sheetMeeting,
                distanceMeters = state.distanceMeters,
                onCopyAddress = {
                    copyToClipboard(context, "주소", fullAddress(sheetMeeting))
                    scope.launch { toast.show("주소를 복사했어요", ToastType.Success) }
                },
                onFindRoute = { openRoute(context, sheetMeeting) { scope.launch { toast.show("길찾기를 열 수 없어요", ToastType.Error) } } },
                onDismiss = viewModel::onCloseSheet,
            )
            DetailSheet.CRITERIA -> CriteriaBottomSheet(
                meeting = sheetMeeting,
                myRecord = state.myRecord,
                onAddRecord = {
                    viewModel.onCloseSheet()
                    onAddRecord(it)
                },
                onDismiss = viewModel::onCloseSheet,
            )
            null -> Unit
        }
        ToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
}

@Composable
private fun DetailContent(
    state: MeetingDetailState,
    meeting: Meeting,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlaceClick: () -> Unit,
    onCriteriaClick: () -> Unit,
    onAction: (DetailAction) -> Unit,
) {
    val role = state.role
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackBar(onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = AppSpace.x4),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
        ) {
            ChipsRow(meeting, role)
            Text(meeting.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, lineHeight = 34.sp)
            ParticipantsSection(meeting, state.nowMillis)
            InfoCard(meeting, onPlaceClick, onCriteriaClick)
            HostCard(meeting)
            if (meeting.description.isNotBlank()) IntroCard(meeting.description)
            if (role == MeetingRole.HOST) HostAreaPlaceholder()
            Spacer(Modifier.height(AppSpace.md))
        }
        BottomBar(meeting.isFavorite, state.bottomAction, state.submitting, onToggleFavorite, onAction)
    }
}

@Composable
private fun BackBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = AppSpace.x4), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).iconButton("뒤로", onBack), contentAlignment = Alignment.CenterStart) {
            ChevronLeftIcon(AppColor.TextPrimary, size = 28.dp)
        }
    }
}

@Composable
private fun ChipsRow(meeting: Meeting, role: MeetingRole) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.clip(RoundedCornerShape(AppRadius.pill)).border1(AppColor.Border).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SportIcon(meeting.sport, AppColor.TextPrimary, size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Text(sportLabel(meeting.sport), fontSize = 15.sp, color = AppColor.TextPrimary)
        }
        statusChipsOf(meeting, role).forEach { chip ->
            val (bg, fg, border) = when (chip.tone) {
                ChipTone.Lime -> Triple(AppColor.Accent, AppColor.TextPrimary, false)
                ChipTone.Dark -> Triple(AppColor.TextPrimary, Color.White, false)
                ChipTone.Gray -> Triple(AppColor.ChipNeutralBg, AppColor.TextSecondary, false)
                ChipTone.Outlined -> Triple(AppColor.Surface, AppColor.TextPrimary, true)
            }
            Text(
                chip.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = fg,
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(bg)
                    .then(if (border) Modifier.border1(AppColor.Border) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ParticipantsSection(meeting: Meeting, nowMillis: Long) {
    val active = meeting.status == MeetingStatus.RECRUITING || meeting.status == MeetingStatus.CLOSED
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
        Text(participantsTitle(meeting), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        if (active) {
            ProgressBar(meeting.confirmedCount, meeting.capacity, lime = true)
            // 명단을 볼 수 없는 사람에게는 이유를 알려 준다. (service-rules 5장)
            if (meeting.members.isEmpty()) Text(MEMBERS_HIDDEN_NOTE, fontSize = 14.sp, color = AppColor.TextSecondary)
            if (meeting.status == MeetingStatus.CLOSED) Text(NOTICE_NOTE, fontSize = 14.sp, color = AppColor.TextSecondary)
        } else if (meeting.status == MeetingStatus.IN_PROGRESS) {
            ProgressBar(meeting.confirmedCount, meeting.capacity, lime = false)
        }
        avatarRowOf(meeting)?.let { AvatarRow(it) }
        statusNoteOf(meeting, nowMillis)?.let { (title, sub) ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.md)).background(AppColor.SurfaceMuted).padding(AppSpace.xxl),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text(sub, fontSize = 14.sp, color = AppColor.TextSecondary)
            }
        }
    }
}

@Composable
private fun ProgressBar(confirmed: Int, capacity: Int, lime: Boolean) {
    val ratio = if (capacity > 0) (confirmed.toFloat() / capacity).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(AppColor.ChipNeutralBg)) {
        Box(Modifier.fillMaxWidth(ratio).height(8.dp).clip(CircleShape).background(if (lime) AppColor.Accent else AppColor.TextDisabled))
    }
}

@Composable
private fun AvatarRow(row: AvatarRow) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        row.initials.forEach { Avatar(it, 52.dp) }
        if (row.extra > 0) Avatar("+${row.extra}", 52.dp)
    }
}

@Composable
private fun Avatar(text: String, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(AppColor.ChipNeutralBg), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 18.sp, color = AppColor.TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoCard(meeting: Meeting, onPlaceClick: () -> Unit, onCriteriaClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.lg)
    val judgement = judgementViewOf(meeting)
    Column(Modifier.fillMaxWidth().clip(shape).border1(AppColor.Border, shape)) {
        InfoRow("일시", formatDetailDateTime(meeting.startAtMillis, meeting.durationMinutes))
        Divider()
        InfoRow(
            "장소", placeLine(meeting), onClick = onPlaceClick,
            // 진행중에는 체크인 범위를 알려 준다. (정의서 7장: 모임 장소 50m 안)
            sub = if (meeting.status == MeetingStatus.IN_PROGRESS) "반경 50m 안에서 체크인할 수 있어요" else null,
        )
        Divider()
        // 5i 기준 요약 행: 판정은 서버가 계산한 값을 그대로 보여 준다.
        CriteriaSummaryRow(
            title = sportCriteriaTitle(meeting.sport),
            summary = criteriaSummaryOf(meeting),
            onClick = onCriteriaClick,
            match = judgement?.match,
            statusText = judgement?.statusText,
            bordered = false,
        )
        Divider()
        InfoRow("정원 · 성별", "${meeting.capacity}명 · ${genderLabel(meeting.genderLimit)}")
    }
}

@Composable
private fun Divider() = Box(Modifier.padding(horizontal = AppSpace.xxl).fillMaxWidth().height(1.dp).background(AppColor.Border))

@Composable
private fun InfoRow(title: String, value: String, onClick: (() -> Unit)? = null, sub: String? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = AppSpace.xxl, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(value, fontSize = 16.sp, color = AppColor.TextPrimary, textAlign = TextAlign.End)
            if (sub != null) Text(sub, fontSize = 13.sp, color = AppColor.TextSecondary, textAlign = TextAlign.End)
        }
        if (onClick != null) {
            Spacer(Modifier.width(AppSpace.xl))
            ChevronRightIcon(AppColor.TextSecondary)
        }
    }
}

@Composable
private fun HostCard(meeting: Meeting) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.lg)).background(AppColor.SurfaceMuted).padding(AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(meeting.hostNickname.take(1), 52.dp)
        Spacer(Modifier.width(AppSpace.xl))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meeting.hostNickname, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Spacer(Modifier.width(AppSpace.md))
                Text(
                    "호스트", fontSize = 12.sp, color = AppColor.TextSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AppColor.ChipNeutralBg).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Text(
                "매너온도 ${meeting.hostMannerTemperature}°C · 후기 ${meeting.hostReviewCount}",
                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColor.AccentStrong,
            )
        }
        ChevronRightIcon(AppColor.TextSecondary)
    }
}

@Composable
private fun IntroCard(description: String) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(Modifier.fillMaxWidth().clip(shape).border1(AppColor.Border, shape).padding(AppSpace.xxl), verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        Text("모임 소개", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Text(description, fontSize = 16.sp, color = AppColor.TextPrimary, lineHeight = 24.sp)
    }
}

// TODO(임시) 호스트 영역(5a-H 계열)은 문진곤이 `HostSection.kt`로 만들어 이 자리에서 부른다.
@Composable
private fun HostAreaPlaceholder() {
    Text("호스트 영역은 준비 중이에요", fontSize = 14.sp, color = AppColor.TextSecondary)
}

@Composable
private fun BottomBar(
    isFavorite: Boolean,
    action: BottomAction?,
    submitting: Boolean,
    onToggleFavorite: () -> Unit,
    onAction: (DetailAction) -> Unit,
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
        Row(Modifier.padding(horizontal = AppSpace.x4, vertical = AppSpace.xl), horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(AppRadius.md)).border1(AppColor.Border, RoundedCornerShape(AppRadius.md))
                    .iconButton(favoriteLabel(isFavorite), onToggleFavorite),
                contentAlignment = Alignment.Center,
            ) {
                HeartIcon(isFavorite, if (isFavorite) Color(0xFFE5383B) else AppColor.TextPrimary, size = 24.dp)
            }
            if (action != null) ActionBox(action, submitting, onAction, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionBox(action: BottomAction, submitting: Boolean, onAction: (DetailAction) -> Unit, modifier: Modifier) {
    val active = action.enabled && !submitting
    val shape = RoundedCornerShape(AppRadius.md)
    val container = when {
        !action.enabled -> AppColor.ChipNeutralBg
        action.outlined -> AppColor.Surface
        else -> AppColor.Accent
    }
    Surface(
        modifier = modifier.height(56.dp).clip(shape).clickable(enabled = active, onClickLabel = action.title) { onAction(action.action) },
        shape = shape, color = container,
        border = if (action.outlined && action.enabled) BorderStroke(1.dp, AppColor.Border) else null,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                action.title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = if (action.enabled) AppColor.TextPrimary else AppColor.TextDisabled,
            )
            if (action.subtitle != null) Text(action.subtitle, fontSize = 12.sp, color = AppColor.TextSecondary)
        }
    }
}

// 얇은 테두리를 두르는 짧은 표기
private fun Modifier.border1(color: Color, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(AppRadius.pill)) =
    this.then(Modifier.border(1.dp, color, shape))

// 주소를 클립보드에 복사한다.
private fun copyToClipboard(context: android.content.Context, label: String, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

// 카카오맵 길찾기 링크를 연다. 열 수 있는 앱이 없으면 onFail을 부른다.
private fun openRoute(context: android.content.Context, meeting: Meeting, onFail: () -> Unit) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(routeUrl(meeting)))
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure { onFail() }
}
