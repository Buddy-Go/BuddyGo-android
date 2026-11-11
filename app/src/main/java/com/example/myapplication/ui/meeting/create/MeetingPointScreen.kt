package com.example.myapplication.ui.meeting.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.GeoPoint
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.explore.LocationPermissions
import com.example.myapplication.ui.explore.fetchMyLocation
import com.example.myapplication.ui.explore.filter.ChevronLeftIcon
import com.example.myapplication.ui.explore.hasLocationPermission
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.record.SportIcon
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.SuitFontFamily

// 10g-4 집결지 지정 · 러닝/등산. 공원·산처럼 넓은 장소는 지도를 움직여 만날 곳에 핀을 맞추고, 만나는 지점을 상세주소로 적는다.
// 핀은 지도 가운데에 고정돼 있고, 지도를 멈추면 그 자리의 좌표가 집결지가 된다. (service-rules 4장)
@Composable
fun MeetingPointScreen(
    place: PlaceResult,
    sport: Sport,
    onBack: () -> Unit,
    onConfirmed: (PlaceSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var center by remember(place) { mutableStateOf(GeoPoint(place.latitude, place.longitude)) }
    var moveTo by remember { mutableStateOf<GeoPoint?>(null) }
    var detail by remember(place) { mutableStateOf("") }
    var hintShown by remember { mutableStateOf(true) }
    // 내 위치 버튼: 권한이 있으면 바로, 없으면 한 번 묻는다. 못 얻으면 아무것도 하지 않는다.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) fetchMyLocation(context) { it?.let { p -> moveTo = p } }
    }
    fun locate() {
        if (hasLocationPermission(context)) fetchMyLocation(context) { it?.let { p -> moveTo = p } }
        else permission.launch(LocationPermissions)
    }

    Box(modifier.fillMaxSize().background(AppColor.Surface)) {
        MeetingPointMap(
            initial = center,
            moveTo = moveTo,
            onCenterChange = { center = it; hintShown = false },
            modifier = Modifier.fillMaxSize(),
        )
        // 가운데 고정 핀: 끝(꼬리)이 지도 가운데를 가리키도록 위로 올려 놓는다.
        Box(Modifier.align(Alignment.Center).offset(y = (-26).dp)) {
            Box(
                Modifier.size(52.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(AppColor.Accent),
                contentAlignment = Alignment.Center,
            ) { SportIcon(sport, AppColor.TextPrimary, size = 26.dp) }
        }
        if (hintShown) {
            Text(
                "지도를 움직여 만날 곳에 맞춰주세요",
                Modifier.align(Alignment.Center).offset(y = (-100).dp).clip(RoundedCornerShape(12.dp)).background(AppColor.TextPrimary)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = AppColor.Surface, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            )
        }
        Row(Modifier.statusBarsPadding().fillMaxWidth().background(AppColor.Surface).padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).iconButton("뒤로", onBack), contentAlignment = Alignment.Center) {
                ChevronLeftIcon(AppColor.TextPrimary)
            }
            Spacer(Modifier.width(AppSpace.xl))
            Text("집결지 지정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
        // 내 위치로 이동
        Surface(
            Modifier.align(Alignment.BottomEnd).padding(end = AppSpace.xxl, bottom = 330.dp).size(52.dp).clip(CircleShape)
                .iconButton("내 위치로 이동", ::locate),
            shape = CircleShape, color = AppColor.Surface, shadowElevation = 6.dp,
        ) { Box(contentAlignment = Alignment.Center) { Text("◎", fontSize = 24.sp, color = AppColor.TextPrimary) } }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(AppColor.Surface).navigationBarsPadding().imePadding().padding(horizontal = AppSpace.x4).padding(top = AppSpace.x4, bottom = AppSpace.x4),
        ) {
            Text(place.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            Text(place.address, Modifier.padding(top = AppSpace.md, bottom = AppSpace.xxl), fontSize = 15.sp, color = AppColor.TextSecondary)
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
            Row(Modifier.padding(top = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
                Text("상세주소", Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text("선택", fontSize = 13.sp, color = AppColor.TextSecondary)
            }
            BasicTextField(
                value = detail, onValueChange = { detail = it.take(DETAIL_ADDRESS_MAX_LENGTH) }, singleLine = true,
                textStyle = TextStyle(fontFamily = SuitFontFamily, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary),
                cursorBrush = SolidColor(AppColor.TextPrimary),
                modifier = Modifier.fillMaxWidth().padding(top = AppSpace.xl, bottom = AppSpace.md),
                decorationBox = { inner ->
                    Box {
                        if (detail.isEmpty()) Text("예) 여의나루역 2번 출구 앞 편의점", fontSize = 17.sp, color = AppColor.TextDisabled)
                        inner()
                    }
                },
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
            Text("넓은 장소는 만나는 지점을 적어주면 찾기 쉬워요", Modifier.padding(top = AppSpace.xl, bottom = AppSpace.xxl), fontSize = 13.sp, color = AppColor.TextSecondary)
            ActionButton(text = "이 장소로 설정", onClick = {
                onConfirmed(meetingPointSelection(place, center, detail))
            })
        }
    }
}

// 집결지로 설정한 결과: 장소 이름·주소는 고른 곳 그대로 두고, 좌표는 지도에서 맞춘 가운데 좌표로 바꾼다. 상세주소는 비면 없는 것으로 본다.
fun meetingPointSelection(place: PlaceResult, center: GeoPoint, detail: String): PlaceSelection = PlaceSelection(
    name = place.name,
    detail = detail.trim().ifEmpty { null },
    address = place.address,
    lotAddress = place.lotAddress,
    latitude = center.latitude,
    longitude = center.longitude,
)
