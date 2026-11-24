package com.example.myapplication.data.local

// 탐색 지도를 마지막으로 본 자리(중심 좌표·배율). 다른 화면에 갔다 오거나 앱을 껐다 켜도 그 자리에서 이어 보게 한다.
// 값이 없을 때만 기본 위치(홍대)에서 시작한다. 로그아웃하면 지운다.
object MapCameraStore {
    data class Snapshot(val latitude: Double, val longitude: Double, val zoom: Int)

    private const val KEY = "map_camera"
    private var store: KeyValueStore? = null

    @Volatile var last: Snapshot? = null
        private set

    // 위치 조건 등으로 지도를 옮긴 '지점 id' 중 이미 적용한 것. 화면을 다시 만들 때 옛 지점으로 되돌아가지 않게 한다.
    @Volatile var appliedFocusId: Int = 0

    fun attach(store: KeyValueStore) {
        this.store = store
        last = store.get(KEY)?.let(::decode)
    }

    fun save(latitude: Double, longitude: Double, zoom: Int) {
        if (!latitude.isFinite() || !longitude.isFinite()) return
        val snapshot = Snapshot(latitude, longitude, zoom)
        last = snapshot
        store?.put(KEY, encode(snapshot))
    }

    fun clear() {
        last = null
        appliedFocusId = 0
        store?.put(KEY, "")
    }

    internal fun encode(s: Snapshot): String = "${s.latitude},${s.longitude},${s.zoom}"

    internal fun decode(text: String): Snapshot? {
        val parts = text.split(',')
        if (parts.size != 3) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        val zoom = parts[2].toIntOrNull() ?: return null
        return Snapshot(lat, lng, zoom)
    }
}
