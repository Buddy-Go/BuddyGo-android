package com.example.myapplication.ui.meeting.create

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.MeetingCriteria
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 10a 개설 요청: 화면 상태 → 공용 Meeting
class MeetingCreateSubmitTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val filled = MeetingCreateState(
        title = " 퇴근 후 가슴 ",
        dateOptions = listOf(DateOption(2026, 9, 8, "오늘")),
        selectedDateIndex = 0,
        startMinutes = 19 * 60,
        durationMinutes = 90,
        healthCriteria = HealthCriteria(parts = listOf(BodyPart.CHEST), showMyRecord = true, myRecord = ExerciseRecord(benchPressKg = 80)),
        place = PlaceSelection("스포애니 합정점", "3층"),
        capacity = 2,
    )

    @Test
    fun emptyStateGivesNull() {
        assertNull(MeetingCreateState().toMeeting(utc))
    }

    @Test
    fun filledStateMapsToMeeting() {
        val m = filled.toMeeting(utc)!!
        assertEquals("퇴근 후 가슴", m.title)
        assertEquals(1788894000000L, m.startAtMillis) // 2026-09-08 19:00 UTC
        assertEquals(90, m.durationMinutes)
        assertEquals(2, m.capacity)
        assertEquals("3층", m.place.detailAddress)
        val c = m.criteria as MeetingCriteria.Health
        assertEquals(setOf(BodyPart.CHEST), c.parts)
        assertEquals(80, c.hostRecord?.benchPressKg)
    }

    @Test
    fun hiddenRecordIsNotSent() {
        val m = filled.copy(healthCriteria = filled.healthCriteria.copy(showMyRecord = false)).toMeeting(utc)!!
        assertNull((m.criteria as MeetingCriteria.Health).hostRecord)
    }
}
