package com.example.myapplication.ui.explore

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

// 서울 홍대 근처. 모임이 없을 때의 첫 화면 중심이다.
private val DefaultCenter = LatLng.from(37.5563, 126.9236)
private const val DefaultZoom = 15

private data class PinImageKey(val style: PinStyle, val count: Int, val favorite: Boolean)

// 카카오맵(View)을 Compose에 올린다. 핀은 pins 목록대로 라벨로 그리고, 선택된 핀은 라임으로 바꾼다.
// 지도가 뜨지 못하면(키 없음·네트워크 등) onError를 부른다.
@Composable
fun KakaoMapView(
    pins: List<PinGroup>,
    selectedKey: String?,
    onPinClick: (String) -> Unit,
    onMapClick: () -> Unit,
    onMovedByUser: () -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,    // 검색창·칩이 덮는 높이
    bottomPadding: Dp = 0.dp, // 시트가 덮는 높이. 두 값으로 지도 중심이 가려지지 않은 영역의 가운데가 된다.
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

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
                        (label.tag as? String)?.let(currentOnPinClick)
                        true
                    }
                    map.setOnMapClickListener { _, _, _, _ -> currentOnMapClick() }
                    // 코드로 옮긴 카메라(Unknown)는 빼고, 사용자가 손으로 옮긴 경우에만 알린다.
                    map.setOnCameraMoveEndListener { _, _, gesture ->
                        if (gesture != GestureType.Unknown) currentOnMoved()
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

    LaunchedEffect(kakaoMap, pins, selectedKey) {
        val map = kakaoMap ?: return@LaunchedEffect
        val layer = map.labelManager?.layer ?: return@LaunchedEffect
        layer.removeAll()
        val options = pins.map { pin ->
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
    // 핀을 고르거나 찜을 눌러도 가장 가까운 핀이 그대로면 움직이지 않아 사용자가 보던 위치를 지키고,
    // 핀이 하나도 없으면 그 자리에 둔다.
    val nearestKey = pins.firstOrNull()?.key
    LaunchedEffect(kakaoMap, nearestKey) {
        val map = kakaoMap ?: return@LaunchedEffect
        val first = pins.firstOrNull() ?: return@LaunchedEffect
        map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(first.latitude, first.longitude)))
    }
    LaunchedEffect(kakaoMap, selectedKey) {
        val map = kakaoMap ?: return@LaunchedEffect
        val selected = pins.firstOrNull { it.key == selectedKey } ?: return@LaunchedEffect
        map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(selected.latitude, selected.longitude)))
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
