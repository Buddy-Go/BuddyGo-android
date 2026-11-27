package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.api.ApiException
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.TimeProposal
import com.example.myapplication.data.model.TimeProposalStatus
import com.example.myapplication.data.model.VoteDecision
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone

// 시작 시각 변경 제안의 계산 규칙 (서버 PR #43 '제안할 수 있는 조건'). 화면 없이 테스트할 수 있게 순수 함수로 둔다.
// 서버가 다시 검사하므로 여기서는 고를 수 없는 것을 미리 막아 주는 용도다.
//   용어: 기존 시작 시각(지금 정해진 시각) · 제안한 시작 시각 · 현재 시각

private const val MINUTE = 60_000L
const val TIME_PROPOSAL_MAX_SHIFT_MINUTES = 60      // 기존 시작에서 앞뒤 최대 1시간
const val TIME_PROPOSAL_STEP_MINUTES = 10           // 10분 단위
const val TIME_PROPOSAL_CLOSED_LEAD_MINUTES = 30    // 마감 모임: 현재 시각보다 30분 넘게 뒤
const val TIME_PROPOSAL_RECRUITING_LEAD_MINUTES = 90 // 모집중 모임: 현재 시각보다 1시간 30분 넘게 뒤

// 정원이 다 찬 모임은 마감 규칙, 아니면 모집중 규칙을 따른다.
private fun usesClosedRule(meeting: Meeting): Boolean = meeting.status == MeetingStatus.CLOSED || meeting.isFull

private fun minLeadMinutes(meeting: Meeting): Int =
    if (usesClosedRule(meeting)) TIME_PROPOSAL_CLOSED_LEAD_MINUTES else TIME_PROPOSAL_RECRUITING_LEAD_MINUTES

// 제안·응답을 할 수 있는 마지막 시각. 마감: 기존 시작 10분 전(체크인 열림) · 모집중: 기존 데드라인
fun timeProposalWindowEndMillis(meeting: Meeting): Long =
    if (usesClosedRule(meeting)) meeting.startAtMillis - CHECK_IN_OPENS_BEFORE_MINUTES * MINUTE else meeting.deadlineAtMillis

// 지금 이 사람에게 '시간 변경 제안' 진입점을 보여 줄지.
//  - 호스트·확정 참가자만, 모집중·마감 모임만
//  - 호스트 말고 확정 참가자가 1명 이상 (없으면 호스트가 모임 수정으로 바꾼다)
//  - 제안 기한 전이고, 진행 중인 제안이 없고, 고를 수 있는 시각이 하나라도 있을 때
fun canProposeTime(meeting: Meeting, role: MeetingRole, current: TimeProposal?, nowMillis: Long): Boolean =
    (role == MeetingRole.HOST || role == MeetingRole.CONFIRMED) &&
        (meeting.status == MeetingStatus.RECRUITING || meeting.status == MeetingStatus.CLOSED) &&
        meeting.confirmedCount > 1 &&
        current == null &&
        nowMillis < timeProposalWindowEndMillis(meeting) &&
        timeProposalOptions(meeting, nowMillis).any { it.enabled }

// 진행 중인 제안을 불러올 사람인지 (그 외에는 서버가 TP002로 거절한다)
fun shouldLoadTimeProposal(meeting: Meeting, role: MeetingRole): Boolean =
    (role == MeetingRole.HOST || role == MeetingRole.CONFIRMED) &&
        (meeting.status == MeetingStatus.RECRUITING || meeting.status == MeetingStatus.CLOSED)

// ---- 시각 선택 시트 ----

// shiftMinutes: 기존 시작에서 몇 분 옮기는지 (앞당기면 음수)
data class TimeOption(val startAtMillis: Long, val shiftMinutes: Int, val enabled: Boolean)

// 기존 시작 ±1시간을 10분 단위로 (0분 제외, 최대 12개). 현재 시각보다 충분히 뒤인 것만 고를 수 있다.
fun timeProposalOptions(meeting: Meeting, nowMillis: Long): List<TimeOption> {
    val earliest = nowMillis + minLeadMinutes(meeting) * MINUTE // 이 시각 '넘게' 뒤여야 한다(같으면 불가)
    val windowOpen = nowMillis < timeProposalWindowEndMillis(meeting)
    return (-TIME_PROPOSAL_MAX_SHIFT_MINUTES..TIME_PROPOSAL_MAX_SHIFT_MINUTES step TIME_PROPOSAL_STEP_MINUTES)
        .filter { it != 0 }
        .map { shift ->
            val at = meeting.startAtMillis + shift * MINUTE
            TimeOption(at, shift, enabled = windowOpen && at > earliest)
        }
}

// 고른 시각으로 제안하면 정해질 응답 기한 = 조건이 처음 깨지는 시각
fun timeProposalExpiresAtMillis(meeting: Meeting, toStartAtMillis: Long): Long =
    minOf(timeProposalWindowEndMillis(meeting), toStartAtMillis - minLeadMinutes(meeting) * MINUTE)

// 시트 아래의 고를 수 없는 이유 안내
fun timeProposalLeadNote(meeting: Meeting): String =
    if (usesClosedRule(meeting)) "현재 시각보다 30분 넘게 뒤인 시각만 고를 수 있어요"
    else "모집 중인 모임은 현재 시각보다 1시간 30분 넘게 뒤인 시각만 고를 수 있어요"

// "30분 앞당김" / "1시간 미룸"
fun formatShift(shiftMinutes: Int): String {
    val abs = kotlin.math.abs(shiftMinutes)
    val amount = if (abs % 60 == 0) "${abs / 60}시간" else if (abs > 60) "${abs / 60}시간 ${abs % 60}분" else "${abs}분"
    return if (shiftMinutes < 0) "$amount 앞당김" else "$amount 미룸"
}

