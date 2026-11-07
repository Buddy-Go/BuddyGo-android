package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.GeoPoint
import com.example.myapplication.ui.theme.AppColor
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory

// 10g-4 집결지 지정용 지도: 손으로 움직일 수 있고, 핀은 지도 가운데에 고정돼 있다(화면이 그린다).
// 지도를 움직이다 멈추면 가운데 좌표를 onCenterChange로 알린다. moveTo가 바뀌면 카메라를 그 좌표로 옮긴다('내 위치').
// 앱 키가 없거나 지도가 뜨지 못하면 안내 문구를 보이고, 처음 좌표를 그대로 집결지로 쓴다.
@Composable
fun MeetingPointMap(
    initial: GeoPoint,
    moveTo: GeoPoint?,
    onCenterChange: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Int = 0,
) {
    var failed by remember { mutableStateOf(BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) }
    if (failed) {
        Box(modifier.background(AppColor.SurfaceMuted), contentAlignment = Alignment.Center) {
            Text("지도를 불러오지 못했어요", fontSize = 14.sp, color = AppColor.TextSecondary)
        }
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val mapView = remember { MapView(context) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    val onChange by rememberUpdatedState(onCenterChange)
    val start = LatLng.from(initial.latitude, initial.longitude)

    DisposableEffect(mapView) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}
                override fun onMapError(error: Exception) { failed = true }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    map.setOnCameraMoveEndListener { m, position, _ ->
                        position.position.let { onChange(GeoPoint(it.latitude, it.longitude)) }
                    }
                    kakaoMap = map
                }

                override fun getPosition(): LatLng = start
                override fun getZoomLevel(): Int = 16
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
    // '내 위치' 같은 요청이 오면 카메라를 옮긴다.
    LaunchedEffect(kakaoMap, moveTo) {
        val map = kakaoMap ?: return@LaunchedEffect
        val target = moveTo ?: return@LaunchedEffect
        map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(target.latitude, target.longitude)))
        onChange(target)
    }
    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}
