package com.example.myapplication.ui.explore


// GeoPoint와 두 점 사이의 거리 계산은 data/model/Geo.kt로 옮겼다(Repository도 쓰기 때문). 이 파일의 기존 이름은 그대로 쓸 수 있게 이어 준다.
typealias GeoPoint = com.example.myapplication.data.model.GeoPoint

fun distanceMeters(from: GeoPoint, toLatitude: Double, toLongitude: Double): Int =
    com.example.myapplication.data.model.distanceMeters(from, toLatitude, toLongitude)

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

