package com.example.myapplication.data.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// 위도·경도 한 점 (내 위치 등)
data class GeoPoint(val latitude: Double, val longitude: Double)

private const val EARTH_RADIUS_METERS = 6_371_000.0

// 두 점 사이의 직선거리(미터, 반올림). 하버사인 공식이라 가까운 거리에서 충분히 정확하다.
fun distanceMeters(from: GeoPoint, toLatitude: Double, toLongitude: Double): Int {
    val dLat = Math.toRadians(toLatitude - from.latitude)
    val dLng = Math.toRadians(toLongitude - from.longitude)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(toLatitude)) * sin(dLng / 2).pow(2)
    return (2 * EARTH_RADIUS_METERS * asin(sqrt(a))).roundToInt()
}

// 지도에 보이는 직사각형 영역 ('이 지역 재검색'의 기준, service-rules 2장). 남서쪽 모서리와 북동쪽 모서리로 나타낸다.
// 날짜변경선을 걸치는 영역은 다루지 않는다(국내 서비스).
data class MapBounds(val south: Double, val west: Double, val north: Double, val east: Double) {
    // 경계 위의 점도 안으로 본다.
    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in south..north && longitude in west..east

    companion object {
        // 화면 모서리 두 점으로 만든다. 어느 쪽이 위·아래여도 맞게 정렬한다.
        fun of(lat1: Double, lng1: Double, lat2: Double, lng2: Double) =
            MapBounds(minOf(lat1, lat2), minOf(lng1, lng2), maxOf(lat1, lat2), maxOf(lng1, lng2))
    }
}
