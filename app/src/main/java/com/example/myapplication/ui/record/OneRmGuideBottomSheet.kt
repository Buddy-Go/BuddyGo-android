package com.example.myapplication.ui.record

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace

// 2c-3 1RM 가이드 (2c 위에 뜨는 바텀시트)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRmGuideBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.Surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = AppSpace.x4).padding(bottom = AppSpace.x4)) {
            GuideTitle("1RM이 뭔가요?", onDismiss)
            Spacer(Modifier.height(AppSpace.xl))
            Text("딱 한 번만 들 수 있는 최대 무게예요. 실제로 측정해볼 필요는 없어요.", color = AppColor.TextPrimary, fontSize = 16.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(AppSpace.xxl))
            HorizontalDivider(color = AppColor.Border)
            Spacer(Modifier.height(AppSpace.xxl))
            Text("모른다면 이렇게 계산해보세요", color = AppColor.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(AppSpace.xl))
            Text("평소 8~10회 드는 무게 × 1.3", color = AppColor.AccentStrong, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(AppSpace.xl))
            Text("예) 벤치프레스 60kg으로 10회 > 60 × 1.3 = 78kg", color = AppColor.TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            Text("스쿼트 100kg으로 8회 > 100 × 1.3 = 130kg", color = AppColor.TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(AppSpace.xxl))
            HorizontalDivider(color = AppColor.Border)
            Spacer(Modifier.height(AppSpace.xxl))
            Text("어림값이어도 괜찮아요. 비슷한 수준의 메이트를 찾는 용도라 10kg 정도 차이는 문제되지 않아요.", color = AppColor.TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}

// 시트 위쪽 제목 줄: 제목 + 닫기(×)
@Composable
internal fun GuideTitle(title: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = AppColor.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val sw = 2.2.dp.toPx()
                drawLine(AppColor.TextPrimary, Offset(0f, 0f), Offset(size.width, size.height), sw, StrokeCap.Round)
                drawLine(AppColor.TextPrimary, Offset(size.width, 0f), Offset(0f, size.height), sw, StrokeCap.Round)
            }
        }
    }
}
