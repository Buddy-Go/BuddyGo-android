package com.example.myapplication.ui.meeting.create

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.SkillLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 화면에 보이는 글자가 디자인 프레임(10a, 10a-2)과 같은지 확인한다.
class MeetingCreateStateTest {

    private val health = HealthCriteria(
        parts = listOf(BodyPart.CHEST, BodyPart.ARM),
        showMyRecord = true,
        myRecord = ExerciseRecord(benchPressKg = 80, armLevel = SkillLevel.INTERMEDIATE),
    )

    @Test
    fun 헬스_기준_기록_표시_10a() {
        assertEquals("가슴 80kg · 팔 중급", health.summary)
    }

    @Test
    fun 헬스_기준_기록_숨김_10a_2() {
        assertEquals("가슴 · 팔", health.copy(showMyRecord = false).summary)
    }

    @Test
    fun 헬스_기록_없는_부위는_이름만() {
        val c = health.copy(myRecord = ExerciseRecord(benchPressKg = 80))
        assertEquals("가슴 80kg · 팔", c.summary)
    }

    @Test
    fun 시간_표시() {
        val s = MeetingCreateState(startMinutes = 19 * 60, durationMinutes = 90)
        assertEquals("오후 7:00 – 8:30 · 1시간 30분", s.timeSummary)
        assertEquals(
            "오전 7:00 – 10:00 · 3시간",
            s.copy(startMinutes = 7 * 60, durationMinutes = 180).timeSummary,
        )
        assertEquals(
            "오전 11:30 – 오후 12:00 · 30분",
            s.copy(startMinutes = 11 * 60 + 30, durationMinutes = 30).timeSummary,
        )
    }

    @Test
    fun 비어_있으면_null() {
        val s = MeetingCreateState()
        assertNull(s.timeSummary)
        assertNull(s.criteriaSummary)
        assertNull(s.placeSummary)
        assertNull(s.capacitySummary)
        assertNull(s.descriptionSummary)
    }

    @Test
    fun 정원_성별_장소() {
        val s = MeetingCreateState(capacity = 2, place = PlaceSelection("스포애니 합정점", "3층 프리웨이트 존"))
        assertEquals("2명 · 누구나", s.capacitySummary)
        assertEquals("스포애니 합정점 · 3층 프리웨이트 존", s.placeSummary)
        assertEquals("북한산 우이동 들머리", PlaceSelection("북한산 우이동 들머리").summary)
    }

    @Test
    fun 필수값이_다_있어야_개설_가능() {
        val full = MeetingCreateState(
            title = "제목",
            selectedDateIndex = 0,
            startMinutes = 600,
            durationMinutes = 60,
            healthCriteria = health,
            place = PlaceSelection("헬스장"),
            capacity = 2,
        )
        assertTrue(full.canSubmit)
        assertFalse(full.copy(title = " ").canSubmit)
        assertFalse(full.copy(place = null).canSubmit)
        assertFalse(full.copy(healthCriteria = HealthCriteria()).canSubmit)
    }

    @Test
    fun 시간_설정_시트_표기_10f() {
        assertEquals("19:00", clock24(19 * 60))
        assertEquals("20:30", clock24(19 * 60 + 90))
        assertEquals("00:30", clock24(24 * 60 + 30))
        assertEquals("1시간 30분", durationText(90))
    }

    @Test
    fun 헬스_부위는_칩_순서대로_표시() {
        val c = health.copy(parts = listOf(BodyPart.ARM, BodyPart.CHEST))
        assertEquals("가슴 80kg · 팔 중급", c.summary)
    }

    @Test
    fun 기록_항목_이름_10d() {
        assertEquals("벤치프레스 1RM", BodyPart.CHEST.recordLabel)
        assertEquals("숙련도", BodyPart.ARM.recordLabel)
        assertEquals("12회", ExerciseRecord(pullUpCount = 12).textFor(BodyPart.BACK))
        assertEquals("0회", ExerciseRecord(pullUpCount = 0).textFor(BodyPart.BACK)) // 0회도 기록 (service-rules 1장)
        assertEquals(null, ExerciseRecord().textFor(BodyPart.LEGS))
        assertFalse(HealthCriteria().hasAnyRecord)
    }
}
