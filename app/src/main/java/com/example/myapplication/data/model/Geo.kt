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
