package com.example.myapplication.ui.explore

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.ui.common.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {
    private val hongdae = GeoPoint(37.5563, 126.9236)

    @Test
    fun 같은_점의_거리는_0이다() {
        assertEquals(0, distanceMeters(hongdae, 37.5563, 126.9236))
    }

    @Test
    fun 홍대에서_망원한강공원까지는_대략_2킬로미터다() {
        // 목데이터의 두 장소(서교동 헬스장, 망원 한강공원)
        val meters = distanceMeters(hongdae, 37.5551, 126.8978)
        assertTrue("2.2km 안팎이어야 한다: $meters", meters in 2_000..2_500)
    }

    @Test
    fun 서울에서_부산까지는_삼백이십_킬로미터_안팎이다() {
        val seoul = GeoPoint(37.5665, 126.9780)
        val meters = distanceMeters(seoul, 35.1796, 129.0756)
        assertTrue("$meters", meters in 315_000..335_000)
    }

    @Test
    fun 방향이_반대여도_거리는_같다() {
        val a = distanceMeters(hongdae, 37.6586, 127.0114)
        val b = distanceMeters(GeoPoint(37.6586, 127.0114), 37.5563, 126.9236)
        assertTrue(kotlin.math.abs(a - b) <= 1)
    }

    private fun meeting(id: String, lat: Double, lng: Double, serverDistance: Int?) = Meeting(
        id = id, place = MeetingPlace(latitude = lat, longitude = lng), distanceMeters = serverDistance,
    )

    private fun stateOf(me: GeoPoint?, vararg meetings: Meeting) =
        ExploreState(meetings = UiState.Success(meetings.toList()), myLocation = me)

    @Test
    fun 내_위치를_모르면_가져온_거리를_그대로_쓴다() {
        val state = stateOf(null, meeting("1", 37.5551, 126.8978, 777))
        assertEquals(777, state.visibleMeetings.single().distanceMeters)
    }

    @Test
    fun 내_위치를_알면_거리를_내_위치에서_다시_잰다() {
        val state = stateOf(hongdae, meeting("1", 37.5551, 126.8978, 777))
        assertTrue(state.visibleMeetings.single().distanceMeters!! in 2_000..2_500)
    }

    @Test
    fun 내_위치를_알면_거리순도_내_위치_기준이다() {
        // 가져온 거리는 a가 더 가깝다고 했지만 내 위치에서는 b가 더 가깝다.
        val state = stateOf(hongdae, meeting("a", 37.6586, 127.0114, 100), meeting("b", 37.5551, 126.8978, 9_000))
        assertEquals(listOf("b", "a"), state.visibleMeetings.map { it.id })
    }

    @Test
    fun 모임_거리는_내_위치가_있을_때만_다시_잰다() {
        val m = meeting("1", 37.5551, 126.8978, 777)
        assertEquals(777, meetingDistanceMeters(m, null))
        assertTrue(meetingDistanceMeters(m, hongdae)!! in 2_000..2_500)
        assertEquals("좌표가 없으면 가져온 값", 777, meetingDistanceMeters(meeting("2", 0.0, 0.0, 777), hongdae))
        assertEquals(null, meetingDistanceMeters(meeting("3", 0.0, 0.0, null), hongdae))
    }

    @Test
    fun 마지막_내_위치를_저장하고_지울_수_있다() {
        LastKnownLocation.clear()
        assertEquals(null, LastKnownLocation.point.value)
        LastKnownLocation.set(hongdae)
        assertEquals(hongdae, LastKnownLocation.point.value)
        LastKnownLocation.clear()
    }

    @Test
    fun 좌표가_없는_모임은_가져온_거리를_그대로_둔다() {
        val state = stateOf(hongdae, meeting("1", 0.0, 0.0, 500))
        assertEquals(500, state.visibleMeetings.single().distanceMeters)
    }
}
