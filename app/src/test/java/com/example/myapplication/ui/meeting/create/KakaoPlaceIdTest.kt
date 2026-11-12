package com.example.myapplication.ui.meeting.create

import com.example.myapplication.ui.explore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 카카오 장소 id가 검색 → 선택 → 최근 장소 저장 → 모임 장소까지 끊기지 않고 이어지는지
class KakaoPlaceIdTest {
    private val json = """{"documents":[
        {"id":"26338954","place_name":"스포애니 합정점","address_name":"서울 마포구 서교동 464-1","road_address_name":"서울 마포구 양화로 45","x":"126.9139","y":"37.5496"},
        {"place_name":"id 없는 곳","address_name":"서울 어딘가","x":"126.9","y":"37.5"}]}"""

    @Test
    fun 카카오_검색_응답의_id를_읽는다() {
        val (a, b) = KakaoPlaceParser.parse(json, null)
        assertEquals("26338954", a.kakaoPlaceId)
        assertNull("id가 없으면 null", b.kakaoPlaceId)
    }

    @Test
    fun 장소_선택에서_모임_장소로_id가_이어진다() {
        val picked = KakaoPlaceParser.parse(json, null).first()
        val selection = PlaceSelection(picked.name, "3층", picked.address, picked.lotAddress, picked.latitude, picked.longitude, picked.kakaoPlaceId)
        assertEquals("26338954", selection.toMeetingPlace().kakaoPlaceId)
        assertEquals("26338954", selection.toResult().kakaoPlaceId)
    }

    @Test
    fun 집결지를_지도에서_옮기면_id를_보내지_않는다() {
        val picked = KakaoPlaceParser.parse(json, null).first()
        val moved = meetingPointSelection(picked, GeoPoint(37.6, 126.8), "")
        assertNull(moved.kakaoPlaceId)
    }

    @Test
    fun 최근_장소를_저장했다_읽어도_id가_남고_옛_형식도_읽힌다() {
        val places = KakaoPlaceParser.parse(json, null)
        val back = decodePlaces(encodePlaces(places))
        assertEquals(listOf("26338954", null), back.map { it.kakaoPlaceId })
        assertEquals(places.map { it.name }, back.map { it.name })

        val old = decodePlaces("옛 장소\t도로명\t지번\t37.5\t126.9") // id 칸이 생기기 전에 저장된 5칸
        assertEquals("옛 장소", old.single().name)
        assertNull(old.single().kakaoPlaceId)
    }
}
