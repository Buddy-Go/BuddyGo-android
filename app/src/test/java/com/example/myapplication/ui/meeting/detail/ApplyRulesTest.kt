package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.SkillLevel
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyRulesTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private fun millis(month: Int, day: Int, hour: Int, min: Int = 0) =
        Calendar.getInstance(seoul).apply { clear(); set(2026, month - 1, day, hour, min) }.timeInMillis

    @Test
    fun 한마디는_이백_자까지만_받는다() {
        assertEquals(200, limitMessage("가".repeat(250)).length)
        assertEquals("짧은 글", limitMessage("짧은 글"))
        assertEquals(200, MAX_MESSAGE_LENGTH)
    }

    @Test
    fun 비었거나_공백뿐인_한마디는_보내지_않는다() {
        assertNull(messageToSend(""))
        assertNull(messageToSend("   \n "))
        assertEquals("같이 해요", messageToSend("  같이 해요  "))
    }

    @Test
    fun 신청_화면_부제목은_날짜_시각_장소_순이다() {
        val m = Meeting(startAtMillis = millis(9, 8, 7), place = MeetingPlace(name = "북한산 우이동 들머리"))
        assertEquals("9/8 (화) 오전 7:00 · 북한산 우이동 들머리", applySubtitle(m, seoul))
        assertEquals("9/8 (화) 오후 12:30", applySubtitle(Meeting(startAtMillis = millis(9, 8, 12, 30)), seoul))
    }

    @Test
    fun 헬스는_이_모임이_보는_부위의_내_기록만_요약한다() {
        val m = Meeting(criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST, BodyPart.ARM, BodyPart.BACK), null))
        val mine = ExerciseRecord(benchPressKg = 80, pullUpCount = 8, squatKg = 100, armLevel = SkillLevel.INTERMEDIATE)
        assertEquals("가슴 80kg · 등 8회 · 팔 중급", myRecordSummary(m, mine))
    }

    @Test
    fun 헬스는_기록이_없는_부위를_건너뛰고_하나도_없으면_미설정이다() {
        val m = Meeting(criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST, BodyPart.LEGS), null))
        assertEquals("가슴 80kg", myRecordSummary(m, ExerciseRecord(benchPressKg = 80)))
        assertNull("이 모임이 보지 않는 부위는 요약에 넣지 않는다", myRecordSummary(m, ExerciseRecord(armLevel = SkillLevel.ADVANCED)))
        assertNull(myRecordSummary(m, null))
    }

    @Test
    fun 러닝과_등산_요약() {
        val running = Meeting(criteria = MeetingCriteria.Running(5f, 345))
        assertEquals("5'30\"/km", myRecordSummary(running, ExerciseRecord(runningPaceSec = 330)))
        assertNull(myRecordSummary(running, ExerciseRecord()))
        val hiking = Meeting(criteria = MeetingCriteria.Hiking(HikingStyle.HOBBY))
        assertEquals("취미", myRecordSummary(hiking, ExerciseRecord(hikingStyle = HikingStyle.HOBBY)))
        assertEquals("전문", myRecordSummary(hiking, ExerciseRecord(hikingStyle = HikingStyle.PRO)))
        assertNull(myRecordSummary(hiking, null))
    }

    @Test
    fun 성별_조건에_맞는지_판단한다() {
        assertTrue(genderAllowed(GenderLimit.ANYONE, Gender.MALE))
        assertTrue(genderAllowed(GenderLimit.FEMALE_ONLY, Gender.FEMALE))
        assertFalse(genderAllowed(GenderLimit.FEMALE_ONLY, Gender.MALE))
        assertTrue(genderAllowed(GenderLimit.MALE_ONLY, Gender.MALE))
        assertFalse(genderAllowed(GenderLimit.MALE_ONLY, Gender.FEMALE))
    }

    @Test
    fun 내_성별을_모르면_막지_않는다() {
        assertTrue(genderAllowed(GenderLimit.FEMALE_ONLY, null))
        assertTrue(genderAllowed(GenderLimit.MALE_ONLY, null))
    }
}
