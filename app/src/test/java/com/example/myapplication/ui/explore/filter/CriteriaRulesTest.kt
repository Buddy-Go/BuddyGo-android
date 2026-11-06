package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CriteriaRulesTest {
    private val record = ExerciseRecord(
        benchPressKg = 80, pullUpCount = 8, squatKg = 100, overheadPressKg = 50,
        armLevel = SkillLevel.INTERMEDIATE, runningPaceSec = 330, hikingStyle = HikingStyle.PRO,
    )

    @Test
    fun 부위를_켜고_끌_수_있다() {
        val on = HealthFilter().withPartToggled(BodyPart.CHEST).withPartToggled(BodyPart.ARM)
        assertEquals(setOf(BodyPart.CHEST, BodyPart.ARM), on.parts)
        assertEquals(setOf(BodyPart.ARM), on.withPartToggled(BodyPart.CHEST).parts)
    }

    @Test
    fun 부위를_끄면_그_부위의_수준도_지워진다() {
        val filter = HealthFilter(
            parts = setOf(BodyPart.CHEST, BodyPart.ARM), criteriaOn = true,
            ranges = mapOf(BodyPart.CHEST to 70..90), armLevel = SkillLevel.ADVANCED,
        )
        val chestOff = filter.withPartToggled(BodyPart.CHEST)
        assertTrue(chestOff.ranges.isEmpty())
        assertEquals(SkillLevel.ADVANCED, chestOff.armLevel)
        assertNull(filter.withPartToggled(BodyPart.ARM).armLevel)
    }

    @Test
    fun 부위가_하나도_없어지면_기준_설정도_꺼진다() {
        val filter = HealthFilter(parts = setOf(BodyPart.CHEST), criteriaOn = true, ranges = mapOf(BodyPart.CHEST to 70..90))
        val none = filter.withPartToggled(BodyPart.CHEST)
        assertFalse(none.criteriaOn)
        assertTrue(none.parts.isEmpty())
    }

    @Test
    fun 부위를_고르지_않으면_기준_설정을_켤_수_없다() {
        assertFalse(HealthFilter().withCriteriaOn(true, record).criteriaOn)
    }

    @Test
    fun 기준_설정을_처음_켜면_내_기록으로_초기값이_채워진다() {
        val on = HealthFilter(parts = setOf(BodyPart.CHEST, BodyPart.BACK, BodyPart.ARM)).withCriteriaOn(true, record)
        assertTrue(on.criteriaOn)
        assertEquals("중량은 ±10kg", 70..90, on.ranges[BodyPart.CHEST])
        assertEquals("턱걸이는 ±2회", 6..10, on.ranges[BodyPart.BACK])
        assertEquals("팔은 내 숙련도 그대로", SkillLevel.INTERMEDIATE, on.armLevel)
    }

    @Test
    fun 초기값은_범위_밖으로_나가지_않는다() {
        val low = ExerciseRecord(pullUpCount = 0, benchPressKg = 5)
        val on = HealthFilter(parts = setOf(BodyPart.BACK, BodyPart.CHEST)).withCriteriaOn(true, low)
        assertEquals(0..2, on.ranges[BodyPart.BACK])
        assertEquals(0..15, on.ranges[BodyPart.CHEST])
        val high = ExerciseRecord(benchPressKg = 195)
        assertEquals(185..200, HealthFilter(parts = setOf(BodyPart.CHEST)).withCriteriaOn(true, high).ranges[BodyPart.CHEST])
    }

    @Test
    fun 내_기록이_없는_부위는_범위가_비어_있다() {
        val on = HealthFilter(parts = setOf(BodyPart.CHEST, BodyPart.LEGS)).withCriteriaOn(true, ExerciseRecord(benchPressKg = 60))
        assertEquals(50..70, on.ranges[BodyPart.CHEST])
        assertNull(on.ranges[BodyPart.LEGS])
        assertTrue(HealthFilter(parts = setOf(BodyPart.CHEST)).withCriteriaOn(true, null).ranges.isEmpty())
    }

    @Test
    fun 기준_설정을_껐다_켜도_직접_바꾼_값은_유지된다() {
        val edited = HealthFilter(parts = setOf(BodyPart.CHEST)).withCriteriaOn(true, record).withRange(BodyPart.CHEST, 100..120)
        val again = edited.withCriteriaOn(false, record).withCriteriaOn(true, record)
        assertEquals(100..120, again.ranges[BodyPart.CHEST])
    }

    @Test
    fun 내_기준으로_설정은_직접_바꾼_값을_내_기록으로_되돌린다() {
        val edited = HealthFilter(parts = setOf(BodyPart.CHEST, BodyPart.ARM), criteriaOn = true)
            .withRange(BodyPart.CHEST, 100..120).withArmLevel(SkillLevel.ADVANCED)
        val reset = edited.withMyCriteria(record)
        assertEquals(70..90, reset.ranges[BodyPart.CHEST])
        assertEquals(SkillLevel.INTERMEDIATE, reset.armLevel)
    }

    @Test
    fun 러닝_기준을_처음_켜면_내_페이스_앞뒤_30초가_초기값이다() {
        val on = RunningFilter().withCriteriaOn(true, record)
        assertTrue(on.criteriaOn)
        assertEquals(300..360, on.paceRangeSec)
        assertNull(RunningFilter().withCriteriaOn(true, ExerciseRecord()).paceRangeSec)
        assertFalse(on.withCriteriaOn(false, record).criteriaOn)
    }

    @Test
    fun 러닝_페이스_초기값은_슬라이더_범위_안이다() {
        val fast = RunningFilter().withCriteriaOn(true, ExerciseRecord(runningPaceSec = 250))
        assertEquals(240..280, fast.paceRangeSec)
        val slow = RunningFilter().withCriteriaOn(true, ExerciseRecord(runningPaceSec = 470))
        assertEquals(440..480, slow.paceRangeSec)
    }

    @Test
    fun 등산_기준을_처음_켜면_내_성향이_초기값이고_직접_바꾼_값은_유지된다() {
        val on = HikingFilter().withCriteriaOn(true, record)
        assertEquals(HikingStyle.PRO, on.style)
        val changed = on.withStyle(HikingStyle.HOBBY)
        assertEquals(HikingStyle.HOBBY, changed.withCriteriaOn(false, record).withCriteriaOn(true, record).style)
    }

    @Test
    fun 슬라이더_최대값은_등이_횟수_나머지는_킬로그램이다() {
        assertEquals(30, sliderMax(BodyPart.BACK))
        assertEquals(200, sliderMax(BodyPart.CHEST))
    }
}
