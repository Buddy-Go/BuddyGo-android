package com.example.myapplication.ui.meeting.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 확정 탭 ← 7b, 7d(마감 후 — 탭 없이 이 내용만)
@Composable
fun ConfirmedTab(members: List<ConfirmedMember>, hint: String) {
    Column {
        members.forEachIndexed { index, member ->
            if (index > 0) ListDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpace.xxl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
            ) {
                InitialAvatar(member.nickname)
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xs)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
                    ) {
                        NicknameText(member.nickname)
                        if (member.isHost) HostTag()
                    }
                    MannerText(member.mannerLabel)
                }
            }
        }
        HintText(text = hint, modifier = Modifier.padding(top = AppSpace.xxl))
    }
}

// ---------- 7번 화면이 같이 쓰는 작은 부품 ----------

// 프로필 사진 대신 닉네임 첫 글자 (40dp 원)
@Composable
internal fun InitialAvatar(nickname: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(AppColor.AvatarBg, RoundedCornerShape(AppRadius.pill)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nickname.take(1),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.TextBody,
        )
    }
}

@Composable
internal fun NicknameText(nickname: String) {
    Text(text = nickname, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
}

@Composable
internal fun MannerText(text: String) {
    Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextSecondary)
}

@Composable
private fun HostTag() {
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

@Composable
internal fun ListDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppColor.DividerSoft),
    )
}

@Composable
internal fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        color = AppColor.TextSecondary,
        modifier = modifier,
    )
}
