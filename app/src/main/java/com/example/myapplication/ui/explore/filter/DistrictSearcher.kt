package com.example.myapplication.ui.explore.filter

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// 위치 검색(4g)에서 검색어로 지역(시·군·구, 동)을 찾는 곳. 실패하면 예외를 던진다.
fun interface DistrictSearcher {
    suspend fun search(query: String): List<District>
}

// 카카오 로컬 API로 전국 지역을 찾는다. REST API 키가 없으면 서울 예시 목록(DistrictCatalog)으로 찾는다.
// 주소 검색에서 결과가 없으면(예: "마포"처럼 일부만 친 경우) 키워드 검색 결과의 주소에서 지역을 뽑는다.
class KakaoDistrictSearcher(private val restApiKey: String = BuildConfig.KAKAO_REST_API_KEY) : DistrictSearcher {
    override suspend fun search(query: String): List<District> {
        if (restApiKey.isBlank()) return DistrictCatalog.search(query)
        val byAddress = KakaoLocalParser.parseAddress(get("address", query))
        if (byAddress.isNotEmpty()) return byAddress
        return withRegionCenters(KakaoLocalParser.parseKeyword(get("keyword", query)))
    }

    // 키워드 검색이 준 좌표는 그 가게의 위치라 지역 중심이 아니다. 위쪽 몇 곳은 주소 검색으로 지역 중심을 다시 받아 바꾼다.
    // 못 받으면 가게 좌표를 그대로 둔다. (지도가 그 지역 안으로 옮겨 가는 데는 충분하다)
    private suspend fun withRegionCenters(found: List<District>): List<District> =
        found.mapIndexed { i, d ->
            if (i >= MAX_CENTER_LOOKUPS) return@mapIndexed d
            val center = runCatching { KakaoLocalParser.parseAddress(get("address", d.fullName)).firstOrNull()?.center }.getOrNull()
            if (center != null) d.copy(center = center) else d
        }

    private suspend fun get(kind: String, query: String): String = withContext(Dispatchers.IO) {
        val url = URL("https://dapi.kakao.com/v2/local/search/$kind.json?size=15&query=${URLEncoder.encode(query, "UTF-8")}")
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
}

private const val MAX_CENTER_LOOKUPS = 5

// 카카오 로컬 API 응답에서 지역만 뽑는다. 이름(name)은 가장 작은 단위(동, 없으면 구·시), 전체 이름은 "시도 구 동".
object KakaoLocalParser {
    fun parseAddress(json: String): List<District> {
        val docs = JSONObject(json).optJSONArray("documents") ?: return emptyList()
        val found = (0 until docs.length()).mapNotNull { i ->
            val doc = docs.getJSONObject(i)
            val a = doc.optJSONObject("address") ?: return@mapNotNull null
            district(
                a.optString("region_1depth_name"), a.optString("region_2depth_name"), a.optString("region_3depth_name"),
                center = point(doc.optString("x").ifBlank { a.optString("x") }, doc.optString("y").ifBlank { a.optString("y") }),
            )
        }
        return found.distinctBy { it.fullName }
    }

    // 키워드 검색의 address_name은 "서울 마포구 합정동 123-4"처럼 온다. 앞의 시도·구·동 조각만 쓴다.
    fun parseKeyword(json: String): List<District> {
        val docs = JSONObject(json).optJSONArray("documents") ?: return emptyList()
        val found = (0 until docs.length()).mapNotNull { i ->
            val doc = docs.getJSONObject(i)
            val parts = doc.optString("address_name").split(" ").filter { it.isNotBlank() }
            if (parts.size < 2) return@mapNotNull null
            // 시도 / 시·군·구 / 읍·면·동 순서. 동 자리가 지번(숫자로 시작)이면 쓰지 않는다.
            val dong = parts.getOrNull(2)?.takeIf { it.isNotEmpty() && !it[0].isDigit() }.orEmpty()
            district(parts[0], parts[1], dong, center = point(doc.optString("x"), doc.optString("y")))
        }
        return found.distinctBy { it.fullName }
    }

    private fun district(sido: String, sigungu: String, dong: String, center: GeoPoint?): District? {
        val name = dong.ifBlank { sigungu }.ifBlank { sido }
        if (name.isBlank()) return null
        return District(name, listOf(sido, sigungu, dong).filter { it.isNotBlank() }.joinToString(" "), center)
    }

    // 카카오는 x가 경도, y가 위도이고 문자열로 준다. 숫자가 아니거나 한국 밖이면 좌표를 쓰지 않는다.
    private fun point(x: String, y: String): GeoPoint? {
        val lng = x.toDoubleOrNull() ?: return null
        val lat = y.toDoubleOrNull() ?: return null
        if (lat !in 33.0..39.0 || lng !in 124.0..132.0) return null
        return GeoPoint(latitude = lat, longitude = lng)
    }
}
