package com.example.myapplication.ui.mate

import com.example.myapplication.ui.common.SportChip
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.ConfirmDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.MateActivity
import com.example.myapplication.data.model.MateActivityKind
import com.example.myapplication.data.model.MateProfile
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.data.model.ProfileReview
import com.example.myapplication.data.model.TagCount
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 메이트 프로필 ← 15a 메이트, 15b 메이트 아님, 15c 신청 보냄
@Composable
fun MateProfileScreen(
    viewModel: MateProfileViewModel,
    onBack: () -> Unit,
    onActivityClick: (meetingId: String) -> Unit, // 현재 활동 카드 → 5번 모임 상세
    onAllReviewsClick: () -> Unit, // '전체 >' (후기 전체 화면은 디자인 없음)
) {
    val state = viewModel.state
    LaunchedEffect(state.isRemoved, state.isBlocked) {
        if (state.isRemoved || state.isBlocked) onBack()
    }
    MateProfileContent(
        state = state,
        onBack = onBack,
        onActivityClick = onActivityClick,
        onAllReviewsClick = onAllReviewsClick,
        onSendRequest = viewModel::onSendRequest,
        onToggleNotification = viewModel::onToggleNotification,
        onRemoveMate = viewModel::onRemoveMate,
        onReport = viewModel::onReport,
        onBlock = viewModel::onBlock,
        onRetry = viewModel::reload,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MateProfileContent(
    state: MateProfileState,
    onBack: () -> Unit,
    onActivityClick: (String) -> Unit,
    onAllReviewsClick: () -> Unit,
    onSendRequest: () -> Unit,
    onToggleNotification: () -> Unit,
    onRemoveMate: () -> Unit,
    onReport: (String) -> Unit = {},
    onBlock: () -> Unit = {},
    onRetry: () -> Unit = {},
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    val profile = state.profile
    var confirmBlock by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var reported by remember { mutableStateOf(false) }
    if (confirmBlock) {
        ConfirmDialog(
            title = "${profile?.nickname ?: "이 사람"}님을 차단할까요?",
            message = "차단하면 메이트와 함께 운동한 사람 목록에서 보이지 않아요. 마이페이지의 차단 관리에서 풀 수 있어요.",
            confirmText = "차단",
            dismissText = "취소",
            destructive = true,
            onConfirm = { confirmBlock = false; onBlock() },
            onDismiss = { confirmBlock = false },
        )
    }
    if (showReport) {
        // 신고 사유를 고르면 바로 보낸다. 디자인이 없어 공용 대화상자에 사유 목록을 넣은 임시 화면이다.
        ConfirmDialog(
            title = "신고 사유를 골라 주세요",
            confirmText = "",
            dismissText = "닫기",
            onConfirm = {},
            onDismiss = { showReport = false },
        ) {
            listOf("욕설·비매너", "노쇼", "허위 정보", "기타").forEach { reason ->
                Text(
                    reason,
                    Modifier.fillMaxWidth().clickable { showReport = false; reported = true; onReport(reason) }.padding(vertical = AppSpace.xl),
                    color = AppColor.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                )
            }
        }
    }
    if (reported) {
        ConfirmDialog(
            title = "신고가 접수됐어요",
            message = "확인 후 필요한 조치를 할게요.",
            confirmText = "확인",
            dismissText = "",
            onConfirm = { reported = false },
            onDismiss = { reported = false },
        )
    }
    // 메이트 끊기는 되돌릴 수 없어 먼저 확인한다. 확인 창 디자인이 없어 공용 ConfirmDialog를 쓴다. (문구는 임시)
    var confirmRemove by remember { mutableStateOf(false) }
    if (confirmRemove) {
        ConfirmDialog(
            title = "메이트를 끊을까요?",
            message = "${profile?.nickname ?: "이 사람"}님이 내 메이트 목록에서 사라져요",
            confirmText = "끊기",
            dismissText = "취소",
            destructive = true,
            onConfirm = {
                confirmRemove = false
                onRemoveMate()
            },
            onDismiss = { confirmRemove = false },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // 상단 바: < 와 ⋯(메이트일 때만)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = AppSpace.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "뒤로 가기",
                tint = AppColor.TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBack),
            )
            Box(Modifier.weight(1f))
            if (state.showMenu) {
                ProfileMenu(
                    isMate = state.isMate,
                    notificationLabel = state.notificationMenuLabel,
                    canReport = state.canReport,
                    onToggleNotification = onToggleNotification,
                    onRemoveMate = { confirmRemove = true },
                    onReport = { showReport = true },
                    onBlock = { confirmBlock = true },
                )
            }
        }
        if (state.loadFailed) {
            FullScreenError(type = ErrorType.SERVER, onRetry = onRetry, modifier = Modifier.weight(1f))
            return@Column
        }
        if (profile == null) return@Column

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = AppSpace.x4, end = AppSpace.x4, bottom = AppSpace.x4),
            verticalArrangement = Arrangement.spacedBy(AppSpace.x5),
        ) {
            // 사진·이름·매너온도
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                MateAvatar(userId = profile.userId, nickname = profile.nickname, size = 72.dp, fontSize = 28.sp)
                Text(
                    text = profile.nickname,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColor.TextPrimary,
                    modifier = Modifier.padding(top = AppSpace.xl, bottom = AppSpace.xs),
                )
                MannerLine(text = mannerText(profile.mannerTemperature), fontSize = 13.sp, iconSize = 15.dp)
            }

            // 많이 받은 태그 (첫 번째만 초록)
            if (profile.topTags.isNotEmpty()) {
                Section("많이 받은 태그") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpace.md),
                    ) {
                        profile.topTags.forEachIndexed { index, tag -> TagCountChip(tag, highlight = index == 0) }
                    }
                }
            }

            // 자주 하는 운동 (15): 이 사람이 함께 운동한 종목
            if (profile.frequentSports.isNotEmpty()) {
                Section("자주 하는 운동") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpace.md), verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                        profile.frequentSports.forEach { SportChip(it, fontSize = 14) }
                    }
                }
            }

            // 현재 활동: 메이트만 볼 수 있다 (service-rules 10장)
            if (state.isMate) {
                if (profile.activities.isNotEmpty()) {
                    Section("현재 활동") {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                            profile.activities.forEach { activity ->
                                ActivityCard(activity, state.now, timeZone, onClick = { onActivityClick(activity.meetingId) })
                            }
                        }
                    }
                }
            } else {
                Text(text = "메이트가 되면 모임 활동을 볼 수 있어요", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
            }

            // 후기
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.reviewHeader,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColor.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (profile.reviewCount > 0) {
                        Text(
                            text = "전체 >",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColor.TextPrimary,
                            modifier = Modifier.clickable(onClick = onAllReviewsClick),
                        )
                    }
                }
                profile.recentReviews.forEach { ReviewItem(it, timeZone) }
            }
        }

        when (state.bottomButton) {
            ProfileButton.REQUEST -> BottomButton("메이트 신청", filled = true, enabled = !state.isSending, onClick = onSendRequest)
            ProfileButton.REQUESTED -> BottomButton("신청함", filled = false, enabled = false, onClick = {})
            ProfileButton.NONE -> Unit
        }
    }
}

