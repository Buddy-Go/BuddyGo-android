package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CriteriaSheetRulesTest {
    private val host = ExerciseRecord(benchPressKg = 80, pullUpCount = 8, armLevel = SkillLevel.INTERMEDIATE)

    @Test
    fun 호스트_범위는_중량_앞뒤_10킬로_턱걸이_앞뒤_2회다() {
        assertEquals(70..90, hostRangeOf(BodyPart.CHEST, host))
        assertEquals(6..10, hostRangeOf(BodyPart.BACK, host))
        assertEquals(0..2, hostRangeOf(BodyPart.BACK, ExerciseRecord(pullUpCount = 0)))
    }

    @Test
    fun 호스트가_기록을_숨기면_범위가_없다() {
        assertNull(hostRangeOf(BodyPart.CHEST, null))
        assertNull(hostRangeOf(BodyPart.CHEST, ExerciseRecord(squatKg = 100)))
        assertNull("팔은 범위가 아니라 숙련도다", hostRangeOf(BodyPart.ARM, host))
    }

    @Test
    fun 내_기록이_범위_안_위_아래인지_가른다() {
        assertEquals(PartVerdict.INSIDE, verdictOf(BodyPart.CHEST, host, ExerciseRecord(benchPressKg = 90)))
        assertEquals(PartVerdict.INSIDE, verdictOf(BodyPart.CHEST, host, ExerciseRecord(benchPressKg = 70)))
        assertEquals(PartVerdict.ABOVE, verdictOf(BodyPart.CHEST, host, ExerciseRecord(benchPressKg = 95)))
        assertEquals(PartVerdict.BELOW, verdictOf(BodyPart.CHEST, host, ExerciseRecord(benchPressKg = 60)))
    }

    @Test
    fun 비교할_수_없는_경우는_따로_구분한다() {
        assertEquals(PartVerdict.NO_MY_RECORD, verdictOf(BodyPart.CHEST, host, ExerciseRecord()))
        assertEquals(PartVerdict.NO_MY_RECORD, verdictOf(BodyPart.CHEST, host, null))
        assertEquals(PartVerdict.HOST_HIDDEN, verdictOf(BodyPart.CHEST, null, ExerciseRecord(benchPressKg = 80)))
    }

    @Test
    fun 부위_안내_문구() {
        assertEquals("내 기록 95kg · 범위보다 높아요", partNote(BodyPart.CHEST, host, ExerciseRecord(benchPressKg = 95)))
        assertEquals("내 기록 60kg · 범위보다 낮아요", partNote(BodyPart.CHEST, host, ExerciseRecord(benchPressKg = 60)))
        assertEquals("내 기록 80kg · 범위 안이에요", partNote(BodyPart.CHEST, host, ExerciseRecord(benchPressKg = 80)))
        assertEquals("내 기록 7회 · 범위 안이에요", partNote(BodyPart.BACK, host, ExerciseRecord(pullUpCount = 7)))
        assertEquals("내 기록을 아직 안 적었어요", partNote(BodyPart.CHEST, host, ExerciseRecord()))
        assertEquals("호스트가 기록을 공개하지 않았어요", partNote(BodyPart.CHEST, null, ExerciseRecord(benchPressKg = 80)))
    }

    @Test
    fun 팔_숙련도는_같을_때만_충족이다() {
        assertEquals(true, armMatches(host, ExerciseRecord(armLevel = SkillLevel.INTERMEDIATE)))
        assertEquals(false, armMatches(host, ExerciseRecord(armLevel = SkillLevel.ADVANCED)))
        assertNull(armMatches(host, ExerciseRecord()))
        assertNull(armMatches(null, ExerciseRecord(armLevel = SkillLevel.ADVANCED)))
    }

    @Test
    fun 팔_안내_문구() {
        assertEquals("내 숙련도는 중급이에요 · 기준 충족", armNote(host, ExerciseRecord(armLevel = SkillLevel.INTERMEDIATE)))
        assertEquals("내 숙련도는 고급이에요 · 기준과 달라요", armNote(host, ExerciseRecord(armLevel = SkillLevel.ADVANCED)))
        assertEquals("내 숙련도는 초급이에요", armNote(null, ExerciseRecord(armLevel = SkillLevel.BEGINNER)))
        assertEquals("내 기록을 아직 안 적었어요", armNote(host, ExerciseRecord()))
    }

    @Test
    fun 등산_안내_문구() {
        assertEquals("내 등산 성향은 취미예요 · 이 모임과 맞아요", hikingNote(HikingStyle.HOBBY, ExerciseRecord(hikingStyle = HikingStyle.HOBBY)))
        assertEquals("내 등산 성향은 전문이에요 · 이 모임과 달라요", hikingNote(HikingStyle.HOBBY, ExerciseRecord(hikingStyle = HikingStyle.PRO)))
        assertNull(hikingNote(HikingStyle.HOBBY, ExerciseRecord()))
    }

    @Test
    fun 러닝_안내_문구는_앞뒤_30초가_범위다() {
        assertEquals("내 페이스 5'30\" · 범위 안이에요", runningNote(345, ExerciseRecord(runningPaceSec = 330)))
        assertEquals("내 페이스 4'30\" · 범위보다 빨라요", runningNote(345, ExerciseRecord(runningPaceSec = 270)))
        assertEquals("내 페이스 7'00\" · 범위보다 느려요", runningNote(345, ExerciseRecord(runningPaceSec = 420)))
        assertEquals("내 기록을 아직 안 적었어요", runningNote(345, ExerciseRecord()))
    }

    @Test
    fun 막대_눈금은_호스트_범위와_내_기록이_모두_보이게_잡는다() {
        assertEquals(BarScale(50, 110), barScaleOf(70..90, null, BodyPart.CHEST))
        assertEquals("내 기록이 범위 밖이면 눈금이 넓어진다", BarScale(50, 130), barScaleOf(70..90, 105, BodyPart.CHEST))
        assertEquals("턱걸이는 앞뒤 3회 여유", BarScale(3, 13), barScaleOf(6..10, 7, BodyPart.BACK))
        assertEquals("0 아래로 내려가지 않는다", 0, barScaleOf(0..2, 0, BodyPart.BACK).min)
    }

    @Test
    fun 설명_문구가_성향과_숙련도마다_있다() {
        assertEquals(2, HikingStyle.entries.map(::hikingStyleDescription).toSet().size)
        assertEquals(3, SkillLevel.entries.map(::armLevelDescription).toSet().size)
    }

    @Test
    fun 받침에_따라_예요_이에요를_고른다() {
        assertEquals("예요", copula("취미"))
        assertEquals("이에요", copula("전문"))
        assertEquals("이에요", copula("중급"))
        assertEquals("이에요", copula("abc"))
        assertEquals("이에요", copula(""))
    }
}
