package com.example.myapplication.ui.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace

// 2c-4 턱걸이 가이드 (2c 위에 뜨는 바텀시트)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullUpGuideBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.Surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = AppSpace.x4).padding(bottom = AppSpace.x4)) {
            GuideTitle("턱걸이 개수는 어떻게 세나요?", onDismiss)
            Spacer(Modifier.height(AppSpace.xl))
            Text("반동 없이 연속으로 할 수 있는 최대 개수예요. 정확히 세지 않아도 괜찮아요.", color = AppColor.TextPrimary, fontSize = 16.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(AppSpace.xxl))
            HorizontalDivider(color = AppColor.Border)
            Spacer(Modifier.height(AppSpace.xxl))
            Text("턱이 봉 위로 올라가는 것을 1회로 봅니다.", color = AppColor.TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            Text("오버그립 기준으로 세어주세요.", color = AppColor.TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
        }
    }
}
