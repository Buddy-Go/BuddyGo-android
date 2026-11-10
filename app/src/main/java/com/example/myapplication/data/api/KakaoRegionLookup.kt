package com.example.myapplication.data.api

import com.example.myapplication.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// 좌표 → 법정동 코드(10자리). 서버는 모임 장소에 법정동 코드를 요구하는데, 장소 검색 결과에는 없어서 개설할 때 카카오로 찾는다.
fun interface RegionLookup {
    suspend fun regionCode(latitude: Double, longitude: Double): String?
}

// 카카오 로컬 API '좌표로 행정구역 정보 받기'. 법정동(region_type = B)의 code를 쓴다. REST 키가 없거나 못 찾으면 null.
class KakaoRegionLookup(private val restApiKey: String = BuildConfig.KAKAO_REST_API_KEY) : RegionLookup {
    override suspend fun regionCode(latitude: Double, longitude: Double): String? {
        if (restApiKey.isBlank()) return null
        val url = URL("https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x=$longitude&y=$latitude")
        val json = withContext(Dispatchers.IO) {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.setRequestProperty("Authorization", "KakaoAK $restApiKey")
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                if (conn.responseCode !in 200..299) return@withContext null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        } ?: return null
        return parse(json)
    }

    companion object {
        fun parse(json: String): String? {
            val docs = JSONObject(json).optJSONArray("documents") ?: return null
            for (i in 0 until docs.length()) {
                val d = docs.getJSONObject(i)
                val code = d.optString("code")
                if (d.optString("region_type") == "B" && code.length == 10 && code.all { it.isDigit() }) return code
            }
            return null
        }
    }
}