@Composable
private fun ProfileMenu(
    isMate: Boolean,
    notificationLabel: String,
    canReport: Boolean,
    onToggleNotification: () -> Unit,
    onRemoveMate: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = "더보기",
            tint = AppColor.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(AppRadius.pill))
                .clickable { expanded = true },
        )
        // 메이트별 알림·메이트 끊기는 정의서 10장에 있지만 화면 디자인이 없어 Material 기본 메뉴로 둔다
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = AppColor.Surface) {
            if (isMate) {
                DropdownMenuItem(
                    text = { Text(notificationLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary) },
                    onClick = { expanded = false; onToggleNotification() },
                )
                DropdownMenuItem(
                    text = { Text("메이트 끊기", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.DestructiveText) },
                    onClick = { expanded = false; onRemoveMate() },
                )
            }
            if (canReport) {
                DropdownMenuItem(
                    text = { Text("신고하기", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary) },
                    onClick = { expanded = false; onReport() },
                )
            }
            DropdownMenuItem(
                text = { Text("차단하기", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.DestructiveText) },
                onClick = { expanded = false; onBlock() },
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        content()
    }
}

@Composable
private fun TagCountChip(tag: TagCount, highlight: Boolean) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .background(if (highlight) AppColor.AccentSoft else AppColor.ChipNeutralBg, RoundedCornerShape(AppRadius.pill))
            .padding(horizontal = AppSpace.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
    ) {
        Text(
            text = tag.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) AppColor.AccentStrong else AppColor.TextBody,
        )
        Text(
            text = tag.count.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (highlight) AppColor.AccentStrong else AppColor.TextSecondary,
        )
    }
}

