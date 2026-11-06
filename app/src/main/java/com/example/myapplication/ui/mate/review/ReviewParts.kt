package com.example.myapplication.ui.mate.review

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.repository.ReviewSession
import com.example.myapplication.data.repository.ReviewTargetInfo
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 평가 화면들이 같이 쓰는 부품

// 사람마다 다른 색의 이니셜 원 (12a: 보라·회색·파랑 …). userId로 색이 정해져서 화면이 바뀌어도 같다.
private val avatarPalette = listOf(
    AppColor.AvatarPurpleBg to AppColor.AvatarPurpleText,
    AppColor.AvatarBg to AppColor.TextBody,
    AppColor.AvatarBlueBg to AppColor.AvatarBlueText,
    AppColor.HostAvatarBg to AppColor.HostAvatarText,
)

internal fun avatarColors(userId: String): Pair<Color, Color> =
    avatarPalette[Math.floorMod(userId.hashCode(), avatarPalette.size)]

@Composable
internal fun ReviewAvatar(userId: String, nickname: String, size: Dp = 44.dp) {
    val (bg, fg) = avatarColors(userId)
    Box(
        modifier = Modifier
            .size(size)
            .background(bg, RoundedCornerShape(AppRadius.pill)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = nickname.take(1), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// 56dp 상단 바 (12a: < 뒤로 / 12b: X 닫기 + 오른쪽 '2 / 3')
@Composable
internal fun ReviewTopBar(@DrawableRes iconRes: Int, iconDescription: String, onIconClick: () -> Unit, trailingText: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = iconDescription,
            tint = AppColor.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onIconClick),
        )
        Text(
            text = "메이트 평가",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(text = trailingText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColor.TextSecondary)
        }
    }
}

// 맨 아래 큰 버튼 (12b 제출, 12c 활동 내역으로)
@Composable
internal fun ReviewBottomButton(text: String, enabled: Boolean, onClick: () -> Unit, showTopLine: Boolean) {
    Column(modifier = Modifier.background(AppColor.Surface)) {
        if (showTopLine) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xl, bottom = AppSpace.xxl)
                .height(54.dp)
                .background(if (enabled) AppColor.Accent else AppColor.DisabledBg, RoundedCornerShape(AppRadius.md))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) AppColor.OnAccent else AppColor.TextDisabled,
            )
        }
    }
}

// ---------- 미리보기용 예시 (12a 디자인과 같은 사람들) ----------

internal val previewReviewState = ReviewState(
    session = ReviewSession(
        meetingId = "m1",
        meetingTitle = "등·이두 같이 치실 분",
        meetingStartAtMillis = 1789380000000L, // 2026-09-14 19:00 (서울)
        placeName = "스포애니 합정점",
        reviewDueAtMillis = 1789984800000L, // 2026-09-21 19:00 (서울)
        targets = listOf(
            ReviewTargetInfo("u4", "서연", isReviewed = false, isMate = false, isMateRequested = false),
            ReviewTargetInfo("u5", "진현", isReviewed = true, isMate = true, isMateRequested = false),
            ReviewTargetInfo("u6", "우진", isReviewed = false, isMate = false, isMateRequested = true),
        ),
    ),
)
