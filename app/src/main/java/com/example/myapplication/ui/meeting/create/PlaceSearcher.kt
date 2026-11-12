package com.example.myapplication.ui.meeting.create

import com.example.myapplication.BuildConfig
import com.example.myapplication.ui.explore.GeoPoint
import com.example.myapplication.ui.explore.distanceMeters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// 장소 검색(10g, 10g-2)에서 쓰는 장소 한 곳. 거리는 내 위치를 알 때만 채운다.
data class PlaceResult(
    val name: String,
    val address: String,        // 도로명 주소. 없으면 지번
    val lotAddress: String,     // 지번 주소. 같은 지번이면 지도 핀 하나로 묶는다 (service-rules 2장)
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int? = null,
    val kakaoPlaceId: String? = null, // 카카오 장소 id. 서버가 같은 장소를 하나로 합치는 키라 개설 요청에 실어 보낸다
)

// 검색어로 장소를 찾는다. near가 있으면 가까운 순으로 찾고 거리를 채운다. 실패하면 예외를 던진다.
fun interface PlaceSearcher {
    suspend fun search(query: String, near: GeoPoint?): List<PlaceResult>
}

// 카카오 로컬 API 키워드 검색. REST API 키가 없으면 예시 장소(SamplePlaces)에서 찾는다.
class KakaoPlaceSearcher(private val restApiKey: String = BuildConfig.KAKAO_REST_API_KEY) : PlaceSearcher {
    override suspend fun search(query: String, near: GeoPoint?): List<PlaceResult> {
        if (restApiKey.isBlank()) return SamplePlaces.search(query, near)
        val location = near?.let { "&x=${it.longitude}&y=${it.latitude}&radius=20000&sort=distance" }.orEmpty()
        val url = URL("https://dapi.kakao.com/v2/local/search/keyword.json?size=15&query=${URLEncoder.encode(query, "UTF-8")}$location")
        val json = withContext(Dispatchers.IO) {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.setRequestProperty("Authorization", "KakaoAK $restApiKey")
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                if (conn.responseCode !in 200..299) error("카카오 로컬 API 오류: ${conn.responseCode}")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
        return KakaoPlaceParser.parse(json, near)
    }
}

object KakaoPlaceParser {
    fun parse(json: String, near: GeoPoint?): List<PlaceResult> {
        val docs = JSONObject(json).optJSONArray("documents") ?: return emptyList()
        return (0 until docs.length()).mapNotNull { i ->
            val d = docs.getJSONObject(i)
            val name = d.optString("place_name")
            val lat = d.optString("y").toDoubleOrNull()
            val lng = d.optString("x").toDoubleOrNull()
            if (name.isBlank() || lat == null || lng == null) return@mapNotNull null
            val lot = d.optString("address_name")
            val address = d.optString("road_address_name").ifBlank { lot }
            // 카카오가 거리를 주면 그 값을, 아니면 내 위치에서 직접 잰다.
            val meters = d.optString("distance").toIntOrNull() ?: near?.let { distanceMeters(it, lat, lng) }
            PlaceResult(name, address, lot, lat, lng, meters, d.optString("id").ifBlank { null })
        }
    }
}

// 키가 없는 환경(테스트, CI)에서 쓰는 예시 장소. 헬스장 위주로 서울 몇 곳만 둔다.
object SamplePlaces {
    val all = listOf(
        PlaceResult("스포애니 합정점", "서울 마포구 양화로 45", "서울 마포구 서교동 464-1", 37.5496, 126.9139),
        PlaceResult("스포애니 망원점", "서울 마포구 월드컵로 120", "서울 마포구 망원동 411-3", 37.5557, 126.9014),
        PlaceResult("스포애니 신촌점", "서울 서대문구 연세로 10", "서울 서대문구 창천동 30-1", 37.5598, 126.9426),
        PlaceResult("에이블짐 홍대점", "서울 마포구 와우산로 94", "서울 마포구 상수동 331-5", 37.5502, 126.9231),
        PlaceResult("여의도 한강공원", "서울 영등포구 여의동로 330", "서울 영등포구 여의도동 2", 37.5284, 126.9326),
    )

    fun search(query: String, near: GeoPoint?): List<PlaceResult> {
        val q = query.trim().replace(" ", "")
        if (q.isEmpty()) return emptyList()
        val found = all.filter { it.name.replace(" ", "").contains(q, ignoreCase = true) || it.address.replace(" ", "").contains(q) }
        val withDistance = found.map { p -> p.copy(distanceMeters = near?.let { distanceMeters(it, p.latitude, p.longitude) }) }
        return if (near != null) withDistance.sortedBy { it.distanceMeters } else withDistance
    }
}
