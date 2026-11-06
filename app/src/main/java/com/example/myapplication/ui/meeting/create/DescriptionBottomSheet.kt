package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace

// 설명 시트 ← 10e. 선택 입력, 최대 DESCRIPTION_MAX_LENGTH자.
@Composable
fun DescriptionBottomSheet(
    initial: String,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    CreateSheetFrame(onDismiss = onDismiss) {
        DescriptionSheetBody(
            text = text,
            onTextChange = { text = it.take(DESCRIPTION_MAX_LENGTH) },
            onApply = { onApply(text.trim()) },
        )
    }
}

@Composable
internal fun DescriptionSheetBody(
    text: String,
    onTextChange: (String) -> Unit,
    onApply: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
        SheetTitle(title = "설명", subtitle = "선택 · 최대 ${DESCRIPTION_MAX_LENGTH}자")
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColor.TextPrimary,
                ),
                cursorBrush = SolidColor(AppColor.TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp),
            )
            Text(
                text = "${text.length} / $DESCRIPTION_MAX_LENGTH",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxs)) {
            Text(text = "이런 내용이 도움이 돼요", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColor.TextSecondary)
            Text(
                text = "진행 방식, 준비물, 집결 방법을 적어주세요. 확정된 메이트에게는 공지로 추가 안내를 보낼 수 있어요.",
                fontSize = 11.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextSecondary,
            )
        }
        // 설명은 선택이라 비어 있어도 적용할 수 있다.
        SheetOutlineButton(text = "적용", enabled = true, onClick = onApply)
    }
}
