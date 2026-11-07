package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.explore.filter.ChevronLeftIcon
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.meeting.detail.PlaceMiniMap
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 10g-3 장소 확인 · 헬스. 지도에서 위치를 보고 상세주소(선택)를 적은 뒤 '이 장소로 설정'한다.
// (러닝·등산의 집결지 지정 10g-4는 헬스만 개설하는 범위 변경으로 만들지 않았다.)
@Composable
fun PlaceConfirmScreen(
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val place by PlaceSelectionStore.pending.collectAsState()
    val picked = place
    if (picked == null) {
        // 고른 장소 없이 들어온 경우(앱이 다시 시작된 직후 등)는 검색으로 돌려보낸다.
        Box(modifier.fillMaxSize().background(AppColor.Surface), contentAlignment = Alignment.Center) { Text("장소를 먼저 골라 주세요", color = AppColor.TextSecondary) }
        return
    }
    // 러닝·등산은 가게 위치가 아니라 지도에서 집결지를 맞추는 화면(10g-4)이다.
    val sport = PlaceSelectionStore.sport
    if (sport != Sport.HEALTH) {
        MeetingPointScreen(
            place = picked, sport = sport, onBack = onBack,
            onConfirmed = { selection -> PlaceSelectionStore.confirm(selection); onConfirmed() },
            modifier = modifier,
        )
        return
    }
    var detail by remember(picked) { mutableStateOf("") }
    Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).iconButton("뒤로", onBack), contentAlignment = Alignment.Center) {
                ChevronLeftIcon(AppColor.TextPrimary)
            }
            Spacer(Modifier.width(AppSpace.xl))
            Text("장소 확인", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
        PlaceMiniMap(picked.latitude, picked.longitude, Sport.HEALTH, Modifier.fillMaxWidth().height(200.dp))
        Column(Modifier.weight(1f).padding(horizontal = AppSpace.x4)) {
            Text(picked.name, Modifier.padding(top = AppSpace.x4), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            Text(picked.address, Modifier.padding(top = AppSpace.md, bottom = AppSpace.x4), fontSize = 15.sp, color = AppColor.TextSecondary)
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
            Row(Modifier.padding(top = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
                Text("상세주소", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text("선택", fontSize = 13.sp, color = AppColor.TextSecondary)
            }
            BasicTextField(
                value = detail, onValueChange = { detail = it.take(DETAIL_ADDRESS_MAX_LENGTH) }, singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary),
                cursorBrush = SolidColor(AppColor.TextPrimary),
                modifier = Modifier.fillMaxWidth().padding(top = AppSpace.xl, bottom = AppSpace.md),
                decorationBox = { inner ->
                    Box {
                        if (detail.isEmpty()) Text("예) 3층 프리웨이트 존", fontSize = 17.sp, color = AppColor.TextDisabled)
                        inner()
                    }
                },
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
        Box(Modifier.padding(AppSpace.x4)) {
            ActionButton(text = "이 장소로 설정", onClick = {
                PlaceSelectionStore.confirm(
                    PlaceSelection(
                        name = picked.name, detail = detail.trim().ifEmpty { null }, address = picked.address,
                        lotAddress = picked.lotAddress, latitude = picked.latitude, longitude = picked.longitude,
                    ),
                )
                onConfirmed()
            })
        }
    }
}
