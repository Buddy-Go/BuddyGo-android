package com.example.myapplication.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

enum class ListFooterState {
    Idle,    // 아무것도 보이지 않음
    Loading, // 16d 추가 로딩
    Error,   // 16h 부분 실패
}

// 목록 맨 아래 표시. 기존 목록은 그대로 두고 이 부품만 아래에 붙인다.
@Composable
fun ListFooter(
    state: ListFooterState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loadingText: String = "불러오는 중",
    errorText: String = "더 불러오지 못했어요",
) {
    when (state) {
        ListFooterState.Idle -> Unit
        ListFooterState.Loading -> InlineLoading(loadingText, modifier.padding(vertical = AppSpace.x4))
        ListFooterState.Error -> Row(
            modifier = modifier
                .fillMaxWidth()
                .background(AppColor.SurfaceMuted, RoundedCornerShape(AppRadius.md))
                .padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExclamationIcon(AppColor.IconMuted, size = 20.dp)
            Spacer(Modifier.width(AppSpace.xl))
            Text(
                text = errorText,
                color = AppColor.TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "다시 시도",
                color = AppColor.AccentStrong,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRetry),
            )
        }
    }
}

// 작은 로딩 표시 (진한 초록 링 + 회색 글자). 목록 아래 '불러오는 중'과
// 당겨서 새로고침의 '새로고침 중'(16d)에 같은 모양을 쓴다. 당기는 동작은 화면이 처리한다.
@Composable
fun InlineLoading(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = AppColor.AccentStrong,
            trackColor = AppColor.SkeletonBase,
            strokeWidth = 2.5.dp,
        )
        Spacer(Modifier.width(AppSpace.xl))
        Text(text, color = AppColor.TextSecondary, fontSize = 14.sp)
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ListFooterPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Column(Modifier.padding(AppSpace.xxl), verticalArrangement = Arrangement.spacedBy(AppSpace.x4)) {
            InlineLoading("새로고침 중")
            ListFooter(ListFooterState.Loading, onRetry = {})
            ListFooter(ListFooterState.Error, onRetry = {}, errorText = "모임을 더 불러오지 못했어요")
        }
    }
}
