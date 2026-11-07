package com.example.myapplication.ui.mypage

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.ReceivedReview
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.model.TagCount
import com.example.myapplication.ui.common.ConfirmDialog
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.common.SkeletonList
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.common.sportLabel
import com.example.myapplication.ui.explore.BellIcon
import com.example.myapplication.ui.explore.ThermometerIcon
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 14 마이페이지
// 위치 설정, 신고·차단 관리 줄은 정의서(1장, 13장)와 달라 넣지 않았다.
@Composable
fun MyPageScreen(
    onLogout: () -> Unit,
    onAllReviewsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MyPageViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var confirmLogout by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refresh() }

    when (val s = state) {
        UiState.Loading -> Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding()) {
            Text("마이페이지", Modifier.padding(AppSpace.xxl), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            SkeletonList(Modifier.padding(horizontal = AppSpace.xxl))
        }
        is UiState.Error -> FullScreenError(type = s.type, onRetry = viewModel::load, modifier = modifier)
        is UiState.Success -> {
            val d = s.data
            Column(modifier.fillMaxSize().background(AppColor.Surface).verticalScroll(rememberScrollState())) {
                ProfileHeader(d)
                if (d.stats.topTags.isNotEmpty()) TopTags(d.stats.topTags)
                ReviewSection(d.reviews, onAllReviewsClick)
                Spacer(Modifier.height(AppSpace.xxl))
                MenuRow("알림 설정", trailing = notificationStatusText(d.notificationEnabled), onClick = viewModel::onToggleNotification, leading = { BellIcon(AppColor.TextPrimary) })
                MenuRow("로그아웃", muted = true, onClick = { confirmLogout = true }, leading = { LogoutIcon() })
                Spacer(Modifier.height(AppSpace.x5))
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "로그아웃할까요?",
            confirmText = "로그아웃",
            dismissText = "취소",
            onConfirm = { confirmLogout = false; viewModel.onLogout(onLogout) },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
private fun ProfileHeader(d: MyPageData) {
    Column(
        Modifier.fillMaxWidth()
            .background(AppColor.Accent)
            .statusBarsPadding()
            .padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl),
    ) {
        Text("마이페이지", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(AppSpace.xxl))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(AppColor.AccentSoft), contentAlignment = Alignment.Center) {
                Text(avatarInitial(d.user.nickname), color = AppColor.AccentStrong, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(AppSpace.xxl))
            Column {
                Text(d.user.nickname, color = AppColor.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                val subtitle = profileSubtitle(d.user)
                if (subtitle.isNotEmpty()) Text(subtitle, color = AppColor.AccentStrong, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(AppSpace.xxl))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(AppRadius.lg)).background(Color.White.copy(alpha = .3f))
                .padding(vertical = AppSpace.xl),
        ) {
            Stat("매너온도", formatTemperature(d.user.mannerTemperature), Modifier.weight(1.3f), thermometer = true)
            Stat("받은 후기", d.stats.receivedReviewCount.toString(), Modifier.weight(1f))
            Stat("개설한 모임", d.stats.hostedMeetingCount.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, thermometer: Boolean = false) {
    Column(modifier.padding(horizontal = AppSpace.xxl).semantics(mergeDescendants = true) { contentDescription = "$label $value" }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (thermometer) {
                ThermometerIcon(AppColor.TextPrimary, size = 16.dp)
                Spacer(Modifier.width(4.dp))
            }
            Text(value, color = AppColor.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = AppColor.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = AppColor.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopTags(tags: List<TagCount>) {
    Column(Modifier.padding(horizontal = AppSpace.x4).padding(top = AppSpace.x5)) {
        SectionTitle("많이 받은 태그")
        Spacer(Modifier.height(AppSpace.xl))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpace.md), verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            tags.forEachIndexed { i, t ->
                val first = i == 0
                Row(
                    Modifier.clip(CircleShape).background(if (first) AppColor.AccentSoft else AppColor.ChipNeutralBg)
                        .padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
                ) {
                    Text(t.label, color = if (first) AppColor.AccentStrong else AppColor.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(AppSpace.md))
                    Text(t.count.toString(), color = if (first) AppColor.AccentStrong else AppColor.TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ReviewSection(reviews: List<ReceivedReview>, onAllClick: () -> Unit) {
    Column(Modifier.padding(horizontal = AppSpace.x4).padding(top = AppSpace.x5)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("받은 후기", Modifier.weight(1f))
            if (reviews.isNotEmpty()) {
                Text("전체 보기", Modifier.clip(CircleShape).clickable(onClickLabel = "받은 후기 전체 보기", onClick = onAllClick).padding(AppSpace.md), color = AppColor.TextSecondary, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(AppSpace.xl))
        if (reviews.isEmpty()) {
            Text("아직 받은 후기가 없어요", color = AppColor.TextDisabled, fontSize = 15.sp, modifier = Modifier.padding(vertical = AppSpace.xl))
        } else {
            // 가장 최근 후기 한 건만 보여 준다. (디자인 14)
            ReviewCard(reviews.first())
        }
    }
}

@Composable
private fun ReviewCard(r: ReceivedReview) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(AppColor.Surface)
            .border(1.dp, AppColor.Border, shape)
            .padding(AppSpace.xxl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F1FF)), contentAlignment = Alignment.Center) {
                Text(avatarInitial(r.reviewerNickname), color = Color(0xFF2563EB), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(AppSpace.xl))
            Text(r.reviewerNickname, color = AppColor.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(AppSpace.md))
            Text("${sportLabel(r.sport)} · ${formatReviewDate(r.createdAtMillis)}", color = AppColor.TextDisabled, fontSize = 13.sp, modifier = Modifier.weight(1f))
            VerdictChip(r.verdict)
        }
        r.comment?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(AppSpace.xl))
            Text(it, color = AppColor.TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun VerdictChip(verdict: ReviewVerdict) {
    val (bg, fg) = when (verdict) {
        ReviewVerdict.GOOD -> AppColor.AccentSoft to AppColor.AccentStrong
        ReviewVerdict.NORMAL -> AppColor.ChipNeutralBg to AppColor.TextSecondary
        ReviewVerdict.BAD -> AppColor.DestructiveSoftBg to AppColor.DestructiveText
    }
    Text(
        verdictLabel(verdict),
        Modifier.clip(RoundedCornerShape(AppRadius.sm)).background(bg).padding(horizontal = AppSpace.xl, vertical = 4.dp),
        color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun MenuRow(
    label: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    muted: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppSpace.x4, vertical = AppSpace.x4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { leading() }
        Spacer(Modifier.width(AppSpace.xxl))
        Text(label, Modifier.weight(1f), color = if (muted) AppColor.TextSecondary else AppColor.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        trailing?.let { Text(it, color = AppColor.TextSecondary, fontSize = 15.sp) }
    }
}

@Composable
private fun LogoutIcon() {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val sw = 1.8.dp.toPx()
        val c = AppColor.IconMuted
        drawLine(c, Offset(w * .08f, w * .5f), Offset(w * .62f, w * .5f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * .45f, w * .3f), Offset(w * .65f, w * .5f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * .45f, w * .7f), Offset(w * .65f, w * .5f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * .75f, w * .15f), Offset(w * .92f, w * .15f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * .92f, w * .15f), Offset(w * .92f, w * .85f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * .75f, w * .85f), Offset(w * .92f, w * .85f), sw, StrokeCap.Round)
    }
}
