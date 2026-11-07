package com.example.myapplication.data.local

import com.example.myapplication.ui.explore.filter.RecentDistrictStore
import com.example.myapplication.ui.meeting.create.PlaceResult
import com.example.myapplication.ui.meeting.create.PlaceSelection
import com.example.myapplication.ui.meeting.create.PlaceSelectionStore
import com.example.myapplication.ui.meeting.create.SamplePlaces
import com.example.myapplication.ui.meeting.create.decodePlaces
import com.example.myapplication.ui.meeting.create.encodePlaces
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 최근 검색 지역·최근 장소가 앱을 껐다 켜도 남는지. 저장소를 새로 붙이는 것이 '앱을 다시 켠 것'이다.
class PersistRecentsTest {
    private var districtsBefore: List<String> = emptyList()
    private var placesBefore: List<PlaceResult> = emptyList()

    @Before fun 준비() {
        districtsBefore = RecentDistrictStore.recent.value
        placesBefore = PlaceSelectionStore.recent.value
    }

    @After fun 정리() {
        // 다른 테스트가 기본 상태를 가정하므로 저장소를 떼고 값을 되돌린다.
        RecentDistrictStore.attach(InMemoryKeyValueStore().apply { put("recent_districts", encodeStrings(districtsBefore)) })
        PlaceSelectionStore.attach(InMemoryKeyValueStore().apply { put("recent_places", encodePlaces(placesBefore)) })
    }

    // ---- 글자 변환 ----

    @Test
    fun 글자_목록은_저장했다가_그대로_읽힌다() {
        val list = listOf("마포구", "성수동", "서울 강남구")
        assertEquals(list, decodeStrings(encodeStrings(list)))
    }

    @Test
    fun 줄바꿈과_탭_빈_값은_정리하고_저장된_값이_없으면_빈_목록이다() {
        assertEquals(listOf("가 나", "다"), decodeStrings(encodeStrings(listOf("가\n나", "  ", "다"))))
        assertTrue(decodeStrings(null).isEmpty())
        assertTrue(decodeStrings("").isEmpty())
    }

    @Test
    fun 장소는_이름_주소_지번_좌표까지_그대로_읽힌다() {
        val places = SamplePlaces.all.take(3)
        val back = decodePlaces(encodePlaces(places))
        assertEquals(places.map { it.copy(distanceMeters = null) }, back)
    }

    @Test
    fun 거리는_저장하지_않는다() {
        val back = decodePlaces(encodePlaces(listOf(SamplePlaces.all[0].copy(distanceMeters = 1200))))
        assertEquals(null, back.single().distanceMeters)
    }

    @Test
    fun 깨진_줄은_건너뛰고_나머지는_살린다() {
        val text = "이름만\n" +
            "스포애니\t서울\t지번\t숫자아님\t126.9\n" +
            "\t주소\t지번\t37.5\t126.9\n" +
            "여의도 한강공원\t서울 영등포구 여의동로 330\t서울 영등포구 여의도동 2\t37.5284\t126.9326"
        val back = decodePlaces(text)
        assertEquals(listOf("여의도 한강공원"), back.map { it.name })
        assertTrue(decodePlaces(null).isEmpty())
    }

    // ---- 최근 검색 지역 ----

    @Test
    fun 최근_지역은_다시_켜도_남는다() {
        val disk = InMemoryKeyValueStore()
        RecentDistrictStore.attach(disk)
        RecentDistrictStore.clear()
        RecentDistrictStore.add("성수동")
        RecentDistrictStore.add("마포구")
        // 앱을 껐다 켬: 같은 저장소를 다시 붙인다
        RecentDistrictStore.attach(disk)
        assertEquals(listOf("마포구", "성수동"), RecentDistrictStore.recent.value)
    }

    @Test
    fun 지운_최근_지역은_다시_켜도_지워져_있고_전부_지워도_예시로_돌아오지_않는다() {
        val disk = InMemoryKeyValueStore()
        RecentDistrictStore.attach(disk)
        RecentDistrictStore.clear()
        RecentDistrictStore.add("성수동")
        RecentDistrictStore.remove("성수동")
        RecentDistrictStore.attach(disk)
        assertTrue("전부 지웠으면 빈 목록이 저장된 값이다", RecentDistrictStore.recent.value.isEmpty())
    }

    @Test
    fun 저장된_값이_없는_첫_실행은_예시로_시작한다() {
        RecentDistrictStore.attach(InMemoryKeyValueStore())
        assertEquals(listOf("마포구", "강북구", "종로구", "영등포구"), RecentDistrictStore.recent.value)
    }

    // ---- 최근 장소 ----

    @Test
    fun 설정한_장소는_다시_켜도_최근_장소_맨_앞에_있다() {
        val disk = InMemoryKeyValueStore()
        PlaceSelectionStore.attach(disk)
        val p = SamplePlaces.all[3]
        PlaceSelectionStore.confirm(PlaceSelection(p.name, "3층", p.address, p.lotAddress, p.latitude, p.longitude))
        PlaceSelectionStore.consumeConfirmed()
        PlaceSelectionStore.attach(disk)
        val first = PlaceSelectionStore.recent.value.first()
        assertEquals(p.name, first.name)
        assertEquals(p.latitude, first.latitude, 0.0)
        assertEquals(p.lotAddress, first.lotAddress)
    }

    @Test
    fun 지운_최근_장소는_다시_켜도_지워져_있다() {
        val disk = InMemoryKeyValueStore()
        PlaceSelectionStore.attach(disk)
        val target = PlaceSelectionStore.recent.value.first()
        PlaceSelectionStore.removeRecent(target)
        PlaceSelectionStore.attach(disk)
        assertTrue(PlaceSelectionStore.recent.value.none { it.name == target.name && it.address == target.address })
    }

    @Test
    fun 저장소를_안_붙이면_메모리에서만_동작한다() {
        // attach 전 상태와 같은 동작(기존 테스트가 기대하는 모양)이 깨지지 않는지: 추가·삭제가 예외 없이 된다.
        RecentDistrictStore.add("테스트동")
        RecentDistrictStore.remove("테스트동")
        assertTrue("테스트동" !in RecentDistrictStore.recent.value)
    }
}
