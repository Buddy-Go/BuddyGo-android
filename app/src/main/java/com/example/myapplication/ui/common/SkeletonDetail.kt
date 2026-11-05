package com.example.myapplication.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 16b 상세 로딩. 칩·제목·정보 카드·호스트 카드·설명 카드와 하단 버튼 자리.
// 뒤로 가기 화살표는 화면이 그리고, 이 부품은 그 아래 내용을 채운다.
@Composable
fun SkeletonDetail(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppSpace.x4),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                SkeletonBlock(Modifier.width(70.dp).height(26.dp), RoundedCornerShape(AppRadius.pill))
                SkeletonBlock(Modifier.width(90.dp).height(26.dp), RoundedCornerShape(AppRadius.pill))
            }
            Spacer(Modifier.height(AppSpace.xl))
            SkeletonBlock(Modifier.width(294.dp).height(24.dp))
            Spacer(Modifier.height(9.dp))
            SkeletonBlock(Modifier.width(185.dp).height(24.dp))
            Spacer(Modifier.height(14.dp))
            SkeletonBlock(Modifier.width(110.dp).height(16.dp))
            Spacer(Modifier.height(10.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(5.dp))
            Spacer(Modifier.height(10.dp))
            SkeletonBlock(Modifier.width(224.dp).height(12.dp))

            Spacer(Modifier.height(AppSpace.xxl))
            InfoCard()
            Spacer(Modifier.height(AppSpace.xxl))
            HostCard()
            Spacer(Modifier.height(AppSpace.xxl))
            DescriptionCard()
        }
        BottomActionSkeleton()
    }
}

@Composable
private fun InfoCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColor.Border, RoundedCornerShape(AppRadius.lg))
            .padding(horizontal = 15.dp, vertical = 20.dp),
    ) {
        InfoRow(leftWidth = 40.dp, rightWidth = 110.dp)
        HorizontalDivider(Modifier.padding(vertical = 19.dp), color = AppColor.Border)
        InfoRow(leftWidth = 40.dp, rightWidth = 130.dp)
        HorizontalDivider(Modifier.padding(vertical = 19.dp), color = AppColor.Border)
        InfoRow(leftWidth = 60.dp, rightWidth = 80.dp)
    }
}

@Composable
private fun InfoRow(leftWidth: Dp, rightWidth: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(Modifier.width(leftWidth).height(14.dp))
        SkeletonBlock(Modifier.width(rightWidth).height(14.dp))
    }
}

@Composable
private fun HostCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColor.SurfaceMuted, RoundedCornerShape(AppRadius.lg))
            .padding(AppSpace.xxl - 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonCircle(44.dp)
        Spacer(Modifier.width(AppSpace.xl))
        Column {
            SkeletonBlock(Modifier.width(60.dp).height(14.dp))
            Spacer(Modifier.height(AppSpace.md))
            SkeletonBlock(Modifier.width(140.dp).height(12.dp))
        }
    }
}

@Composable
private fun DescriptionCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColor.Border, RoundedCornerShape(AppRadius.lg))
            .padding(15.dp),
    ) {
        SkeletonBlock(Modifier.width(70.dp).height(14.dp))
        Spacer(Modifier.height(AppSpace.xl))
        SkeletonBlock(Modifier.fillMaxWidth().height(12.dp))
        Spacer(Modifier.height(AppSpace.md))
        SkeletonBlock(Modifier.width(232.dp).height(12.dp))
    }
}

// 화면 아래 고정 영역: 작은 버튼(찜) + 큰 버튼(신청 등)
@Composable
private fun BottomActionSkeleton() {
    Column(Modifier.navigationBarsPadding()) {
        HorizontalDivider(color = AppColor.Border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpace.x4),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
        ) {
            SkeletonBlock(Modifier.size(54.dp), RoundedCornerShape(AppRadius.md))
            SkeletonBlock(Modifier.weight(1f).height(54.dp), RoundedCornerShape(AppRadius.md))
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SkeletonDetailPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize().padding(top = 56.dp)) { SkeletonDetail() }
    }
}
