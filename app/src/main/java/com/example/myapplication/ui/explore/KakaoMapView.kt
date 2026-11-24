package com.example.myapplication.ui.explore

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.data.model.GeoPoint
import com.example.myapplication.data.model.MapBounds
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.CompetitionType
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

// 서울 홍대 근처. 모임이 없을 때의 첫 화면 중심이다.
private val DefaultCenter = LatLng.from(37.5563, 126.9236)
private const val DefaultZoom = 15

// 위치 조건(구·동)을 고르면 지도를 그 지역이 한눈에 들어오는 배율로 옮긴다.
private const val DistrictZoom = 13

private const val MyLocationLayerId = "my-location"

// 내 위치로 옮길 때의 배율 (동네가 보이는 정도)
// 내 위치 점 레이어의 우선순위. 지도 기본 아이콘(호텔·가게 표시 등) 위에 그려져야 점이 가려지지 않고,
// 모임 핀(기본 레이어, 10000)보다는 아래여야 핀을 누를 수 있다. (0으로 두면 지도 아이콘 아래로 깔려 점이 가려졌다)
private const val MyLocationLayerZOrder = 9999

const val MyLocationZoom = 16

// 이보다 멀리(작은 숫자) 볼 수 없다. 너무 넓게 보면 핀이 의미가 없어서 구 몇 개가 보이는 정도에서 막는다.
const val MinMapZoom = 11

