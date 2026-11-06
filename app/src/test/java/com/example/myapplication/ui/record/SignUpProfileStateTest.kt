package com.example.myapplication.ui.record

import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignUpProfileStateTest {
    @Test
    fun 선택한_종목은_헬스_러닝_등산_순서로_보인다() {
        val state = SignUpProfileState(sports = setOf(Sport.HIKING, Sport.HEALTH))
        assertEquals(listOf(Sport.HEALTH, Sport.HIKING), state.selectedSports)
    }

    @Test
    fun 기록이_하나라도_있으면_입력_완료() {
        assertFalse(ExerciseRecord().hasRecordFor(Sport.HEALTH))
        assertTrue(ExerciseRecord(pullUpCount = 0).hasRecordFor(Sport.HEALTH)) // 턱걸이 0회도 입력한 값
        assertTrue(ExerciseRecord(armLevel = SkillLevel.BEGINNER).hasRecordFor(Sport.HEALTH))
        assertTrue(ExerciseRecord(runningPaceSec = 360).hasRecordFor(Sport.RUNNING))
        assertTrue(ExerciseRecord(hikingStyle = HikingStyle.HOBBY).hasRecordFor(Sport.HIKING))
        assertFalse("다른 종목 기록은 세지 않음", ExerciseRecord(runningPaceSec = 360).hasRecordFor(Sport.HIKING))
    }

    @Test
    fun 기록_줄의_회색_글자() {
        val state = SignUpProfileState(
            sports = setOf(Sport.HEALTH, Sport.RUNNING, Sport.HIKING),
            record = ExerciseRecord(benchPressKg = 80),
        )
        assertEquals("입력 완료", state.recordStatusText(Sport.HEALTH))
        assertEquals("페이스 미입력", state.recordStatusText(Sport.RUNNING))
        assertEquals("레벨 미입력", state.recordStatusText(Sport.HIKING))
    }

    @Test
    fun 종목을_해제하면_그_종목_기록만_지운다() {
        val record = ExerciseRecord(benchPressKg = 80, pullUpCount = 10, runningPaceSec = 330, hikingStyle = HikingStyle.PRO)
        val cleared = record.clearedFor(Sport.HEALTH)
        assertFalse(cleared.hasRecordFor(Sport.HEALTH))
        assertEquals(330, cleared.runningPaceSec)
        assertEquals(HikingStyle.PRO, cleared.hikingStyle)
    }
}
