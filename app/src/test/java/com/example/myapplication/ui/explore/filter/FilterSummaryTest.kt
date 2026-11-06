package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilterSummaryTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    private fun millis(month: Int, day: Int, hour: Int = 0) =
        Calendar.getInstance(seoul).apply { clear(); set(2026, month - 1, day, hour, 0) }.timeInMillis

    @Test
    fun 날짜_선택지는_오늘부터_다섯_날이고_첫_칸만_오늘이다() {
        val options = dateOptions(millis(9, 8, 15), seoul)
        assertEquals(5, options.size)
        assertEquals(listOf("오늘", "수", "목", "금", "토"), options.map { it.weekdayLabel })
        assertEquals(listOf("9/8", "9/9", "9/10", "9/11", "9/12"), options.map { it.dateLabel })
        assertEquals("날짜는 그날 0시", millis(9, 8, 0), options.first().startOfDayMillis)
    }

    @Test
    fun 날짜_선택지는_달과_해가_바뀌어도_이어진다() {
        val options = dateOptions(millis(12, 30, 10), seoul)
        assertEquals(listOf("12/30", "12/31", "1/1", "1/2", "1/3"), options.map { it.dateLabel })
    }

    @Test
    fun 시간_범위_문구는_시작_시각_기준이라고_쓴다() {
        assertEquals("07:00 – 11:00 시작", timeRangeLabel(TimeOfDayRange(7 * 60, 11 * 60)))
        assertEquals("00:05 – 23:59 시작", timeRangeLabel(TimeOfDayRange(5, 23 * 60 + 59)))
    }

    @Test
    fun 종목을_고르지_않으면_기준_줄은_없다() {
        assertNull(criteriaSummary(MeetingFilter()))
    }

    @Test
    fun 헬스_부위를_고르지_않으면_관계없음이다() {
        assertEquals("관계없음", criteriaSummary(MeetingFilter(sport = Sport.HEALTH)))
    }

    @Test
    fun 헬스_기준_설정이_꺼져_있으면_부위만_쓰고_수준은_관계없음이다() {
        val filter = MeetingFilter(sport = Sport.HEALTH, health = HealthFilter(parts = setOf(BodyPart.ARM, BodyPart.CHEST)))
        assertEquals("가슴 · 팔 · 수준 관계없음", criteriaSummary(filter))
        val all = MeetingFilter(sport = Sport.HEALTH, health = HealthFilter(parts = BodyPart.entries.toSet()))
        assertEquals("디자인 순서: 가슴·등·팔·하체·어깨", "가슴 · 등 · 팔 · 하체 · 어깨 · 수준 관계없음", criteriaSummary(all))
    }

    @Test
    fun 헬스_기준_설정이_켜져_있으면_부위별_수준을_쓴다() {
        val filter = MeetingFilter(
            sport = Sport.HEALTH,
            health = HealthFilter(
                parts = setOf(BodyPart.CHEST, BodyPart.ARM, BodyPart.BACK), criteriaOn = true,
                ranges = mapOf(BodyPart.CHEST to 70..90, BodyPart.BACK to 0..2), armLevel = SkillLevel.INTERMEDIATE,
            ),
        )
        assertEquals("가슴 70–90kg · 등 0–2회 · 팔 중급", criteriaSummary(filter))
    }

    @Test
    fun 헬스_기준_설정이_켜져_있어도_수준을_정하지_않은_부위는_이름만_쓴다() {
        val filter = MeetingFilter(sport = Sport.HEALTH, health = HealthFilter(parts = setOf(BodyPart.LEGS), criteriaOn = true))
        assertEquals("하체", criteriaSummary(filter))
    }

    @Test
    fun 러닝_기준은_페이스_범위를_값_그대로_쓴다() {
        val on = MeetingFilter(sport = Sport.RUNNING, running = RunningFilter(criteriaOn = true, paceRangeSec = 330..390))
        assertEquals("5'30\"–6'30\"", criteriaSummary(on))
        assertEquals("관계없음", criteriaSummary(MeetingFilter(sport = Sport.RUNNING)))
    }

    @Test
    fun 등산_기준은_성향을_쓴다() {
        val on = MeetingFilter(sport = Sport.HIKING, hiking = HikingFilter(criteriaOn = true, style = HikingStyle.PRO))
        assertEquals("전문", criteriaSummary(on))
        assertEquals("관계없음", criteriaSummary(MeetingFilter(sport = Sport.HIKING)))
    }

    @Test
    fun 위치_줄은_고른_구_동이고_없으면_지역_전체다() {
        assertEquals("마포구", locationSummary(MeetingFilter(district = "마포구")))
        assertEquals("지역 전체", locationSummary(MeetingFilter()))
        assertEquals("지역 전체", locationSummary(MeetingFilter(district = "  ")))
    }

    @Test
    fun 상세_조건_줄은_성별과_매너온도다() {
        assertEquals("누구나 · 매너 관계없음", detailSummary(MeetingFilter()))
        assertEquals("누구나 · 매너 30°C+", detailSummary(MeetingFilter(minMannerTemperature = 30)))
        assertEquals("여성만 · 매너 70°C+", detailSummary(MeetingFilter(gender = GenderLimit.FEMALE_ONLY, minMannerTemperature = 70)))
        assertEquals("남성만 · 매너 관계없음", detailSummary(MeetingFilter(gender = GenderLimit.MALE_ONLY)))
    }

    @Test
    fun 검색창_제목은_날짜와_종목이다() {
        val now = millis(9, 8, 15)
        assertEquals("모든 운동", searchTitle(MeetingFilter(), now, seoul))
        assertEquals("헬스", searchTitle(MeetingFilter(sport = Sport.HEALTH), now, seoul))
        assertEquals("오늘 · 러닝", searchTitle(MeetingFilter(sport = Sport.RUNNING, dateMillis = millis(9, 8)), now, seoul))
        assertEquals("9/10 · 모든 운동", searchTitle(MeetingFilter(dateMillis = millis(9, 10)), now, seoul))
    }
}
