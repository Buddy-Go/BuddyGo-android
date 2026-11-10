package com.example.myapplication.ui.activity

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingMember
import com.example.myapplication.data.model.MeetingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 13: 저장소의 모임 목록 → 호스트·참여 탭, 평가 대기
class ActivityBuildTest {

    private val now = 1_000_000_000_000L
    private val hour = 3_600_000L
    private val me = "u1"

    private val meetings = listOf(
        Meeting(id = "h1", hostId = me, confirmedIds = listOf(me), confirmedCount = 1, capacity = 3,
            pendingIds = listOf("u2"), startAtMillis = now + 5 * hour, status = MeetingStatus.RECRUITING),
        Meeting(id = "h2", hostId = me, startAtMillis = now - 30 * hour, durationMinutes = 60, status = MeetingStatus.ENDED),
        Meeting(id = "j1", hostId = "u3", confirmedIds = listOf("u3", me), startAtMillis = now + hour, status = MeetingStatus.CLOSED),
        Meeting(id = "j2", hostId = "u3", pendingIds = listOf(me), startAtMillis = now + hour, distanceMeters = 800),
        Meeting(id = "j3", hostId = "u3", confirmedIds = listOf("u3", me), startAtMillis = now - 200 * hour, status = MeetingStatus.ENDED),
        Meeting(id = "x", hostId = "u3", startAtMillis = now), // 나와 상관없음
    )

    @Test
    fun splitsHostedAndJoined() {
        val s = buildActivityState(meetings, me, now)
        assertEquals(listOf("h1", "h2"), s.hosted.map { it.meetingId })
        assertEquals(listOf("j1", "j2", "j3"), s.joined.map { it.meetingId })
        assertEquals(JoinedStatus.CONFIRMED, s.joined[0].status)
        assertEquals(JoinedStatus.PENDING, s.joined[1].status)
        assertTrue(kotlin.math.abs(s.joined[1].distanceKm!! - 0.8) < 0.0001)
        assertEquals(JoinedStatus.ATTENDED, s.joined[2].status)
    }

    @Test
    fun recruitingHasDeadlineAndApplicantCount() {
        val h1 = buildActivityState(meetings, me, now).hosted.first { it.meetingId == "h1" }
        assertEquals(1, h1.pendingApplicantCount)
        assertEquals(now + 5 * hour - 90 * 60_000L, h1.deadlineAt)
        assertNull(buildActivityState(meetings, me, now).hosted.first { it.meetingId == "h2" }.deadlineAt)
    }

    @Test
    fun reviewPendingOnlyWithinWindow() {
        val s = buildActivityState(meetings, me, now)
        // h2는 29시간 전에 끝나 기한 안, j3는 199시간 전에 끝나 기한 지남
        assertEquals(listOf("h2"), s.reviewPending.map { it.meetingId })
        assertTrue(s.reviewPending[0].reviewDueAt > now)
    }

    @Test
    fun keepsSelectedTab() {
        assertEquals(ActivityTab.JOINED, buildActivityState(meetings, me, now, ActivityTab.JOINED).selectedTab)
    }

    @Test
    fun 끝난_모임에서_체크인하지_않았으면_노쇼다() {
        val ended = Meeting(
            id = "e1", hostId = "u3", confirmedIds = listOf("u3", me), startAtMillis = now - 200 * hour, status = MeetingStatus.ENDED,
            members = listOf(MeetingMember("u3", "호스트", checkedIn = true), MeetingMember(me, "나", checkedIn = false)),
        )
        val attended = ended.copy(id = "e2", members = listOf(MeetingMember("u3", "호스트", true), MeetingMember(me, "나", true)))

        val s = buildActivityState(listOf(ended, attended), me, now)

        assertEquals(JoinedStatus.NO_SHOW, s.joined.first { it.meetingId == "e1" }.status)
        assertEquals(JoinedStatus.ATTENDED, s.joined.first { it.meetingId == "e2" }.status)
        assertEquals(1, s.joinedTotal) // '참여 n'은 참여 완료만 센다
    }
}
