package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.api.ApiException
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.TimeProposal
import com.example.myapplication.data.model.TimeProposalStatus
import com.example.myapplication.data.model.TimeProposalVote
import com.example.myapplication.data.model.VoteDecision
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 서버 PR #43의 예시(19:00 시작 마감 모임, 현재 18:20)를 그대로 옮겼다.
class TimeProposalRulesTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val minute = 60_000L

    private fun at(hour: Int, min: Int = 0) =
        Calendar.getInstance(seoul).apply { clear(); set(2026, 10, 28, hour, min) }.timeInMillis

    private fun meeting(status: MeetingStatus = MeetingStatus.CLOSED, confirmed: Int = 3, capacity: Int = 3) = Meeting(
        id = "m", hostId = "h", startAtMillis = at(19), durationMinutes = 90,
        confirmedIds = listOf("h", "a", "b").take(confirmed), confirmedCount = confirmed, capacity = capacity, status = status,
    )

    private fun enabledAt(m: Meeting, now: Long) = timeProposalOptions(m, now).filter { it.enabled }.map { it.startAtMillis }

    @Test
    fun `선택지는 기존 시작 앞뒤 1시간을 10분 단위로 12개`() {
        val options = timeProposalOptions(meeting(), at(12))
        assertEquals(12, options.size)
        assertEquals(at(18), options.first().startAtMillis)
        assertEquals(at(20), options.last().startAtMillis)
        assertFalse(options.any { it.startAtMillis == at(19) })
    }

    @Test
    fun `마감 모임은 현재보다 30분 넘게 뒤인 시각만 고를 수 있다`() {
        val enabled = enabledAt(meeting(), at(18, 20))
        assertTrue(at(19, 30) in enabled)
        assertFalse(at(18, 40) in enabled)
        assertFalse("딱 30분은 불가", at(18, 50) in enabled)
        assertTrue(at(19, 0) + 10 * minute in enabled)
    }

    @Test
    fun `마감 모임은 체크인이 열리면 제안할 수 없다`() {
        assertTrue(enabledAt(meeting(), at(18, 50)).isEmpty())
        assertFalse(canProposeTime(meeting(), MeetingRole.HOST, null, at(18, 50)))
        assertTrue(canProposeTime(meeting(), MeetingRole.CONFIRMED, null, at(18, 20)))
    }

    @Test
    fun `모집중 모임은 데드라인까지, 현재보다 1시간 30분 넘게 뒤인 시각만`() {
        val m = meeting(status = MeetingStatus.RECRUITING, confirmed = 2, capacity = 4)
        val enabled = enabledAt(m, at(16, 50))
        assertFalse("딱 90분은 불가", at(18, 20) in enabled)
        assertTrue(at(18, 30) in enabled)
        assertFalse(canProposeTime(m, MeetingRole.HOST, null, at(17, 30))) // 데드라인
    }

    @Test
    fun `호스트만 있거나 진행 중인 제안이 있으면 진입점이 없다`() {
        assertFalse(canProposeTime(meeting(status = MeetingStatus.RECRUITING, confirmed = 1), MeetingRole.HOST, null, at(12)))
        assertFalse(canProposeTime(meeting(), MeetingRole.HOST, proposal(), at(12)))
        assertFalse(canProposeTime(meeting(), MeetingRole.PENDING, null, at(12)))
    }

    @Test
    fun `응답 기한은 조건이 처음 깨지는 시각`() {
        assertEquals(at(18, 50), timeProposalExpiresAtMillis(meeting(), at(19, 30)))
        assertEquals(at(18, 10), timeProposalExpiresAtMillis(meeting(), at(18, 40)))
    }

    @Test
    fun `카드 버튼은 내 투표와 제안자, 기한으로 정한다`() {
        val p = proposal()
        assertEquals(ProposalCardActions.RESPOND, timeProposalCardOf(p, "b", at(18), seoul).actions)
        assertEquals(ProposalCardActions.WITHDRAW, timeProposalCardOf(p, "a", at(18), seoul).actions)
        assertEquals(ProposalCardActions.NONE, timeProposalCardOf(p, "h", at(18), seoul).actions)
        assertEquals(ProposalCardActions.NONE, timeProposalCardOf(p, "b", at(18, 50), seoul).actions)
        val card = timeProposalCardOf(p, "b", at(18), seoul)
        assertEquals("오후 7:00", card.fromText)
        assertEquals("오후 7:30", card.toText)
        assertEquals("30분 미룸", card.shiftText)
        assertEquals("2/3명 수락", card.progressText)
        assertEquals("오후 6:50까지 응답", card.deadlineText)
    }

    @Test
    fun `수락 결과가 CANCELED면 실패가 아니라 무산 안내`() {
        assertEquals("참가자가 바뀌어 제안이 무산됐어요" to false, acceptResultNotice(TimeProposalStatus.CANCELED))
        assertTrue(acceptResultNotice(TimeProposalStatus.APPROVED).second)
    }

    @Test
    fun `실패 안내는 TP 코드면 서버 문장, 네트워크면 연결 안내`() {
        assertEquals("인터넷 연결을 확인하고 다시 시도해 주세요", timeProposalErrorMessage(IOException(), "x"))
        assertEquals("이미 진행 중인 제안이 있어요", timeProposalErrorMessage(ApiException(409, "TP004", "이미 진행 중인 제안이 있어요"), "x"))
        assertEquals("x", timeProposalErrorMessage(ApiException(500, "G001", "Internal"), "x"))
    }

    @Test
    fun `옮긴 시간 표기`() {
        assertEquals("1시간 앞당김", formatShift(-60))
        assertEquals("10분 미룸", formatShift(10))
    }

    private fun proposal() = TimeProposal(
        id = "7", status = TimeProposalStatus.PENDING, proposerId = "a", proposerNickname = "서연",
        fromStartAtMillis = at(19), toStartAtMillis = at(19, 30), expiresAtMillis = at(18, 50),
        votes = listOf(
            TimeProposalVote("h", "지훈", isHost = true, decision = VoteDecision.ACCEPTED),
            TimeProposalVote("a", "서연", isHost = false, decision = VoteDecision.ACCEPTED),
            TimeProposalVote("b", "민준", isHost = false, decision = VoteDecision.PENDING),
        ),
    )
}
