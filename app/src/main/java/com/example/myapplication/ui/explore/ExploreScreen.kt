package com.example.myapplication.ui.explore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.BuildConfig
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.filter.searchTitle
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 3-After 탐색. 지도 + 검색창·칩 + 하단 시트. (3-2-After 시트 내림, 3-3-After 핀 선택, 3-5 +n 핀 선택, 16e 부분 로딩)
@Composable
fun ExploreScreen(
    onFilterClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onMeetingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    // 내 위치: 권한이 있으면 바로 가져오고, 없으면 이번 실행에서 한 번만 묻는다. 거절하면 위치 없이 쓴다.
    val context = LocalContext.current
    var pendingMoveToMe by remember { mutableStateOf(false) } // 내 위치 버튼으로 권한을 물었으면, 허용 뒤 바로 옮긴다
    var locationGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val move = pendingMoveToMe
        pendingMoveToMe = false
        if (grants.values.any { it }) {
            locationGranted = true
            fetchMyLocation(context) { loc -> if (move) viewModel.onMoveToMyLocation(loc) else viewModel.onMyLocation(loc) }
        }
    }
    LaunchedEffect(Unit) {
        when {
            hasLocationPermission(context) -> fetchMyLocation(context, viewModel::onMyLocation)
            !viewModel.state.value.locationAsked -> {
                viewModel.onLocationAsked()
                permissionLauncher.launch(LocationPermissions)
            }
        }
    }
    // 화면이 보이는 동안 위치가 바뀌면 파란 점을 따라 옮긴다. 처음에 위치를 못 얻었어도 나중에 얻으면 그때 뜬다.
    DisposableEffect(locationGranted) {
        val stop = if (locationGranted) startMyLocationUpdates(context) { viewModel.onMyLocation(it) } else ({})
        onDispose { stop() }
    }
    // 화면이 다시 보일 때마다 읽지 않은 알림 여부를 새로 확인한다.
    LaunchedEffect(Unit) {
        viewModel.refreshNotifications()
        viewModel.refreshMeetings()
    }
    // 앱 키가 없으면 지도를 시작하지 않는다. (local.properties의 KAKAO_NATIVE_APP_KEY)
    var mapFailed by remember { mutableStateOf(BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) }

    // 목록을 못 불러오면 화면 전체를 오류로 덮는다. (16f, 16g)
    (state.meetings as? UiState.Error)?.let {
        FullScreenError(type = it.type, onRetry = viewModel::load, modifier = modifier, showHome = false)
        return
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val multiPin = state.selectedPin?.isMultiple == true
        val sheetHeight = maxHeight * if (multiPin) 0.75f else 0.6f
        val showFloatingCard = state.floatingCard != null
        val sheetBottomSpace = when {
            showFloatingCard -> 0.dp
            state.sheetExpanded -> sheetHeight
            else -> CollapsedSheetHeight
        }

        if (mapFailed) {
            MapUnavailable()
        } else {
            KakaoMapView(
                pins = state.pins,
                selectedKey = state.selectedPinKey,
                onPinClick = viewModel::onSelectPin,
                onMapClick = viewModel::onClearPin,
                onMovedByUser = { bounds -> viewModel.onCameraMoved(bounds) },
                onError = { mapFailed = true },
                modifier = Modifier.fillMaxSize(),
                myLocation = state.myLocation,
                focus = state.mapFocus,
                followNearestPin = state.followNearestPin,
                topPadding = TopOverlayHeight,
                bottomPadding = sheetBottomSpace,
            )
        }

        TopOverlay(
            state = state,
            onSearchClick = onFilterClick,
            onNotificationClick = onNotificationClick,
            onToggleFavoriteOnly = viewModel::onToggleFavoriteOnly,
        )

        // 네이버 지도처럼: 지도를 손으로 옮겨 보이는 범위가 달라졌을 때만 위쪽에 '현 지도에서 검색'을 띄운다.
        // 옮겼다고 목록을 바로 다시 부르지는 않고, 버튼을 눌렀을 때만 새 범위로 불러온다. 핀을 고른 동안(3-3, 3-5)에는 숨긴다.
        if (state.showResearchButton) {
            ResearchButton(
                onClick = viewModel::onResearch,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = ResearchButtonTop),
            )
        }

        // 내 위치 버튼: 지도를 보다가 내 위치로 돌아온다. 시트(또는 접힌 시트) 바로 위 오른쪽에 둔다.
        if (!mapFailed && !showFloatingCard) {
            MyLocationButton(
                onClick = {
                    if (hasLocationPermission(context)) {
                        // 위치를 새로 읽는 데 몇 초 걸릴 수 있어, 이미 아는 위치가 있으면 먼저 그리로 옮기고 뒤에서 위치를 갱신한다.
                        val moved = viewModel.onMoveToMyLocation(null)
                        fetchMyLocation(context) { loc ->
                            if (moved) viewModel.onMyLocation(loc)
                            else if (!viewModel.onMoveToMyLocation(loc)) {
                                android.widget.Toast.makeText(context, "현재 위치를 확인할 수 없어요. 위치 서비스를 확인해주세요.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        pendingMoveToMe = true
                        permissionLauncher.launch(LocationPermissions)
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = AppSpace.xxl, bottom = sheetBottomSpace + AppSpace.xl),
            )
        }

        if (showFloatingCard) {
            // 3-3-After: 모임이 하나뿐인 핀을 고르면 시트 대신 떠 있는 카드를 보여 준다.
            val meeting = state.floatingCard!!
            MeetingCard(
                meeting = meeting,
                onClick = { onMeetingClick(meeting.id) },
                onToggleFavorite = { viewModel.onToggleFavorite(meeting.id) },
                showCriteriaChip = true,
                modifier = Modifier.align(Alignment.BottomCenter).padding(AppSpace.xxl),
            )
        } else {
            ExploreBottomSheet(
                state = state,
                expandedHeight = sheetHeight,
                onSetExpanded = viewModel::onSetSheetExpanded,
                onMeetingClick = onMeetingClick,
                onToggleFavorite = viewModel::onToggleFavorite,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun TopOverlay(
    state: ExploreState,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onToggleFavoriteOnly: () -> Unit,
) {
    Column(Modifier.statusBarsPadding().padding(top = AppSpace.xl)) {
        Row(Modifier.padding(horizontal = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
            SearchBar(state, onClick = onSearchClick, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(AppSpace.md))
            Surface(
                modifier = Modifier.size(52.dp).clip(CircleShape).iconButton(notificationLabel(state.hasUnreadNotifications), onNotificationClick),
                shape = CircleShape, color = AppColor.Surface, shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BellIcon(AppColor.TextPrimary)
                    if (state.hasUnreadNotifications) {
                        Box(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp).size(7.dp).clip(CircleShape).background(Color(0xFFE5383B)))
                    }
                }
            }
        }
        Spacer(Modifier.height(AppSpace.xl))
        LazyRow(
            contentPadding = PaddingValues(horizontal = AppSpace.xxl),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                FilterChip(selected = state.favoriteOnly, onClick = onToggleFavoriteOnly) {
                    HeartIcon(filled = state.favoriteOnly, color = if (state.favoriteOnly) Color(0xFFE5383B) else AppColor.TextPrimary, size = 16.dp)
                    Text("찜", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun SearchBar(state: ExploreState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).clickable(onClickLabel = "조건 설정 열기", role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(26.dp), color = AppColor.Surface, shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(horizontal = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
            SearchIcon(AppColor.TextPrimary)
            Spacer(Modifier.width(AppSpace.xl))
            Column(Modifier.weight(1f)) {
                Text(
                    searchTitle(state.filter, System.currentTimeMillis(), onlySport = com.example.myapplication.data.model.SportSupport.supported.singleOrNull()),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary,
                )
                Text(
                    "${if (state.hasLocationCondition) "설정한 지역" else "현재 지도 범위"} · 모임 ${state.visibleMeetings.size}개",
                    fontSize = 12.sp, color = AppColor.TextSecondary,
                )
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(AppColor.ChipNeutralBg), contentAlignment = Alignment.Center) {
                FilterIcon(AppColor.TextPrimary)
            }
        }
    }
}

@Composable
private fun FilterChip(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).selectableItem(selected, onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AppColor.Accent else AppColor.Surface,
        shadowElevation = 3.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
    }
}

// '현 지도에서 검색'. 지도를 옮긴 뒤에만 보이므로 늘 라임색으로 눈에 띄게 한다.
@Composable
private fun ResearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = AppColor.Accent,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
        ) {
            RefreshIcon(AppColor.TextPrimary)
            Text("현 지도에서 검색", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
    }
}

// 내 위치로 돌아오는 회색 원형 버튼
@Composable
private fun MyLocationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(48.dp).clip(CircleShape).iconButton("내 위치로 이동", onClick),
        shape = CircleShape, color = AppColor.Surface, shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { MyLocationIcon(Color(0xFF6B7280)) }
    }
}

// 지도를 못 그릴 때(앱 키 없음·네트워크 문제 등). 목록과 칩은 그대로 쓸 수 있다.
@Composable
private fun MapUnavailable() {
    Box(Modifier.fillMaxSize().background(AppColor.SurfaceMuted), contentAlignment = Alignment.Center) {
        Text("지도를 불러오지 못했어요", color = AppColor.TextSecondary, fontSize = 15.sp)
    }
}

// 상태바 아래 검색창과 칩 줄이 차지하는 높이(대략). 지도 중심 계산에만 쓴다.
private val TopOverlayHeight = 168.dp

// 검색창과 칩 줄 바로 아래 가운데에 '현 지도에서 검색'을 놓는 위치(상태바 아래 기준)
private val ResearchButtonTop = 134.dp

// 안드로이드 스튜디오 미리보기. 펀치홀 있는 기기(Pixel 8)에 상태바까지 그려서 상단 여백을 바로 본다.
// 실제 앱의 Scaffold는 상단 인셋을 주지 않으므로(AppNavHost), 여기서도 화면이 직접 statusBarsPadding()을 적용한 모습 그대로다.
@Preview(name = "탐색 · 상단 여백 (Pixel 8)", device = "id:pixel_8", showSystemUi = true)
@Composable
private fun ExploreTopPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize()) {
            MapUnavailable()
            TopOverlay(state = ExploreState(), onSearchClick = {}, onNotificationClick = {}, onToggleFavoriteOnly = {})
        }
    }
}
