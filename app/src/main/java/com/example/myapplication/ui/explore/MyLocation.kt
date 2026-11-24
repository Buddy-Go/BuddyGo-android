package com.example.myapplication.ui.explore

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal

val LocationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

// 정확한 위치나 대략적인 위치 중 하나라도 허용되어 있으면 true
fun hasLocationPermission(context: Context): Boolean = LocationPermissions.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

// 위치를 한 번 기다리는 최대 시간. 실내처럼 GPS 고정이 늦으면 이 안에 못 얻을 수 있어, 그때는 마지막으로 알려진 위치를 쓴다.
private const val LOCATION_TIMEOUT_MILLIS = 10_000L

// 마지막으로 알려진 위치를 대신 쓸 수 있는 나이. 체크인처럼 정확해야 하는 곳은 짧게, 지도 점은 조금 길게 잡는다.
private const val FETCH_LAST_KNOWN_MAX_AGE_MILLIS = 10 * 60_000L
private const val UPDATES_LAST_KNOWN_MAX_AGE_MILLIS = 30 * 60_000L

// 내 위치를 한 번 가져온다. 권한이 없거나, 위치 서비스가 모두 꺼져 있거나, 마지막 위치도 없으면 null을 돌려준다.
// 켜져 있는 제공자(GPS·네트워크·퓨즈드)에 동시에 물어 먼저 응답하는 값을 쓴다.
// 제한 시간(10초) 안에 아무도 답하지 않으면 기기가 알고 있는 가장 최근 위치로 대신한다. (실내에서 GPS가 늦은 경우)
@SuppressLint("MissingPermission") // 아래에서 hasLocationPermission으로 먼저 확인한다.
fun fetchMyLocation(context: Context, onResult: (GeoPoint?) -> Unit) {
    if (!hasLocationPermission(context)) return onResult(null)
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return onResult(null)
    val providers = enabledProviders(manager)
    if (providers.isEmpty()) return onResult(lastKnown(manager, FETCH_LAST_KNOWN_MAX_AGE_MILLIS)?.toGeoPoint())

    val main = Handler(Looper.getMainLooper())
    val signals = providers.map { CancellationSignal() }
    var finished = false
    var pending = providers.size

    fun finish(point: GeoPoint?) {
        if (finished) return
        finished = true
        main.removeCallbacksAndMessages(null)
        signals.forEach { runCatching { it.cancel() } }
        onResult(point ?: lastKnown(manager, FETCH_LAST_KNOWN_MAX_AGE_MILLIS)?.toGeoPoint())
    }

    main.postDelayed({ finish(null) }, LOCATION_TIMEOUT_MILLIS)
    providers.forEachIndexed { i, provider ->
        runCatching {
            LocationManagerCompat.getCurrentLocation(manager, provider, signals[i], ContextCompat.getMainExecutor(context)) { location ->
                if (location != null) finish(location.toGeoPoint())
                else if (--pending <= 0) finish(null) // 모든 제공자가 위치를 못 줬다
            }
        }.onFailure { if (--pending <= 0) finish(null) }
    }
}

// 화면이 보이는 동안 위치가 바뀔 때마다 알려 준다. 처음 한 번 위치를 못 얻어도 나중에 얻으면 알려 주므로 파란 점이 늦게라도 뜬다.
// 부르는 쪽이 돌려받은 함수를 호출하면 멈춘다. 권한이 없으면 아무것도 하지 않는다.
@SuppressLint("MissingPermission")
fun startMyLocationUpdates(context: Context, onLocation: (GeoPoint) -> Unit): () -> Unit {
    if (!hasLocationPermission(context)) return {}
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return {}
    lastKnown(manager, UPDATES_LAST_KNOWN_MAX_AGE_MILLIS)?.let { onLocation(it.toGeoPoint()) } // 기기가 이미 아는 위치로 먼저 그린다
    val listener = LocationListener { location -> onLocation(location.toGeoPoint()) }
    val started = enabledProviders(manager).mapNotNull { provider ->
        runCatching {
            // 5초마다, 20m 이상 움직였을 때
            manager.requestLocationUpdates(provider, 5_000L, 20f, listener, Looper.getMainLooper())
            provider
        }.getOrNull()
    }
    return { if (started.isNotEmpty()) runCatching { manager.removeUpdates(listener) } }
}

private fun enabledProviders(manager: LocationManager): List<String> {
    val candidates = buildList {
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
    }
    return candidates.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
}

// 모든 제공자 중 가장 최근에 알려진 위치(maxAgeMillis보다 오래된 것은 쓰지 않는다)
@SuppressLint("MissingPermission")
private fun lastKnown(manager: LocationManager, maxAgeMillis: Long): Location? =
    runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .filter { System.currentTimeMillis() - it.time <= maxAgeMillis }
        .maxByOrNull { it.time }

private fun Location.toGeoPoint() = GeoPoint(latitude, longitude)
