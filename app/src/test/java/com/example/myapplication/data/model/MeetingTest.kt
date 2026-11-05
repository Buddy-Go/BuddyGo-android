package com.example.myapplication.data.model

import com.example.myapplication.data.mock.MockMeetings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingTest {
    @Test
    fun roleOf_구분() {
        assertEquals(MeetingRole.HOST, MockMeetings.hosted.roleOf("u1"))
        assertEquals(MeetingRole.CONFIRMED, MockMeetings.closed.roleOf("u1"))
        assertEquals(MeetingRole.PENDING, MockMeetings.pending.roleOf("u1"))
        assertEquals(MeetingRole.NONE, MockMeetings.recruiting.roleOf("u1"))
    }

    @Test
    fun 마감은_확정이_정원과_같을_때만() {
        assertTrue(MockMeetings.closed.isFull)
        assertFalse(MockMeetings.recruiting.isFull)
    }

    @Test
    fun 종료시각은_시작에_진행시간을_더한_값() {
        val m = MockMeetings.recruiting
        assertEquals(m.startAtMillis + 90 * 60_000L, m.endAtMillis)
    }

    @Test
    fun 종목은_기준에서_정해진다() {
        assertEquals(Sport.HEALTH, MockMeetings.recruiting.sport)
        assertEquals(Sport.RUNNING, MockMeetings.pending.sport)
        assertEquals(Sport.HIKING, MockMeetings.inProgress.sport)
    }
}
