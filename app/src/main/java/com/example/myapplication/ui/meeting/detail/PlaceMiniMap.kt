package com.example.myapplication.ui.meeting.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.explore.PinBitmap
import com.example.myapplication.ui.explore.PinLook
import com.example.myapplication.ui.explore.PinStyle
import com.example.myapplication.ui.theme.AppColor
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

// 장소 시트(5e)의 작은 지도. 모임 장소 한 곳에 핀을 꽂아 보여 주기만 하고 손으로 움직일 수 없다.
// 앱 키가 없거나 지도가 뜨지 못하면 안내 문구를 보여 준다.
@Composable
fun PlaceMiniMap(latitude: Double, longitude: Double, sport: Sport, modifier: Modifier = Modifier, look: PinLook = PinLook.Default) {
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
    val position = LatLng.from(latitude, longitude)

    DisposableEffect(mapView) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}
                override fun onMapError(error: Exception) { failed = true }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    // 보기만 하는 지도라서 모든 손짓을 막는다.
                    GestureType.entries.forEach { runCatching { map.setGestureEnable(it, false) } }
                    val bitmap = PinBitmap.create(context, PinStyle(sport, look), count = 1, hasFavorite = false)
                    map.labelManager?.layer?.addLabel(
                        LabelOptions.from(position)
                            .setStyles(LabelStyles.from(LabelStyle.from(bitmap).setApplyDpScale(false).setAnchorPoint(0.5f, 1f))),
                    )
                }

                override fun getPosition(): LatLng = position
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
            mapView.finish()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}
