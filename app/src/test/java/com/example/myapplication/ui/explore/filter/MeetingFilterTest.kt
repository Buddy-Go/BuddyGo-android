package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingFilterTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    private fun millis(month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        Calendar.getInstance(seoul).apply { clear(); set(2026, month - 1, day, hour, minute) }.timeInMillis

    private fun health(vararg parts: BodyPart, record: ExerciseRecord? = null) =
        Meeting(id = "h", criteria = MeetingCriteria.Health(parts.toSet(), record))

    private fun MeetingFilter.ok(m: Meeting) = matches(m, seoul)

    @Test
    fun 조건이_없으면_모든_모임이_맞다() {
        assertTrue(MeetingFilter().ok(health(BodyPart.CHEST)))
        assertTrue(MeetingFilter().ok(Meeting(criteria = MeetingCriteria.Hiking(HikingStyle.PRO))))
    }

    @Test
    fun 종목을_고르면_그_종목만_남는다() {
        val filter = MeetingFilter(sport = Sport.RUNNING)
        assertTrue(filter.ok(Meeting(criteria = MeetingCriteria.Running(5f, 360))))
        assertFalse(filter.ok(health(BodyPart.CHEST)))
    }

    @Test
    fun 종목을_바꾸면_이전_종목의_기준이_지워진다() {
        val before = MeetingFilter(sport = Sport.HEALTH, health = HealthFilter(parts = setOf(BodyPart.CHEST), criteriaOn = true))
        val after = before.withSport(Sport.RUNNING)
        assertEquals(HealthFilter(), after.health)
        assertEquals(Sport.RUNNING, after.sport)
        assertEquals("같은 종목이면 그대로", before, before.withSport(Sport.HEALTH))
    }

    // ---- 날짜·시간 ----

    @Test
    fun 날짜를_고르면_그날_모임만_남는다() {
        val filter = MeetingFilter(dateMillis = millis(9, 8))
        assertTrue(filter.ok(Meeting(startAtMillis = millis(9, 8, 23, 59))))
        assertFalse(filter.ok(Meeting(startAtMillis = millis(9, 9, 0, 0))))
    }

    @Test
    fun 시간대는_시작_시각_기준이고_끝_시각은_포함하지_않는다() {
        val evening = MeetingFilter(timeSlots = setOf(TimeSlot.EVENING))
        assertTrue(evening.ok(Meeting(startAtMillis = millis(9, 8, 18, 0))))
        assertFalse(evening.ok(Meeting(startAtMillis = millis(9, 8, 17, 59))))
        val morning = MeetingFilter(timeSlots = setOf(TimeSlot.MORNING))
        assertFalse("12:00은 12–18 시간대", morning.ok(Meeting(startAtMillis = millis(9, 8, 12, 0))))
    }

    @Test
    fun 시간대는_여러_개를_고를_수_있다() {
        val filter = MeetingFilter(timeSlots = setOf(TimeSlot.DAWN, TimeSlot.MORNING))
        assertTrue(filter.ok(Meeting(startAtMillis = millis(9, 8, 5, 0))))
        assertTrue(filter.ok(Meeting(startAtMillis = millis(9, 8, 11, 59))))
        assertFalse(filter.ok(Meeting(startAtMillis = millis(9, 8, 12, 0))))
    }

    @Test
    fun 직접_고른_시간_범위는_시작과_끝을_포함한다() {
        val filter = MeetingFilter(customRange = TimeOfDayRange(7 * 60, 11 * 60))
        assertTrue(filter.ok(Meeting(startAtMillis = millis(9, 8, 7, 0))))
        assertTrue(filter.ok(Meeting(startAtMillis = millis(9, 8, 11, 0))))
        assertFalse(filter.ok(Meeting(startAtMillis = millis(9, 8, 11, 1))))
        assertFalse(filter.ok(Meeting(startAtMillis = millis(9, 8, 6, 59))))
    }

    @Test
    fun 시간_범위를_직접_고르면_시간대_선택은_지워지고_반대도_그렇다() {
        val withSlots = MeetingFilter().withTimeSlots(setOf(TimeSlot.EVENING))
        val withRange = withSlots.withCustomRange(TimeOfDayRange(420, 660))
        assertTrue(withRange.timeSlots.isEmpty())
        assertTrue(withRange.hasTimeCondition)
        val backToSlots = withRange.withTimeSlots(setOf(TimeSlot.DAWN))
        assertEquals(null, backToSlots.customRange)
        assertFalse(MeetingFilter().hasTimeCondition)
    }

    @Test
    fun 시간_범위의_시작이_끝보다_늦으면_만들_수_없다() {
        val result = runCatching { TimeOfDayRange(600, 500) }
        assertTrue(result.isFailure)
    }

    // ---- 헬스 기준 ----

    @Test
    fun 고른_부위를_모두_포함하는_모임만_남는다() {
        val filter = MeetingFilter(sport = Sport.HEALTH, health = HealthFilter(parts = setOf(BodyPart.CHEST, BodyPart.ARM)))
        assertTrue("다른 부위가 더 있어도 된다", filter.ok(health(BodyPart.CHEST, BodyPart.BACK, BodyPart.ARM)))
        assertFalse(filter.ok(health(BodyPart.CHEST)))
    }

    @Test
    fun 기준_설정을_끄면_수준은_보지_않는다() {
        val filter = MeetingFilter(
            sport = Sport.HEALTH,
            health = HealthFilter(parts = setOf(BodyPart.CHEST), criteriaOn = false, ranges = mapOf(BodyPart.CHEST to 70..90)),
        )
        assertTrue(filter.ok(health(BodyPart.CHEST, record = ExerciseRecord(benchPressKg = 200))))
    }

    @Test
    fun 기준_설정을_켜면_범위_안의_호스트_기록만_남는다() {
        val filter = MeetingFilter(
            sport = Sport.HEALTH,
            health = HealthFilter(parts = setOf(BodyPart.CHEST), criteriaOn = true, ranges = mapOf(BodyPart.CHEST to 70..90)),
        )
        assertTrue(filter.ok(health(BodyPart.CHEST, record = ExerciseRecord(benchPressKg = 80))))
        assertTrue("경계값 포함", filter.ok(health(BodyPart.CHEST, record = ExerciseRecord(benchPressKg = 90))))
        assertFalse(filter.ok(health(BodyPart.CHEST, record = ExerciseRecord(benchPressKg = 91))))
    }

    @Test
    fun 고른_부위_중_하나라도_범위_안이면_포함한다() {
        val filter = MeetingFilter(
            sport = Sport.HEALTH,
            health = HealthFilter(
                parts = setOf(BodyPart.CHEST, BodyPart.ARM), criteriaOn = true,
                ranges = mapOf(BodyPart.CHEST to 70..90), armLevel = SkillLevel.INTERMEDIATE,
            ),
        )
        val onlyArmMatches = ExerciseRecord(benchPressKg = 150, armLevel = SkillLevel.INTERMEDIATE)
        assertTrue("일부만 맞아요 모임도 나온다", filter.ok(health(BodyPart.CHEST, BodyPart.ARM, record = onlyArmMatches)))
        val noneMatches = ExerciseRecord(benchPressKg = 150, armLevel = SkillLevel.ADVANCED)
        assertFalse(filter.ok(health(BodyPart.CHEST, BodyPart.ARM, record = noneMatches)))
    }

    @Test
    fun 호스트가_기록을_숨기면_범위_안으로_치지_않는다() {
        val filter = MeetingFilter(
            sport = Sport.HEALTH,
            health = HealthFilter(parts = setOf(BodyPart.CHEST), criteriaOn = true, ranges = mapOf(BodyPart.CHEST to 0..999)),
        )
        assertFalse(filter.ok(health(BodyPart.CHEST, record = null)))
    }

    @Test
    fun 호스트_기록에_그_부위_값이_없으면_범위_안이_아니다() {
        val filter = MeetingFilter(
            sport = Sport.HEALTH,
            health = HealthFilter(parts = setOf(BodyPart.CHEST), criteriaOn = true, ranges = mapOf(BodyPart.CHEST to 0..999)),
        )
        assertFalse(filter.ok(health(BodyPart.CHEST, record = ExerciseRecord(squatKg = 100))))
    }

    @Test
    fun 범위를_정한_부위가_하나도_없으면_수준은_보지_않는다() {
        val filter = MeetingFilter(
            sport = Sport.HEALTH,
            health = HealthFilter(parts = setOf(BodyPart.CHEST), criteriaOn = true),
        )
        assertTrue(filter.ok(health(BodyPart.CHEST, record = null)))
    }

    @Test
    fun 턱걸이는_횟수_범위로_비교한다() {
        val filter = MeetingFilter(
            sport = Sport.HEALTH,
            health = HealthFilter(parts = setOf(BodyPart.BACK), criteriaOn = true, ranges = mapOf(BodyPart.BACK to 0..2)),
        )
        assertTrue("0회도 범위 안", filter.ok(health(BodyPart.BACK, record = ExerciseRecord(pullUpCount = 0))))
        assertFalse(filter.ok(health(BodyPart.BACK, record = ExerciseRecord(pullUpCount = 10))))
    }

    @Test
    fun 종목을_고르지_않으면_헬스_기준은_보지_않는다() {
        val filter = MeetingFilter(health = HealthFilter(parts = setOf(BodyPart.LEGS)))
        assertTrue(filter.ok(health(BodyPart.CHEST)))
    }

    // ---- 러닝·등산 기준 ----

    @Test
    fun 러닝_기준을_켜면_페이스_범위_안의_모임만_남는다() {
        val filter = MeetingFilter(sport = Sport.RUNNING, running = RunningFilter(criteriaOn = true, paceRangeSec = 330..390))
        assertTrue(filter.ok(Meeting(criteria = MeetingCriteria.Running(5f, 360))))
        assertFalse(filter.ok(Meeting(criteria = MeetingCriteria.Running(5f, 420))))
    }

    @Test
    fun 러닝_기준을_끄면_페이스는_보지_않는다() {
        val filter = MeetingFilter(sport = Sport.RUNNING, running = RunningFilter(criteriaOn = false, paceRangeSec = 330..390))
        assertTrue(filter.ok(Meeting(criteria = MeetingCriteria.Running(5f, 600))))
    }

    @Test
    fun 등산_기준을_켜면_성향이_같은_모임만_남는다() {
        val filter = MeetingFilter(sport = Sport.HIKING, hiking = HikingFilter(criteriaOn = true, style = HikingStyle.HOBBY))
        assertTrue(filter.ok(Meeting(criteria = MeetingCriteria.Hiking(HikingStyle.HOBBY))))
        assertFalse(filter.ok(Meeting(criteria = MeetingCriteria.Hiking(HikingStyle.PRO))))
    }

    // ---- 위치·상세 조건 ----

    @Test
    fun 구_또는_동으로_거른다() {
        val place = MeetingPlace(address = "서울 마포구 양화로 100", lotAddress = "서교동 123-4")
        assertTrue(MeetingFilter(district = "마포구").ok(Meeting(place = place)))
        assertTrue(MeetingFilter(district = "서교동").ok(Meeting(place = place)))
        assertFalse(MeetingFilter(district = "강북구").ok(Meeting(place = place)))
        assertTrue("비어 있으면 위치 무관", MeetingFilter(district = " ").ok(Meeting(place = place)))
    }

    @Test
    fun 성별은_모임의_성별_조건과_같은_것만_남긴다() {
        val filter = MeetingFilter(gender = GenderLimit.FEMALE_ONLY)
        assertTrue(filter.ok(Meeting(genderLimit = GenderLimit.FEMALE_ONLY)))
        assertFalse(filter.ok(Meeting(genderLimit = GenderLimit.ANYONE)))
        assertTrue("성별을 정하지 않으면 전부", MeetingFilter().ok(Meeting(genderLimit = GenderLimit.MALE_ONLY)))
    }

    @Test
    fun 매너온도는_호스트_기준_하한이고_경계값을_포함한다() {
        val filter = MeetingFilter(minMannerTemperature = 50)
        assertTrue(filter.ok(Meeting(hostMannerTemperature = 50f)))
        assertFalse(filter.ok(Meeting(hostMannerTemperature = 49.9f)))
    }

    @Test
    fun 조건은_모두_함께_적용된다() {
        val filter = MeetingFilter(sport = Sport.HEALTH, minMannerTemperature = 30, timeSlots = setOf(TimeSlot.EVENING))
        val ok = health(BodyPart.CHEST).copy(hostMannerTemperature = 40f, startAtMillis = millis(9, 8, 19))
        assertTrue(filter.ok(ok))
        assertFalse(filter.ok(ok.copy(hostMannerTemperature = 20f)))
        assertFalse(filter.ok(ok.copy(startAtMillis = millis(9, 8, 9))))
    }

    @Test
    fun 시작을_끝보다_늦게_바꾸면_끝도_같이_옮겨진다() {
        val range = TimeOfDayRange(7 * 60, 11 * 60)
        assertEquals(TimeOfDayRange(8 * 60, 11 * 60), range.withStart(8 * 60))
        assertEquals(TimeOfDayRange(13 * 60, 13 * 60), range.withStart(13 * 60))
    }

    @Test
    fun 끝을_시작보다_이르게_바꾸면_시작도_같이_옮겨진다() {
        val range = TimeOfDayRange(7 * 60, 11 * 60)
        assertEquals(TimeOfDayRange(7 * 60, 9 * 60), range.withEnd(9 * 60))
        assertEquals(TimeOfDayRange(5 * 60, 5 * 60), range.withEnd(5 * 60))
    }

    @Test
    fun 시간_범위는_하루_안으로_제한된다() {
        val range = TimeOfDayRange(0, 60)
        assertEquals(TimeOfDayRange.MAX_MINUTE, range.withEnd(5000).endMinute)
        assertEquals(0, range.withStart(-30).startMinute)
    }

    @Test
    fun 전체_이름으로_저장된_조건은_구와_동이_모두_맞아야_한다() {
        val 달서구 = MeetingPlace(address = "대구 달서구 도원로 1", lotAddress = "대구 달서구 도원동 12-3")
        val 용산구 = MeetingPlace(address = "서울 용산구 한강로 1", lotAddress = "서울 용산구 도원동 5")
        val 제물포구 = MeetingPlace(address = "인천 제물포구 도원로 2", lotAddress = "인천 제물포구 도원동 9")
        val filter = MeetingFilter(district = "대구 달서구 도원동")
        assertTrue(filter.ok(Meeting(place = 달서구)))
        assertFalse("이름만 같은 서울 용산구의 도원동은 아니다", filter.ok(Meeting(place = 용산구)))
        assertFalse("이름만 같은 인천 제물포구의 도원동은 아니다", filter.ok(Meeting(place = 제물포구)))
    }

    @Test
    fun 시도_표기가_서버_주소와_달라도_구와_동이_맞으면_통과한다() {
        val place = MeetingPlace(address = "대구광역시 달서구 도원로 1", lotAddress = "대구광역시 달서구 도원동 12-3")
        assertTrue(MeetingFilter(district = "대구 달서구 도원동").ok(Meeting(place = place)))
        assertTrue(MeetingFilter(district = "대구 달서구").ok(Meeting(place = place)))
        assertFalse(MeetingFilter(district = "대구 중구 도원동").ok(Meeting(place = place)))
    }

    @Test
    fun 시도만_저장된_조건은_시도_이름으로_거른다() {
        assertTrue(MeetingFilter(district = "서울").ok(Meeting(place = MeetingPlace(address = "서울특별시 마포구 양화로 1"))))
        assertFalse(MeetingFilter(district = "서울").ok(Meeting(place = MeetingPlace(address = "대구 달서구 도원로 1"))))
    }

    @Test
    fun 지역_이름은_시도를_뺀_조각으로_나눈다() {
        assertEquals(listOf("달서구", "도원동"), districtParts("대구 달서구 도원동"))
        assertEquals(listOf("수원시", "영통구", "영통동"), districtParts("경기 수원시 영통구 영통동"))
        assertEquals(listOf("마포구"), districtParts("서울 마포구"))
        assertEquals(listOf("마포구"), districtParts("마포구"))
        assertEquals(listOf("서울"), districtParts("서울"))
        assertEquals(emptyList<String>(), districtParts("  "))
    }
}
