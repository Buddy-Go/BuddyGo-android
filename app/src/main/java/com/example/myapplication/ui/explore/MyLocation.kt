package com.example.myapplication.ui.explore

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal

val LocationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

// 정확한 위치나 대략적인 위치 중 하나라도 허용되어 있으면 true
fun hasLocationPermission(context: Context): Boolean = LocationPermissions.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

// 내 위치를 한 번 가져온다. 권한이 없거나 위치를 못 얻으면 null을 돌려준다.
// 위치 서비스가 켜져 있는 제공자(GPS·네트워크) 중 먼저 응답하는 값을 쓴다.
@SuppressLint("MissingPermission") // 아래에서 hasLocationPermission으로 먼저 확인한다.
fun fetchMyLocation(context: Context, onResult: (GeoPoint?) -> Unit) {
    if (!hasLocationPermission(context)) return onResult(null)
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return onResult(null)
    val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        ?: return onResult(null)
    runCatching {
        LocationManagerCompat.getCurrentLocation(
            manager, provider, CancellationSignal(), ContextCompat.getMainExecutor(context),
        ) { location -> onResult(location?.let { GeoPoint(it.latitude, it.longitude) }) }
    }.onFailure { onResult(null) }
}
