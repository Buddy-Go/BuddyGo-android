package com.example.myapplication.ui.meeting.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.explore.LocationPermissions
import com.example.myapplication.ui.explore.LocationSettingsDialog
import com.example.myapplication.ui.explore.SearchIcon
import com.example.myapplication.ui.explore.fetchMyLocation
import com.example.myapplication.ui.explore.filter.ChevronLeftIcon
import com.example.myapplication.ui.explore.filter.ChevronRightIcon
import com.example.myapplication.ui.explore.filter.CloseIcon
import com.example.myapplication.ui.explore.formatDistance
import com.example.myapplication.ui.explore.hasLocationPermission
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.explore.isLocationPermanentlyDenied
import com.example.myapplication.ui.explore.openAppSettings
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.SuitFontFamily

// 10g 장소 검색, 10g-2 검색 결과. 장소를 고르면 장소 확인(10g-3)으로 간다.
@Composable
fun PlaceSearchScreen(
    onBack: () -> Unit,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceSearchViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) {
            fetchMyLocation(context, viewModel::onNearby)
        } else {
            viewModel.onNearby(null)
            // 다시 묻지 않음으로 거절했으면 설정으로 안내한다.
            if (isLocationPermanentlyDenied(context)) showSettings = true
        }
    }
    fun findNearby() {
        if (hasLocationPermission(context)) fetchMyLocation(context, viewModel::onNearby)
        else permissionLauncher.launch(LocationPermissions)
    }

    if (showSettings) {
        LocationSettingsDialog(
            onOpenSettings = { showSettings = false; openAppSettings(context) },
            onDismiss = { showSettings = false },
        )
    }
    Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).iconButton("뒤로", onBack), contentAlignment = Alignment.Center) {
                ChevronLeftIcon(AppColor.TextPrimary)
            }
            Spacer(Modifier.width(AppSpace.xl))
            Text("장소", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
        SearchField(state.query, viewModel::onQueryChange, viewModel::onClearQuery)
        Spacer(Modifier.height(AppSpace.xl))
        if (!state.showsResults) {
            NearbyRow(onClick = ::findNearby)
            Box(Modifier.padding(horizontal = AppSpace.x4).fillMaxWidth().height(1.dp).background(AppColor.Border))
            RecentList(state.recent, onSelect = { viewModel.onSelect(it); onSelected() }, onRemove = viewModel::onRemoveRecent)
        } else {
            ResultList(state, query = state.query, onSelect = { viewModel.onSelect(it); onSelected() }, onRetry = { if (state.searchedNearby) findNearby() else viewModel.onQueryChange(state.query) })
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.padding(horizontal = AppSpace.x4).fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(24.dp)).background(AppColor.ChipNeutralBg).padding(horizontal = AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchIcon(AppColor.TextSecondary, size = 20.dp)
        Spacer(Modifier.width(AppSpace.xl))
        BasicTextField(
            value = query, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(fontFamily = SuitFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary),
            cursorBrush = SolidColor(AppColor.TextPrimary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("헬스장 이름으로 검색", fontSize = 16.sp, color = AppColor.TextDisabled)
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(AppColor.TextDisabled).iconButton("검색어 지우기", onClear), contentAlignment = Alignment.Center) {
                CloseIcon(AppColor.Surface, size = 12.dp)
            }
        }
    }
}

@Composable
private fun NearbyRow(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "현재 위치 주변에서 찾기", onClick = onClick).padding(horizontal = AppSpace.x4, vertical = AppSpace.x4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
            val w = size.width; val c = androidx.compose.ui.geometry.Offset(w / 2, w / 2); val sw = 1.8.dp.toPx()
            drawCircle(AppColor.TextPrimary, w * .26f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
            drawCircle(AppColor.TextPrimary, w * .08f, c)
            listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 1f).forEach { }
            drawLine(AppColor.TextPrimary, androidx.compose.ui.geometry.Offset(w / 2, w * .02f), androidx.compose.ui.geometry.Offset(w / 2, w * .2f), sw, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(AppColor.TextPrimary, androidx.compose.ui.geometry.Offset(w / 2, w * .8f), androidx.compose.ui.geometry.Offset(w / 2, w * .98f), sw, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(AppColor.TextPrimary, androidx.compose.ui.geometry.Offset(w * .02f, w / 2), androidx.compose.ui.geometry.Offset(w * .2f, w / 2), sw, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(AppColor.TextPrimary, androidx.compose.ui.geometry.Offset(w * .8f, w / 2), androidx.compose.ui.geometry.Offset(w * .98f, w / 2), sw, androidx.compose.ui.graphics.StrokeCap.Round)
        }
        Spacer(Modifier.width(AppSpace.xl))
        Text("현재 위치 주변에서 찾기", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
        ChevronRightIcon(AppColor.TextSecondary)
    }
}

@Composable
private fun RecentList(recent: List<PlaceResult>, onSelect: (PlaceResult) -> Unit, onRemove: (PlaceResult) -> Unit) {
    if (recent.isEmpty()) return
    Text("최근 장소", Modifier.padding(horizontal = AppSpace.x4).padding(top = AppSpace.xxl, bottom = AppSpace.md), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
    LazyColumn {
        items(recent, key = { it.name + it.address }) { p ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(p) }.padding(horizontal = AppSpace.x4, vertical = AppSpace.xl), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
                    Text(p.address, fontSize = 14.sp, color = AppColor.TextSecondary)
                }
                Box(Modifier.size(32.dp).clip(CircleShape).iconButton("최근 장소 지우기", { onRemove(p) }), contentAlignment = Alignment.Center) {
                    CloseIcon(AppColor.TextDisabled, size = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun ResultList(state: PlaceSearchState, query: String, onSelect: (PlaceResult) -> Unit, onRetry: () -> Unit) {
    when {
        state.failed -> Message("장소를 불러오지 못했어요", "다시 시도", onRetry)
        state.loading && state.results.isEmpty() -> Message("찾는 중이에요", null, {})
        state.isEmptyResult -> Message("검색 결과가 없어요", null, {})
        else -> LazyColumn {
            items(state.results, key = { it.name + it.address + it.latitude }) { p ->
                Row(Modifier.fillMaxWidth().clickable { onSelect(p) }.padding(horizontal = AppSpace.x4, vertical = AppSpace.xl), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(highlighted(p.name, query), fontSize = 17.sp, color = AppColor.TextSecondary)
                        Text(p.address, fontSize = 14.sp, color = AppColor.TextSecondary)
                    }
                    formatDistance(p.distanceMeters)?.let { Text(it, fontSize = 14.sp, color = AppColor.TextSecondary) }
                }
            }
        }
    }
}

// 이름에서 검색어와 맞는 부분을 굵고 진하게 (10g-2 "스포애니 합정점")
@Composable
private fun highlighted(name: String, query: String) = buildAnnotatedString {
    val at = if (query.isBlank()) -1 else name.indexOf(query.trim(), ignoreCase = true)
    if (at < 0) {
        append(name)
    } else {
        append(name.substring(0, at))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)) { append(name.substring(at, at + query.trim().length)) }
        append(name.substring(at + query.trim().length))
    }
}

@Composable
private fun Message(text: String, action: String?, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = AppSpace.x5 * 3), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
        Text(text, fontSize = 15.sp, color = AppColor.TextSecondary)
        if (action != null) {
            Text(action, Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(AppSpace.xl), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.AccentStrong)
        }
    }
}
