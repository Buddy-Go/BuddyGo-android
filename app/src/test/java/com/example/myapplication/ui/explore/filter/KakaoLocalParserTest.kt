package com.example.myapplication.ui.explore.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoLocalParserTest {
    @Test
    fun 주소_검색_결과에서_지역을_뽑고_같은_곳은_하나만_남긴다() {
        val json = """{"documents":[
            {"address_name":"경기 수원시 영통구 영통동","address":{"region_1depth_name":"경기","region_2depth_name":"수원시 영통구","region_3depth_name":"영통동"}},
            {"address_name":"경기 수원시 영통구 영통동 1-1","address":{"region_1depth_name":"경기","region_2depth_name":"수원시 영통구","region_3depth_name":"영통동"}},
            {"address_name":"부산 해운대구","address":{"region_1depth_name":"부산","region_2depth_name":"해운대구","region_3depth_name":""}}
        ]}"""
        assertEquals(
            listOf(District("영통동", "경기 수원시 영통구 영통동"), District("해운대구", "부산 해운대구")),
            KakaoLocalParser.parseAddress(json),
        )
    }

    @Test
    fun 키워드_검색_결과의_주소에서_지역을_뽑는다() {
        val json = """{"documents":[
            {"address_name":"서울 마포구 신공덕동 1"},
            {"address_name":"서울 마포구 합정동 411"},
            {"address_name":"서울 마포구 합정동 5"}
        ]}"""
        assertEquals(
            listOf(District("신공덕동", "서울 마포구 신공덕동"), District("합정동", "서울 마포구 합정동")),
            KakaoLocalParser.parseKeyword(json),
        )
    }

    @Test
    fun 결과가_없으면_빈_목록이다() {
        assertEquals(emptyList<District>(), KakaoLocalParser.parseAddress("""{"documents":[]}"""))
        assertEquals(emptyList<District>(), KakaoLocalParser.parseKeyword("""{"meta":{}}"""))
    }
}
