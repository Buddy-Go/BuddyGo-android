package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun 주소_검색_결과의_x_y를_지역_중심_좌표로_읽는다() {
        // 카카오는 x가 경도, y가 위도이고 둘 다 문자열이다.
        val json = """{"documents":[
            {"address_name":"대구 수성구","address_type":"REGION","x":"128.630635463648","y":"35.8581745018635",
             "address":{"region_1depth_name":"대구","region_2depth_name":"수성구","region_3depth_name":"","x":"128.630635463648","y":"35.8581745018635"}}
        ]}"""
        assertEquals(
            listOf(District("수성구", "대구 수성구", GeoPoint(latitude = 35.8581745018635, longitude = 128.630635463648))),
            KakaoLocalParser.parseAddress(json),
        )
    }

    @Test
    fun 바깥_x_y가_없으면_address_안의_좌표를_쓴다() {
        val json = """{"documents":[
            {"address":{"region_1depth_name":"서울","region_2depth_name":"마포구","region_3depth_name":"연남동","x":"126.922023","y":"37.564477"}}
        ]}"""
        assertEquals(GeoPoint(37.564477, 126.922023), KakaoLocalParser.parseAddress(json).single().center)
    }

    @Test
    fun 키워드_검색은_장소_좌표를_넣는다() {
        val json = """{"documents":[
            {"address_name":"대구 수성구 두산동 539","place_name":"수성못","x":"128.617276","y":"35.827701"}
        ]}"""
        assertEquals(GeoPoint(35.827701, 128.617276), KakaoLocalParser.parseKeyword(json).single().center)
    }

    @Test
    fun 숫자가_아니거나_한국_밖이거나_없는_좌표는_쓰지_않는다() {
        val json = """{"documents":[
            {"address_name":"a","x":"abc","y":"35.0","address":{"region_1depth_name":"대구","region_2depth_name":"수성구","region_3depth_name":""}},
            {"address_name":"b","x":"0","y":"0","address":{"region_1depth_name":"부산","region_2depth_name":"해운대구","region_3depth_name":""}},
            {"address_name":"c","address":{"region_1depth_name":"서울","region_2depth_name":"중구","region_3depth_name":""}}
        ]}"""
        val centers = KakaoLocalParser.parseAddress(json).map { it.center }
        assertEquals(listOf(null, null, null), centers)
        assertNull(District("수성구", "대구 수성구").center)
    }
}