// 15a 현재 활동 카드
@Composable
private fun ActivityCard(activity: MateActivity, now: Long, timeZone: TimeZone, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, AppColor.Border, shape)
            .clickable(onClick = onClick)
            .padding(AppSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpace.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SportChip(activity.sport)
            Box(Modifier.weight(1f))
            if (activity.capacity > 0) {
                Text(
                    text = "${activity.confirmedCount}/${activity.capacity}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColor.AccentStrong,
                )
            }
        }
        Text(text = activity.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Text(text = activity.cardMeta(now, timeZone), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextBody)
    }
}

@Composable
private fun ReviewItem(review: ProfileReview, timeZone: TimeZone) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            MateAvatar(userId = review.authorId, nickname = review.authorNickname, size = 24.dp, fontSize = 11.sp)
            Text(text = review.authorNickname, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            Text(text = monthDayText(review.createdAtMillis, timeZone), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
        }
        review.tag?.let { tag ->
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .background(AppColor.AccentSoft, RoundedCornerShape(AppRadius.pill))
                    .padding(horizontal = AppSpace.lg),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = tag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColor.AccentStrong)
            }
        }
        if (review.comment.isNotBlank()) {
            Text(text = review.comment, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
        }
    }
}

@Composable
private fun BottomButton(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.md)
    Column(modifier = Modifier.background(AppColor.Surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
        Box(
            modifier = Modifier
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xl, bottom = AppSpace.xxl)
                .fillMaxWidth()
                .height(54.dp)
                .clip(shape)
                .background(if (filled) AppColor.Accent else AppColor.Surface)
                .then(if (filled) Modifier else Modifier.border(1.dp, AppColor.Border, shape))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (filled) AppColor.OnAccent else AppColor.TextDisabled,
            )
        }
    }
}

// ---------- 미리보기 ----------

private const val PREVIEW_NOW = 1788566400000L // 2026-09-05 09:00 (서울)
private const val DAY = 86_400_000L

private val previewProfile = MateProfile(
    userId = "w1",
    nickname = "서연",
    mannerTemperature = 44.1f,
    topTags = listOf(TagCount("시간 약속을 지켜요", 18), TagCount("서로 배려해요", 12), TagCount("안전하게 운동해요", 9)),
    relation = MateRelation.MATE,
    workedOutTogether = true,
    activities = listOf(
        MateActivity("a1", MateActivityKind.HOSTING, "퇴근 후 등·이두 같이 치실 분", "에이블짐 망원점", PREVIEW_NOW + DAY - 2 * 3_600_000L, 2, 3),
    ),
    reviewCount = 32,
    recentReviews = listOf(
        ProfileReview("u2", "지훈", PREVIEW_NOW - 5 * DAY, "시간 약속을 지켜요", "루틴도 잘 맞고 너무 즐거운 운동이었어요. 다음에 또 같이 하고 싶어요!"),
        ProfileReview("u3", "태윤", PREVIEW_NOW - 8 * DAY, "서로 배려해요", ""),
    ),
)

@Composable
private fun PreviewContent(profile: MateProfile) {
    MateProfileContent(
        state = MateProfileState(profile = profile, now = PREVIEW_NOW),
        onBack = {},
        onActivityClick = {},
        onAllReviewsClick = {},
        onSendRequest = {},
        onToggleNotification = {},
        onRemoveMate = {},
        timeZone = TimeZone.getTimeZone("Asia/Seoul"),
    )
}

@Preview(name = "15a 메이트 프로필 · 메이트", widthDp = 360, heightDp = 800)
@Composable
private fun Preview15a() = PreviewContent(previewProfile)

@Preview(name = "15b 메이트 프로필 · 메이트 아님", widthDp = 360, heightDp = 800)
@Composable
private fun Preview15b() = PreviewContent(
    previewProfile.copy(userId = "w3", nickname = "태윤", relation = MateRelation.NONE, activities = emptyList(), reviewCount = 6),
)

@Preview(name = "15c 메이트 프로필 · 신청 보냄", widthDp = 360, heightDp = 800)
@Composable
private fun Preview15c() = PreviewContent(
    previewProfile.copy(userId = "r2", nickname = "지훈", relation = MateRelation.REQUESTED, activities = emptyList(), reviewCount = 18),
)
