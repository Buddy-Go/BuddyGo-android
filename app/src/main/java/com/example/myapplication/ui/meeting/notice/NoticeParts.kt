package com.example.myapplication.ui.meeting.notice

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 공지 화면 세 개가 같이 쓰는 부품

// 56dp 상단 바: 왼쪽 아이콘(X 또는 <) + 제목 + 오른쪽 내용
@Composable
internal fun NoticeTopBar(
    @DrawableRes leadingIcon: Int,
    leadingDescription: String,
    title: String,
    onLeadingClick: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
    ) {
        Icon(
            painter = painterResource(leadingIcon),
            contentDescription = leadingDescription,
            tint = AppColor.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onLeadingClick),
        )
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

// ⋯ 버튼과 눌렀을 때 뜨는 메뉴.
// 메뉴 모양은 디자인에 없어서 Material 기본 메뉴를 쓴다 (팀에 질문 대상).
data class MoreMenuItem(val label: String, val isDestructive: Boolean = false, val onClick: () -> Unit)

@Composable
internal fun MoreMenuButton(
    items: List<MoreMenuItem>,
    iconSize: Dp,
    tint: Color,
    description: String = "더보기",
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Icon(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = description,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(AppRadius.pill))
                .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppColor.Surface,
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isDestructive) AppColor.DestructiveText else AppColor.TextPrimary,
                        )
                    },
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                )
            }
        }
    }
}

@Composable
internal fun NoticeDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppColor.DividerSoft),
    )
}
