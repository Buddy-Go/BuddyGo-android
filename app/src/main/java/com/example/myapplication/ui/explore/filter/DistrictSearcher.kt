package com.example.myapplication.ui.explore.filter

import com.example.myapplication.BuildConfig
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
        return KakaoLocalParser.parseKeyword(get("keyword", query))
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

// 카카오 로컬 API 응답에서 지역만 뽑는다. 이름(name)은 가장 작은 단위(동, 없으면 구·시), 전체 이름은 "시도 구 동".
object KakaoLocalParser {
    fun parseAddress(json: String): List<District> {
        val docs = JSONObject(json).optJSONArray("documents") ?: return emptyList()
        val found = (0 until docs.length()).mapNotNull { i ->
            val a = docs.getJSONObject(i).optJSONObject("address") ?: return@mapNotNull null
            district(a.optString("region_1depth_name"), a.optString("region_2depth_name"), a.optString("region_3depth_name"))
        }
        return found.distinctBy { it.fullName }
    }

    // 키워드 검색의 address_name은 "서울 마포구 합정동 123-4"처럼 온다. 앞의 시도·구·동 조각만 쓴다.
    fun parseKeyword(json: String): List<District> {
        val docs = JSONObject(json).optJSONArray("documents") ?: return emptyList()
        val found = (0 until docs.length()).mapNotNull { i ->
            val parts = docs.getJSONObject(i).optString("address_name").split(" ").filter { it.isNotBlank() }
            if (parts.size < 2) return@mapNotNull null
            // 시도 / 시·군·구 / 읍·면·동 순서. 동 자리가 지번(숫자로 시작)이면 쓰지 않는다.
            val dong = parts.getOrNull(2)?.takeIf { it.isNotEmpty() && !it[0].isDigit() }.orEmpty()
            district(parts[0], parts[1], dong)
        }
        return found.distinctBy { it.fullName }
    }

    private fun district(sido: String, sigungu: String, dong: String): District? {
        val name = dong.ifBlank { sigungu }.ifBlank { sido }
        if (name.isBlank()) return null
        return District(name, listOf(sido, sigungu, dong).filter { it.isNotBlank() }.joinToString(" "))
    }
}
