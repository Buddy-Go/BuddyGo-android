package com.example.myapplication.ui.mate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 메이트 화면들이 같이 쓰는 부품

// 이니셜 원. userId로 색이 정해져서 화면이 바뀌어도 같다 (디자인: 회색·초록·연회색+초록 글자)
private val matePalette = listOf(
    AppColor.AvatarBg to AppColor.TextBody,
    AppColor.HostAvatarBg to AppColor.HostAvatarText,
    AppColor.NeutralSoft to AppColor.AccentStrong,
)

@Composable
internal fun MateAvatar(userId: String, nickname: String, size: Dp = 40.dp, fontSize: TextUnit = 15.sp) {
    val (bg, fg) = matePalette[Math.floorMod(userId.hashCode(), matePalette.size)]
    Box(
        modifier = Modifier
            .size(size)
            .background(bg, RoundedCornerShape(AppRadius.pill)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = nickname.take(1), fontSize = fontSize, fontWeight = FontWeight.Bold, color = fg)
    }
}

// 11b 오른쪽: '메이트 신청'(초록 테두리) / '신청함'(회색 테두리) / '메이트'(글자만)
@Composable
internal fun RelationAction(relation: MateRelation, onRequest: () -> Unit) {
    when (relation) {
        MateRelation.MATE -> Text(text = "메이트", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextDisabled)
        MateRelation.REQUESTED -> PillButton("신청함", AppColor.Border, AppColor.TextSecondary, enabled = false, onClick = {})
        MateRelation.NONE -> PillButton("메이트 신청", AppColor.Accent, AppColor.AccentStrong, enabled = true, onClick = onRequest)
    }
}

@Composable
private fun PillButton(text: String, borderColor: Color, textColor: Color, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.pill)
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = AppSpace.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

// 🌡 매너온도 (11c, 15)
@Composable
internal fun MannerLine(text: String, fontSize: TextUnit = 12.sp, iconSize: Dp = 14.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_thermometer),
            contentDescription = null,
            tint = AppColor.AccentStrong,
            modifier = Modifier
                .padding(end = AppSpace.xs)
                .size(iconSize),
        )
        Text(text = text, fontSize = fontSize, fontWeight = FontWeight.ExtraBold, color = AppColor.AccentStrong)
    }
}
