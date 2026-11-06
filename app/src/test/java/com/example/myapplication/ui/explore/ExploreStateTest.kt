package com.example.myapplication.ui.explore

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.ui.common.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreStateTest {
    private val lotA = MeetingPlace(name = "A짐", lotAddress = "서교동 1-1", latitude = 37.1, longitude = 127.1)
    private val lotB = MeetingPlace(name = "B공원", lotAddress = "망원동 2-2", latitude = 37.2, longitude = 127.2)

    private fun meeting(
        id: String,
        place: MeetingPlace = lotA,
        status: MeetingStatus = MeetingStatus.RECRUITING,
        distance: Int? = 100,
        favorite: Boolean = false,
    ) = Meeting(id = id, place = place, status = status, distanceMeters = distance, isFavorite = favorite)

    private fun stateOf(vararg meetings: Meeting, selected: String? = null, favoriteOnly: Boolean = false) =
        ExploreState(
            meetings = UiState.Success(meetings.toList()),
            selectedPinKey = selected,
            favoriteOnly = favoriteOnly,
        )

    @Test
    fun 지도에는_모집중_모임만_나온다() {
        val state = stateOf(
            meeting("1"),
            meeting("2", status = MeetingStatus.CLOSED),
            meeting("3", status = MeetingStatus.IN_PROGRESS),
            meeting("4", status = MeetingStatus.ENDED),
            meeting("5", status = MeetingStatus.CANCELED),
        )
        assertEquals(listOf("1"), state.visibleMeetings.map { it.id })
    }

    @Test
    fun 같은_지번의_모임은_핀_하나로_묶인다() {
        val pins = stateOf(meeting("1"), meeting("2"), meeting("3", place = lotB)).pins
        assertEquals(2, pins.size)
        val a = pins.first { it.key == "서교동 1-1" }
        assertEquals(listOf("1", "2"), a.meetings.map { it.id }.sorted())
        assertEquals(2, a.count)
        assertTrue(a.isMultiple)
    }

    @Test
    fun 지번이_다르면_이름이_같아도_따로_둔다() {
        val sameName = lotA.copy(lotAddress = "서교동 9-9")
        assertEquals(2, stateOf(meeting("1"), meeting("2", place = sameName)).pins.size)
    }

    @Test
    fun 지번이_비어_있으면_모임마다_핀이_따로다() {
        val noLot = lotA.copy(lotAddress = "")
        assertEquals(2, stateOf(meeting("1", place = noLot), meeting("2", place = noLot)).pins.size)
    }

    @Test
    fun 핀_위치는_묶음_안_가장_가까운_모임의_좌표다() {
        val pin = stateOf(meeting("far", distance = 900), meeting("near", distance = 50)).pins.single()
        assertEquals("near", pin.meetings.first().id)
        assertEquals(lotA.latitude, pin.latitude, 0.0)
    }

    @Test
    fun 기본_정렬은_거리순이고_거리를_모르면_맨_뒤다() {
        val state = stateOf(meeting("c", distance = null), meeting("b", distance = 500), meeting("a", distance = 100))
        assertEquals(listOf("a", "b", "c"), state.visibleMeetings.map { it.id })
    }

    @Test
    fun 찜_칩을_켜면_찜한_모임만_나온다() {
        val state = stateOf(meeting("1", favorite = true), meeting("2"), favoriteOnly = true)
        assertEquals(listOf("1"), state.visibleMeetings.map { it.id })
    }

    @Test
    fun 모임이_하나뿐인_핀을_고르면_떠_있는_카드가_보인다() {
        val state = stateOf(meeting("1"), meeting("2", place = lotB), selected = "망원동 2-2")
        assertEquals("2", state.floatingCard?.id)
        assertEquals("모임이 하나뿐이면 시트 목록은 그대로", 2, state.sheetMeetings.size)
        assertNull(state.sheetTitlePlace)
    }

    @Test
    fun 모임이_여러_개인_핀을_고르면_시트에_그_지번의_모임만_나온다() {
        val state = stateOf(meeting("1"), meeting("2"), meeting("3", place = lotB), selected = "서교동 1-1")
        assertNull(state.floatingCard)
        assertEquals(listOf("1", "2"), state.sheetMeetings.map { it.id }.sorted())
        assertEquals("A짐", state.sheetTitlePlace)
    }

    @Test
    fun 선택한_핀이_목록에서_사라지면_선택하지_않은_것으로_본다() {
        val state = stateOf(meeting("1", favorite = false), selected = "서교동 1-1", favoriteOnly = true)
        assertNull(state.selectedPin)
        assertNull(state.floatingCard)
    }

    @Test
    fun 불러오는_중이거나_실패하면_보이는_모임이_없다() {
        assertTrue(ExploreState(meetings = UiState.Loading).visibleMeetings.isEmpty())
        assertTrue(ExploreState(meetings = UiState.Error()).pins.isEmpty())
    }

    @Test
    fun 목데이터에서_같은_짐의_모집중_모임이_핀_하나로_묶인다() {
        val state = ExploreState(meetings = UiState.Success(MockMeetings.all))
        assertTrue(state.visibleMeetings.all { it.status == MeetingStatus.RECRUITING })
        assertTrue(state.pins.any { it.isMultiple })
        assertFalse(state.pins.any { it.meetings.any { m -> m.status != MeetingStatus.RECRUITING } })
    }
}
