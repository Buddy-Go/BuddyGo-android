package com.example.myapplication.data.model

import com.example.myapplication.data.mock.MockMeetings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingTest {
    @Test
    fun 체크인한_참가자_수를_센다() {
        val meeting = Meeting(
            members = listOf(MeetingMember("a", "가", true), MeetingMember("b", "나"), MeetingMember("c", "다", true)),
        )
        assertEquals(2, meeting.checkedInCount)
        assertEquals(0, Meeting().checkedInCount)
    }

    @Test
    fun 목데이터에_판정이_맞음_일부_맞지_않음이_모두_있다() {
        val results = MockMeetings.all.mapNotNull { it.criteriaJudgement?.result }.toSet()
        assertEquals(CriteriaMatchResult.entries.toSet(), results)
    }

    @Test
    fun 일부만_맞으면_범위_밖_부위가_담겨_있다() {
        val judgement = MockMeetings.recruiting.criteriaJudgement!!
        assertEquals(CriteriaMatchResult.PARTIAL, judgement.result)
        assertEquals(listOf(BodyPart.CHEST), judgement.outOfRangeParts)
    }

    @Test
    fun 판정이_없는_모임도_있다() {
        assertNull(MockMeetings.hosted.criteriaJudgement)
    }

    @Test
    fun 명단이_공개되는_모임에만_참가자가_담겨_있다() {
        assertTrue(MockMeetings.recruiting.members.isEmpty())
        assertEquals(2, MockMeetings.closed.members.size)
        assertEquals(1, MockMeetings.inProgress.checkedInCount)
        assertEquals(2, MockMeetings.ended.checkedInCount)
    }
}
