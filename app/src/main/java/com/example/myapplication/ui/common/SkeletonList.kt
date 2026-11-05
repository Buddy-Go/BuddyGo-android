package com.example.myapplication.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 16a 리스트 로딩. 헤더 한 줄 + 카드 [count]개.
// 탐색의 지도·검색창·시트 틀은 화면이 그리고, 시트 안 목록 자리에 이 부품을 넣는다.
@Composable
fun SkeletonList(modifier: Modifier = Modifier, count: Int = 3) {
    Column(
        modifier = modifier.padding(horizontal = AppSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpace.xl),
    ) {
        SkeletonBlock(Modifier.width(110.dp).height(18.dp))
        repeat(count) { index -> SkeletonCard(wide = index % 2 == 0) }
    }
}

// 모임 카드 모양의 로딩 자리. wide는 제목·부제목 줄 길이를 번갈아 다르게 하기 위한 값이다.
@Composable
private fun SkeletonCard(wide: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColor.Border, RoundedCornerShape(AppRadius.lg))
            .padding(AppSpace.xxl - 1.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBlock(Modifier.width(56.dp).height(24.dp), RoundedCornerShape(AppRadius.pill))
            if (wide) SkeletonBlock(Modifier.width(44.dp).height(24.dp), RoundedCornerShape(AppRadius.pill))
        }
        Spacer(Modifier.height(10.dp))
        SkeletonBlock(Modifier.width(if (wide) 232.dp else 203.dp).height(20.dp))
        Spacer(Modifier.height(10.dp))
        SkeletonBlock(Modifier.width(if (wide) 164.dp else 134.dp).height(14.dp))
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonCircle(24.dp)
                Spacer(Modifier.width(AppSpace.md))
                SkeletonBlock(Modifier.width(40.dp).height(11.dp))
            }
            SkeletonBlock(Modifier.width(52.dp).height(6.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SkeletonListPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Column {
            Spacer(Modifier.height(24.dp))
            SkeletonList()
        }
    }
}
