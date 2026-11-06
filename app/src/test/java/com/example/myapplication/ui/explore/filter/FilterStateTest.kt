package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.Sport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilterStateTest {
    private fun meeting(id: String, mannerTemperature: Float = 40f, status: MeetingStatus = MeetingStatus.RECRUITING) =
        Meeting(id = id, hostMannerTemperature = mannerTemperature, status = status)

    @Test
    fun 조건이_없으면_모집중인_모임을_모두_센다() {
        val state = FilterState(meetings = listOf(meeting("1"), meeting("2"), meeting("3", status = MeetingStatus.CLOSED)))
        assertEquals(2, state.resultCount)
    }

    @Test
    fun 찾기_개수는_지금_고르는_조건을_따라간다() {
        val meetings = listOf(meeting("1", 20f), meeting("2", 55f), meeting("3", 75f))
        assertEquals(3, FilterState(meetings = meetings).resultCount)
        assertEquals(2, FilterState(meetings = meetings, draft = MeetingFilter(minMannerTemperature = 50)).resultCount)
        assertEquals(1, FilterState(meetings = meetings, draft = MeetingFilter(minMannerTemperature = 70)).resultCount)
    }

    @Test
    fun 불러오기_전에는_0개다() {
        assertEquals(0, FilterState().resultCount)
    }

    @Test
    fun 시트를_적용_없이_닫으면_열기_전_조건으로_돌아간다() {
        val before = FilterState(draft = MeetingFilter(sport = Sport.HEALTH))
        val editing = before.withSheetOpened(FilterSheet.CRITERIA).copy(draft = MeetingFilter(sport = Sport.HEALTH, minMannerTemperature = 70))
        val dismissed = editing.withSheetDismissed()
        assertEquals(before.draft, dismissed.draft)
        assertNull(dismissed.openSheet)
        assertNull(dismissed.sheetBackup)
    }

    @Test
    fun 시트에서_적용하면_고친_조건이_남는다() {
        val editing = FilterState().withSheetOpened(FilterSheet.DETAIL).copy(draft = MeetingFilter(minMannerTemperature = 50))
        val applied = editing.withSheetApplied()
        assertEquals(50, applied.draft.minMannerTemperature)
        assertNull(applied.openSheet)
        assertNull(applied.sheetBackup)
    }

    @Test
    fun 열려_있지_않은_시트를_닫아도_조건은_그대로다() {
        val state = FilterState(draft = MeetingFilter(minMannerTemperature = 30))
        assertEquals(state, state.withSheetDismissed())
    }
}
