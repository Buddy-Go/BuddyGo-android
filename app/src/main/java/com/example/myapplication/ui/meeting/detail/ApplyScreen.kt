package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.common.ConfirmDialog
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.common.InlineErrorBanner
import com.example.myapplication.ui.common.SkeletonDetail
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.filter.ChevronRightIcon
import com.example.myapplication.ui.record.InfoIcon
import com.example.myapplication.ui.explore.filter.CloseIcon
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.SuitFontFamily

// 6a 신청 작성(기준 일치), 6b 신청 작성(기준 미설정). 호스트에게 가는 정보를 보여 주고 한마디를 받아 신청한다.
// 기준이 맞지 않아도 신청은 막지 않는다. (service-rules 5장)
@Composable
fun ApplyScreen(
    meetingId: String,
    onClose: () -> Unit,
    onApplied: () -> Unit,
    onEditRecord: (Sport) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ApplyViewModel = viewModel(
        key = "apply-$meetingId",
        factory = viewModelFactory { initializer { ApplyViewModel(meetingId) } },
    ),
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // 신청이 끝나면 화면을 닫고 모임 상세로 돌아간다.
    LaunchedEffect(state.done) { if (state.done) onApplied() }

    Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding().imePadding()) {
        TopBar(onClose)
        when (val loaded = state.meeting) {
            UiState.Loading -> SkeletonDetail()
            is UiState.Error -> FullScreenError(loaded.type, onRetry = viewModel::load)
            is UiState.Success -> ApplyForm(state, loaded.data, viewModel, onEditRecord)
        }
    }

    // 한마디를 비우고 신청하면 한 번 확인한다. (5장)
    if (state.confirmEmpty) {
        ConfirmDialog(
            title = "한마디 없이 신청할까요?",
            message = "한마디를 남기면 수락받을 확률이 높아요",
            confirmText = "신청하기",
            dismissText = "한마디 쓰기",
            onConfirm = viewModel::onConfirmEmpty,
            onDismiss = viewModel::onDismissConfirmEmpty,
        )
    }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = AppSpace.x4), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).iconButton("신청 닫기", onClose), contentAlignment = Alignment.CenterStart) {
            CloseIcon(AppColor.TextPrimary, size = 24.dp)
        }
        Spacer(Modifier.width(AppSpace.md))
        Text("신청하기", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
    }
}

@Composable
private fun ColumnScope.ApplyForm(
    state: ApplyState,
    meeting: Meeting,
    viewModel: ApplyViewModel,
    onEditRecord: (Sport) -> Unit,
) {
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = AppSpace.x4),
        verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
    ) {
        if (state.failed) InlineErrorBanner("신청을 보내지 못했어요. 다시 시도해주세요.")
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            Text(meeting.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, lineHeight = 34.sp)
            Text(applySubtitle(meeting), fontSize = 15.sp, color = AppColor.TextSecondary)
        }
        Divider()
        Text("호스트에게 전달되는 정보", fontSize = 15.sp, color = AppColor.TextSecondary)
        state.me?.let { me ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(AppColor.ChipNeutralBg), contentAlignment = Alignment.Center) {
                    Text(me.nickname.take(1), fontSize = 20.sp, color = AppColor.TextSecondary, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(AppSpace.xl))
                Column {
                    Text(me.nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text("매너온도 ${me.mannerTemperature}°C", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColor.AccentStrong)
                }
            }
        }
        Divider()
        RecordRow(state.recordSummary, onClick = { onEditRecord(meeting.sport) })
        MessageField(state.message, state.messageLengthText, viewModel::onMessageChange)
        Text(
            "신청하면 호스트가 수락할 때까지 대기 상태예요.\n수락되면 알림을 보내드릴게요.",
            fontSize = 14.sp, color = AppColor.TextSecondary, lineHeight = 21.sp,
        )
    }
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
        Column(Modifier.padding(horizontal = AppSpace.x4, vertical = AppSpace.xl), verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            // 16i 제출 실패: 버튼을 '다시 시도'로 바꾸고 띠는 화면 위에 보인다. 16c 제출 중에는 버튼이 '신청하는 중'이다.
            ActionButton(
                text = if (state.failed) "다시 시도" else "신청하기",
                loadingText = "신청하는 중",
                loading = state.submitting,
                onClick = viewModel::onSubmit,
            )
        }
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))

// 이 종목의 내 기록. 있으면 요약을, 없으면 '미설정'과 안내를 보여 준다. 누르면 기록을 고치러 간다. (6a, 6b)
@Composable
private fun RecordRow(summary: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, AppColor.Border, shape)
            .clickable(onClickLabel = "내 기록 고치기", role = Role.Button, onClick = onClick).padding(horizontal = 15.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(AppSpace.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("기준", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
            Text(
                summary ?: "미설정", fontSize = 16.sp, textAlign = TextAlign.End,
                color = if (summary == null) AppColor.TextSecondary else AppColor.TextPrimary,
                modifier = Modifier.weight(1f).padding(horizontal = AppSpace.xl),
            )
            ChevronRightIcon(AppColor.TextSecondary)
        }
        if (summary == null) Row(verticalAlignment = Alignment.CenterVertically) {
            InfoIcon(AppColor.IconMuted, size = 16.dp)
            Spacer(Modifier.width(AppSpace.md))
            Text("기록을 설정하면 호스트가 판단하기 쉬워요", fontSize = 14.sp, color = AppColor.TextSecondary)
        }
    }
}

@Composable
private fun MessageField(message: String, counter: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("한마디 남기기", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                "선택", fontSize = 13.sp, color = AppColor.TextSecondary,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AppColor.ChipNeutralBg).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Box(Modifier.fillMaxWidth().height(108.dp)) {
            if (message.isEmpty()) {
                Text(
                    "왜 참여하고 싶은지, 어떤 페이스로 하고 싶은지 적으면 수락받을 확률이 높아요",
                    fontSize = 16.sp, color = AppColor.TextDisabled, lineHeight = 24.sp,
                )
            }
            BasicTextField(
                value = message, onValueChange = onChange,
                textStyle = TextStyle(fontFamily = SuitFontFamily, fontSize = 16.sp, color = AppColor.TextPrimary, lineHeight = 24.sp),
                cursorBrush = SolidColor(AppColor.AccentStrong),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Divider()
        Text(counter, fontSize = 15.sp, color = AppColor.TextSecondary, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
    }
}
