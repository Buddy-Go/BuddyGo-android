package com.example.myapplication.data.local

import com.example.myapplication.data.model.GeoPoint
import com.example.myapplication.data.repository.SignUpDraftStore
import com.example.myapplication.ui.explore.LastKnownLocation
import com.example.myapplication.ui.explore.filter.FilterStore
import com.example.myapplication.ui.explore.filter.MeetingFilter
import com.example.myapplication.ui.explore.filter.RecentDistrictStore
import com.example.myapplication.ui.meeting.create.PlaceResult
import com.example.myapplication.ui.meeting.create.PlaceSelection
import com.example.myapplication.ui.meeting.create.PlaceSelectionStore
import com.example.myapplication.ui.meeting.create.SamplePlaces
import com.example.myapplication.data.model.Sport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionResetTest {
    private var districtsBefore: List<String> = emptyList()
    private var placesBefore: List<PlaceResult> = emptyList()

    @After fun 정리() {
        RecentDistrictStore.attach(InMemoryKeyValueStore().apply { put("recent_districts", encodeStrings(districtsBefore)) })
        PlaceSelectionStore.attach(InMemoryKeyValueStore().apply { put("recent_places", com.example.myapplication.ui.meeting.create.encodePlaces(placesBefore)) })
        FilterStore.clear(); LastKnownLocation.clear(); SignUpDraftStore.clear()
    }

    private fun remember() {
        districtsBefore = RecentDistrictStore.recent.value
        placesBefore = PlaceSelectionStore.recent.value
    }

    @Test
    fun 로그아웃하면_조건_위치_가입_입력이_지워진다() {
        remember()
        FilterStore.apply(MeetingFilter(sport = Sport.HEALTH, district = "마포구"))
        LastKnownLocation.set(GeoPoint(37.5, 126.9))
        SignUpDraftStore.update { it.copy(email = "a@b.com") }
        SessionReset.onLogout()
        assertEquals(MeetingFilter(), FilterStore.applied.value)
        assertNull(LastKnownLocation.point.value)
        assertEquals("", SignUpDraftStore.draft.value.email)
    }

    @Test
    fun 로그아웃하면_최근_지역과_최근_장소가_비고_고르던_장소도_사라진다() {
        remember()
        PlaceSelectionStore.select(SamplePlaces.all[0])
        PlaceSelectionStore.confirm(PlaceSelection("이름", null, "주소", "지번", 37.5, 126.9))
        SessionReset.onLogout()
        assertTrue(RecentDistrictStore.recent.value.isEmpty())
        assertTrue(PlaceSelectionStore.recent.value.isEmpty())
        assertNull(PlaceSelectionStore.pending.value)
        assertNull(PlaceSelectionStore.confirmed.value)
    }

    @Test
    fun 저장소가_붙어_있으면_저장된_값도_지워져_다시_켜도_비어_있다() {
        remember()
        val disk = InMemoryKeyValueStore()
        RecentDistrictStore.attach(disk)
        PlaceSelectionStore.attach(disk)
        RecentDistrictStore.add("성수동")
        SessionReset.onLogout()
        RecentDistrictStore.attach(disk)
        PlaceSelectionStore.attach(disk)
        assertTrue("예시로 되돌아가지 않는다", RecentDistrictStore.recent.value.isEmpty())
        assertTrue(PlaceSelectionStore.recent.value.isEmpty())
    }
}
