package com.example.myapplication.ui.mypage

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.local.BlockedUser
import com.example.myapplication.data.local.BlockedUsersStore
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 신고 · 차단 관리: 내가 차단한 사람 목록과 차단 해제. 마이페이지 14의 행에서 들어온다.
// 디자인이 없어 다른 목록 화면(11c 받은 신청)의 모양을 따른 임시 화면이다.
@Composable
fun BlockManageScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val blocked by BlockedUsersStore.blocked.collectAsState()
    BlockManageContent(blocked = blocked, onBack = onBack, onUnblock = BlockedUsersStore::unblock, modifier = modifier)
}

@Composable
internal fun BlockManageContent(
    blocked: List<BlockedUser>,
    onBack: () -> Unit,
    onUnblock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = AppSpace.xxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "뒤로 가기",
                tint = AppColor.TextPrimary,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
            Text("신고 · 차단 관리", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
        if (blocked.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("차단한 사람이 없어요", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColor.TextDisabled)
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = AppSpace.x4)) {
                Text(
                    "차단한 사람은 메이트와 함께 운동한 사람 목록에 나오지 않아요",
                    fontSize = 13.sp, color = AppColor.TextSecondary, modifier = Modifier.padding(vertical = AppSpace.lg),
                )
                blocked.forEach { user ->
                    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(AppColor.ChipNeutralBg), contentAlignment = Alignment.Center) {
                            Text(avatarInitial(user.nickname), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.TextBody)
                        }
                        Spacer(Modifier.size(AppSpace.xl))
                        Text(user.nickname, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                        Text(
                            "차단 해제",
                            Modifier
                                .clip(RoundedCornerShape(AppRadius.pill))
                                .clickable { onUnblock(user.userId) }
                                .padding(horizontal = AppSpace.xl, vertical = AppSpace.md),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColor.AccentStrong,
                        )
                    }
                }
            }
        }
    }
}
