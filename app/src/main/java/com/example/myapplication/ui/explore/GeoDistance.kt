package com.example.myapplication.ui.explore

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// 위도·경도 한 점 (내 위치 등)
data class GeoPoint(val latitude: Double, val longitude: Double)

// 내 위치를 알면 거리를 그 위치에서 다시 재고, 모르면 가져온 거리(서버 계산)를 그대로 쓴다.
// 좌표가 없는(0, 0) 모임은 잴 수 없어 가져온 값을 쓴다. 탐색과 모임 상세가 같은 값을 보여 주도록 이 함수 하나로 정한다.
fun meetingDistanceMeters(meeting: com.example.myapplication.data.model.Meeting, me: GeoPoint?): Int? {
    val place = meeting.place
    if (me == null || (place.latitude == 0.0 && place.longitude == 0.0)) return meeting.distanceMeters
    return distanceMeters(me, place.latitude, place.longitude)
}

// 마지막으로 알아낸 내 위치. 탐색에서 얻어 모임 상세처럼 다른 화면이 같은 위치를 쓰도록 앱 안에 하나만 둔다.
object LastKnownLocation {
    private val _point = kotlinx.coroutines.flow.MutableStateFlow<GeoPoint?>(null)
    val point: kotlinx.coroutines.flow.StateFlow<GeoPoint?> = _point

    fun set(location: GeoPoint) { _point.value = location }
    fun clear() { _point.value = null }
}

private const val EARTH_RADIUS_METERS = 6_371_000.0

// 두 점 사이의 직선거리(미터, 반올림). 하버사인 공식이라 가까운 거리에서 충분히 정확하다.
fun distanceMeters(from: GeoPoint, toLatitude: Double, toLongitude: Double): Int {
    val dLat = Math.toRadians(toLatitude - from.latitude)
    val dLng = Math.toRadians(toLongitude - from.longitude)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(toLatitude)) * sin(dLng / 2).pow(2)
    return (2 * EARTH_RADIUS_METERS * asin(sqrt(a))).roundToInt()
}
