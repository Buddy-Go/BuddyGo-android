package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.myapplication.ui.theme.SuitFontFamily

// 상세주소 시트 (모임 수정). 선택 입력이고 비워도 적용할 수 있다. (service-rules 4장)
@Composable
fun DetailAddressBottomSheet(
    placeName: String,
    initial: String,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    CreateSheetFrame(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
            SheetTitle(title = "상세주소", subtitle = placeName)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(DETAIL_ADDRESS_MAX_LENGTH) },
                    textStyle = TextStyle(fontFamily = SuitFontFamily, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary),
                    cursorBrush = SolidColor(AppColor.TextPrimary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) {
                                Text("예) 3층 프리웨이트 존", fontSize = 15.sp, color = AppColor.TextSecondary)
                            }
                            inner()
                        }
                    },
                )
                Text(
                    text = "${text.length} / $DETAIL_ADDRESS_MAX_LENGTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SheetOutlineButton(text = "적용", enabled = true, onClick = { onApply(text.trim()) })
        }
    }
}