// "오후 7:30". 기준 시각과 날짜가 다르면 "11월 29일 오전 12:10"처럼 날짜를 붙인다.
fun formatClock(millis: Long, sameDayAs: Long? = null, timeZone: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
    val hour24 = c.get(Calendar.HOUR_OF_DAY)
    val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
    val time = "${if (hour24 < 12) "오전" else "오후"} $hour12:${c.get(Calendar.MINUTE).toString().padStart(2, '0')}"
    if (sameDayAs == null) return time
    val base = Calendar.getInstance(timeZone).apply { timeInMillis = sameDayAs }
    val sameDay = base.get(Calendar.YEAR) == c.get(Calendar.YEAR) && base.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) time else "${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.DAY_OF_MONTH)}일 $time"
}

// ---- 진행 중인 제안 카드 ----

enum class ProposalCardActions { RESPOND, WITHDRAW, NONE }

data class TimeProposalCardView(
    val proposalId: String,     // 접기·펼치기 상태를 제안마다 따로 둔다
    val summary: String,        // 접었을 때 한 줄: "제안 시각 오전 9:30 · 1/2명 수락"
    val title: String,          // "서연님이 시간 변경을 제안했어요"
    val fromText: String,       // "오후 7:00"
    val toText: String,         // "오후 7:30"
    val shiftText: String,      // "30분 미룸"
    val progressText: String,   // "2/3명 수락"
    val deadlineText: String,   // "오후 6:50까지 응답" / "응답 기한이 지났어요"
    val note: String?,          // 버튼 대신 보여 줄 안내
    val actions: ProposalCardActions,
)

fun timeProposalCardOf(
    proposal: TimeProposal,
    myUserId: String,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): TimeProposalCardView {
    val expired = nowMillis >= proposal.expiresAtMillis
    val isProposer = proposal.proposerId == myUserId
    val myDecision = proposal.voteOf(myUserId)?.decision
    val actions = when {
        expired -> ProposalCardActions.NONE
        isProposer -> ProposalCardActions.WITHDRAW
        myDecision == VoteDecision.PENDING -> ProposalCardActions.RESPOND
        else -> ProposalCardActions.NONE
    }
    val note = when {
        expired -> "모두 수락하지 않아 곧 무산돼요"
        actions == ProposalCardActions.WITHDRAW -> "모두 수락하면 시작 시각이 바뀌어요"
        myDecision == VoteDecision.ACCEPTED -> "수락했어요. 다른 메이트의 응답을 기다리고 있어요"
        actions == ProposalCardActions.RESPOND -> "한 명이라도 거절하면 제안은 무산돼요"
        else -> null
    }
    val shift = ((proposal.toStartAtMillis - proposal.fromStartAtMillis) / MINUTE).toInt()
    val toText = formatClock(proposal.toStartAtMillis, sameDayAs = proposal.fromStartAtMillis, timeZone = timeZone)
    val progressText = "${proposal.acceptedCount}/${proposal.votes.size}명 수락"
    return TimeProposalCardView(
        proposalId = proposal.id,
        summary = "제안 시각 $toText · $progressText",
        title = if (isProposer) "시간 변경을 제안했어요" else "${proposal.proposerNickname}님이 시간 변경을 제안했어요",
        fromText = formatClock(proposal.fromStartAtMillis, timeZone = timeZone),
        toText = toText,
        shiftText = formatShift(shift),
        progressText = progressText,
        deadlineText = if (expired) "응답 기한이 지났어요" else "${formatClock(proposal.expiresAtMillis, sameDayAs = nowMillis, timeZone = timeZone)}까지 응답",
        note = note,
        actions = actions,
    )
}

// 시각으로 정해지는 값(응답 기한 지남, 제안 기한 지남)이 바뀌는 다음 시각
fun nextTimeProposalChangeMillis(meeting: Meeting, proposal: TimeProposal?, nowMillis: Long): Long? =
    listOfNotNull(
        proposal?.expiresAtMillis,
        timeProposalWindowEndMillis(meeting),
        // 고를 수 있는 시각이 하나 줄어드는 시각 (시트의 선택지가 제때 꺼지고, 마지막 선택지가 사라지면 진입점도 숨겨진다)
        timeProposalOptions(meeting, nowMillis).filter { it.enabled }.minOfOrNull { it.startAtMillis - minLeadMinutes(meeting) * MINUTE },
    ).filter { it > nowMillis }.minOrNull()

// ---- 응답 결과·실패 안내 ----

// 수락 결과에 따른 안내. 200이어도 CANCELED(그 사이 명단이 바뀜)일 수 있다.
fun acceptResultNotice(status: TimeProposalStatus): Pair<String, Boolean> = when (status) {
    TimeProposalStatus.APPROVED -> "모두 수락해서 시작 시각이 바뀌었어요" to true
    TimeProposalStatus.PENDING -> "수락했어요. 모두 수락하면 시각이 바뀌어요" to true
    TimeProposalStatus.CANCELED -> "참가자가 바뀌어 제안이 무산됐어요" to false
    else -> "이미 끝난 제안이에요" to false
}

// 서버가 TP00x로 거절하면 그 이유 문장을 그대로 보여 준다. (서버 PR #44에서 사용자용 문장으로 다듬었다)
fun timeProposalErrorMessage(e: Throwable, fallback: String): String = when {
    e is IOException -> "인터넷 연결을 확인하고 다시 시도해 주세요"
    e is ApiException && e.code.startsWith("TP") && !e.message.isNullOrBlank() -> e.message!!
    else -> fallback
}
