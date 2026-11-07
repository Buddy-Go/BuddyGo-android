package com.example.myapplication.ui.meeting.manage

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.JoinRequestStatus
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.repository.FakeMeetingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 참가자 관리(7a~7d): 공용 모델 → 화면 상태, 표시 글자
class ParticipantManageStateTest {

    private val meeting = Meeting(
        id = "m",
        hostId = "u1",
        hostNickname = "민준",
        hostMannerTemperature = 41.5f,
        capacity = 3,
        confirmedIds = listOf("u1", "u2"),
        confirmedCount = 2,
        status = MeetingStatus.RECRUITING,
        criteria = MeetingCriteria.Health(emptySet()),
    )
    private val users = MockUsers.all.associateBy { it.id }
    private val requests = listOf(
        JoinRequest("r1", "m", "u3", "지훈", 41.0f, ExerciseRecord(benchPressKg = 90, armLevel = SkillLevel.ADVANCED), "안녕하세요"),
        JoinRequest("r2", "m", "u4", "유나", 37.0f, ExerciseRecord(), null),
        JoinRequest("r3", "m", "u5", "취소한 사람", 36.5f, status = JoinRequestStatus.CANCELED),
    )

    @Test
    fun 호스트가_맨_앞이고_대기_신청만_보임() {
        val s = buildParticipantState(meeting, requests, users)
        assertEquals(listOf("u1", "u2"), s.confirmed.map { it.userId })
        assertTrue(s.confirmed.first().isHost)
        assertEquals(listOf("r1", "r2"), s.applicants.map { it.applicationId })
        assertFalse(s.isClosed)
        assertTrue(s.showTabs)
    }

    @Test
    fun 확정이_정원과_같으면_마감_7d() {
        val full = meeting.copy(confirmedIds = listOf("u1", "u2", "u3"), confirmedCount = 3)
        val s = buildParticipantState(full, emptyList(), users)
        assertTrue(s.isClosed)
        assertFalse(s.showTabs)
        assertEquals(ManageTab.CONFIRMED, s.visibleTab)
        assertEquals("모집이 마감돼서 더 이상 신청을 받지 않아요", s.confirmedHint)
    }

    @Test
    fun 헬스_기록_요약과_미설정() {
        val s = buildParticipantState(meeting, requests, users)
        assertEquals("가슴 90kg · 팔 고급", s.applicants[0].recordLabel)
        assertEquals("미설정", s.applicants[1].recordLabel)
        assertNull(healthRecordSummary(ExerciseRecord(runningPaceSec = 330))) // 러닝 기록만 있으면 헬스는 미설정
        assertEquals("등 0회", healthRecordSummary(ExerciseRecord(pullUpCount = 0)))
    }

    @Test
    fun 종목별_신청자_기록_요약() {
        val running = ExerciseRecord(runningPaceSec = 330)
        val hiking = ExerciseRecord(hikingStyle = HikingStyle.PRO)
        assertEquals("페이스 5'30\"", applicantRecordSummary(Sport.RUNNING, running))
        assertNull(applicantRecordSummary(Sport.RUNNING, ExerciseRecord()))
        assertEquals("전문", applicantRecordSummary(Sport.HIKING, hiking))
        assertNull(applicantRecordSummary(Sport.HIKING, running))
        // 헬스 모임이면 러닝 기록만 있어도 미설정
        assertNull(applicantRecordSummary(Sport.HEALTH, running))
    }

    @Test
    fun 표시_글자() {
        assertEquals("매너온도 44.1°C", Applicant("a", "u", "n", 44.1, null, null).mannerLabel)
        assertEquals("매너온도 40.0°C", ConfirmedMember("x", "x", 40.0).mannerLabel)
        assertEquals("참가자에게는 마감 후 명단이 공개돼요", buildParticipantState(meeting, emptyList(), users).confirmedHint)
    }

    @Test
    fun 공용_가짜_저장소로_수락하면_확정에_들어감() = runBlocking {
        val repo = FakeMeetingRepository()
        val hosted = MockMeetings.all.first { it.hostId == MockUsers.me.id && it.pendingIds.isNotEmpty() }
        val request = repo.getJoinRequests(hosted.id).getOrThrow().first()
        repo.accept(hosted.id, request.id)
        val after = repo.getMeeting(hosted.id).getOrThrow()
        val s = buildParticipantState(after, repo.getJoinRequests(hosted.id).getOrThrow(), users)
        assertTrue(s.confirmed.any { it.userId == request.applicantId })
        assertFalse(s.applicants.any { it.applicationId == request.id })
    }
}