// 파란 점 + 흰 테두리 + 옅은 파란 번짐
private fun myLocationDot(density: Float): Bitmap {
    val size = (36 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val c = size / 2f
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.color = 0x332F6BFF; canvas.drawCircle(c, c, c, paint)
    paint.color = android.graphics.Color.WHITE; canvas.drawCircle(c, c, 9 * density, paint)
    paint.color = 0xFF2F6BFF.toInt(); canvas.drawCircle(c, c, 7 * density, paint)
    return bitmap
}

private data class PinImageKey(val style: PinStyle, val count: Int, val favorite: Boolean)

// 카카오맵(View)을 Compose에 올린다. 핀은 pins 목록대로 라벨로 그리고, 선택된 핀은 라임으로 바꾼다.
// 지도가 뜨지 못하면(키 없음·네트워크 등) onError를 부른다.
@Composable
fun KakaoMapView(
    pins: List<PinGroup>,
    selectedKey: String?,
    onPinClick: (String) -> Unit,
    onMapClick: () -> Unit,
    onMovedByUser: (MapBounds?) -> Unit, // 옮긴 뒤 지도에 보이는 영역(읽지 못하면 null)
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    myLocation: GeoPoint? = null, // 있으면 파란 점으로 표시한다
    focus: MapFocus? = null, // 바뀌면 지도를 그 지점으로 한 번 옮긴다 (위치 조건으로 고른 지역)
    followNearestPin: Boolean = true, // false면 핀이 바뀌어도 카메라를 가장 가까운 핀으로 옮기지 않는다 ('현 지도에서 검색'으로 사용자가 고른 영역을 지킨다)
    topPadding: Dp = 0.dp,    // 검색창·칩이 덮는 높이
    bottomPadding: Dp = 0.dp, // 시트가 덮는 높이. 두 값으로 지도 중심이 가려지지 않은 영역의 가운데가 된다.
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var zoomLevel by remember { mutableIntStateOf(DefaultZoom) } // 지금 배율. 핀 묶음을 정한다.

    val currentOnPinClick by rememberUpdatedState(onPinClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMoved by rememberUpdatedState(onMovedByUser)
    val currentOnError by rememberUpdatedState(onError)

    DisposableEffect(mapView) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}
                override fun onMapError(error: Exception) = currentOnError()
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    map.setOnLabelClickListener { _, _, label ->
                        val tag = label.tag as? String
                        if (tag != null && isClusterKey(tag)) {
                            // 묶음 핀: 그 자리로 확대해서 핀을 풀어 보여 준다.
                            map.moveCamera(
                                CameraUpdateFactory.newCenterPosition(label.position, minOf(zoomLevel + 2, CLUSTER_MAX_ZOOM + 1)),
                            )
                        } else {
                            tag?.let(currentOnPinClick)
                        }
                        true
                    }
                    map.setOnMapClickListener { _, _, _, _ -> currentOnMapClick() }
                    // 코드로 옮긴 카메라(Unknown)는 빼고, 사용자가 손으로 옮긴 경우에만 알린다.
                    map.setOnCameraMoveEndListener { _, position, gesture ->
                        if (position.zoomLevel < MinMapZoom) {
                            // 너무 멀리 줄였으면 가장 넓은 배율로 되돌린다.
                            map.moveCamera(CameraUpdateFactory.zoomTo(MinMapZoom))
                            return@setOnCameraMoveEndListener
                        }
                        zoomLevel = position.zoomLevel
                        if (gesture != GestureType.Unknown) currentOnMoved(visibleBounds(map, mapView.width, mapView.height))
                    }
                    kakaoMap = map
                }

                override fun getPosition(): LatLng = DefaultCenter
                override fun getZoomLevel(): Int = DefaultZoom
            },
        )
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            kakaoMap = null
            mapView.finish()
        }
    }

    val density = LocalDensity.current.density
    LaunchedEffect(kakaoMap, topPadding, bottomPadding) {
        kakaoMap?.setPadding(0, (topPadding.value * density).toInt(), 0, (bottomPadding.value * density).toInt())
    }

    // 같은 모양의 핀 비트맵은 한 번만 그린다.
    val bitmaps = remember { mutableMapOf<PinImageKey, Bitmap>() }

    // 멀리서 볼 때는 가까운 핀을 묶어 개수로 보여 준다. 고른 핀은 묶지 않는다.
    val shownPins = remember(pins, zoomLevel, selectedKey) { clusterPins(pins, zoomLevel, selectedKey) }

    LaunchedEffect(kakaoMap, shownPins, selectedKey) {
        val map = kakaoMap ?: return@LaunchedEffect
        val layer = map.labelManager?.layer ?: return@LaunchedEffect
        layer.removeAll()
        val options = shownPins.map { pin ->
            val style = pinStyleOf(pin, selectedKey)
            val key = PinImageKey(style, pin.count, pin.hasFavorite())
            val bitmap = bitmaps.getOrPut(key) { PinBitmap.create(context, style, pin.count, key.favorite) }
            LabelOptions.from(LatLng.from(pin.latitude, pin.longitude))
                .setStyles(LabelStyles.from(LabelStyle.from(bitmap).setApplyDpScale(false).setAnchorPoint(0.5f, 1f)))
                .setTag(pin.key)
                .setClickable(true)
                .setRank(if (style.look == PinLook.Selected) 1L else 0L)
        }
        if (options.isNotEmpty()) layer.addLabels(options)
    }

    // 보여 줄 핀이 달라지면(처음 불러올 때, 종목·찜·조건을 바꿨을 때) 가장 가까운 핀으로 카메라를 옮긴다.
    // 핀을 골라도 카메라는 옮기지 않는다. 찜을 눌러도 가장 가까운 핀이 그대로면 움직이지 않아 사용자가 보던 위치를 지키고,
    // 핀이 하나도 없으면 그 자리에 둔다. '현 지도에서 검색'으로 영역을 고른 뒤에는 사용자가 옮긴 카메라를 그대로 둔다.
    val nearestKey = pins.firstOrNull()?.key
    LaunchedEffect(kakaoMap, nearestKey) {
        if (!followNearestPin) return@LaunchedEffect
        val map = kakaoMap ?: return@LaunchedEffect
        val first = pins.firstOrNull() ?: return@LaunchedEffect
        map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(first.latitude, first.longitude)))
    }
    // 위치 조건으로 지역을 고르면 그 지역 중심으로 옮긴다. 핀이 있으면 아래 핀 이동이 이어서 그 지역의 가장 가까운 핀으로 맞춘다.
    LaunchedEffect(kakaoMap, focus) {
        val map = kakaoMap ?: return@LaunchedEffect
        val target = focus ?: return@LaunchedEffect
        map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(target.point.latitude, target.point.longitude), target.zoom ?: DistrictZoom))
    }

    // 내 위치 파란 점. 핀과 다른 레이어에 그려 핀을 다시 그려도 지워지지 않고, 핀 아래에 깔린다.
    val dotBitmap = remember { myLocationDot(context.resources.displayMetrics.density) }
    LaunchedEffect(kakaoMap, myLocation) {
        val map = kakaoMap ?: return@LaunchedEffect
        val manager = map.labelManager ?: return@LaunchedEffect
        // 다른 라벨(핀, 지도의 가게 이름 등)과 겹쳐도 가려지지 않게 경쟁하지 않는 레이어에 그린다.
        // (기본값이면 겹칠 때 파란 점이 숨어서 '가끔 안 보이는' 문제가 있었다)
        val layer = manager.getLayer(MyLocationLayerId)
            ?: manager.addLayer(LabelLayerOptions.from(MyLocationLayerId).setCompetitionType(CompetitionType.None).setZOrder(MyLocationLayerZOrder).setClickable(false))
            ?: return@LaunchedEffect
        layer.removeAll()
        val here = myLocation ?: return@LaunchedEffect
        layer.addLabel(
            LabelOptions.from(LatLng.from(here.latitude, here.longitude))
                .setStyles(LabelStyles.from(LabelStyle.from(dotBitmap).setApplyDpScale(false).setAnchorPoint(0.5f, 0.5f)))
                .setRank(Long.MAX_VALUE)
                .setClickable(false),
        )
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

// 지도에 보이는 영역: 화면의 왼쪽 위와 오른쪽 아래 모서리를 좌표로 바꿔 만든다. 바꾸지 못하면(지도가 아직 준비 중 등) null.
private fun visibleBounds(map: KakaoMap, width: Int, height: Int): MapBounds? {
    if (width <= 0 || height <= 0) return null
    val topLeft = runCatching { map.fromScreenPoint(0, 0) }.getOrNull() ?: return null
    val bottomRight = runCatching { map.fromScreenPoint(width, height) }.getOrNull() ?: return null
    return MapBounds.of(topLeft.latitude, topLeft.longitude, bottomRight.latitude, bottomRight.longitude)
}
