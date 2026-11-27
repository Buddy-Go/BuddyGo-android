package com.example.myapplication.ui.meeting.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.TimeProposal
import com.example.myapplication.data.model.TimeProposalStatus
import com.example.myapplication.data.model.TimeProposalVote
import com.example.myapplication.data.model.VoteDecision
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeNoticeRepository
import com.example.myapplication.data.repository.FakeTimeProposalRepository
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.ui.theme.MyApplicationTheme

// 모임 상세 전체 화면에서 시간 변경 제안이 어떻게 보이는지 목데이터로 띄운다. (나 = 민준 u1)
// 기기에서 보려면: adb shell am start -n com.example.myapplication/androidx.compose.ui.tooling.PreviewActivity \
//   --es composable com.example.myapplication.ui.meeting.detail.TimeProposalScreenPreviewsKt.<함수 이름>
// ViewModel이 코루틴으로 목 저장소를 읽으므로 Android Studio 정적 미리보기에서는 로딩 화면만 보일 수 있다(인터랙티브 모드나 기기에서 본다).

private const val MIN = 60_000L

@Composable
private fun DetailWithFakes(meetingId: String, proposal: TimeProposal?, openSheet: Boolean = false) {
    val vm = remember {
        MeetingDetailViewModel(
            meetingId = meetingId,
            repository = FakeMeetingRepository(),
            userRepository = FakeUserRepository(),
            noticeRepository = FakeNoticeRepository(),
            timeProposalRepository = FakeTimeProposalRepository(proposal?.let { mapOf(meetingId to it) }.orEmpty()),
        )
    }
    MyApplicationTheme(dynamicColor = false) {
        MeetingDetailScreen(
            meetingId = meetingId,
            onBack = {}, onApply = {}, onReview = {}, onAddRecord = {}, onManageParticipants = {},
            onWriteNotice = {}, onOpenNotices = {}, onOpenNotice = {}, onEditMeeting = {},
            openTimeProposal = openSheet,
            viewModel = vm,
        )
    }
}

// 확정 참가자(민준): 호스트 지훈이 30분 미루자고 제안했고 내 응답을 기다린다
@Preview(name = "상세 - 참가자, 응답할 차례", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewDetailParticipantRespond() {
    val m = MockMeetings.closed
    DetailWithFakes(
        m.id,
        TimeProposal(
            id = "7", status = TimeProposalStatus.PENDING, proposerId = "u3", proposerNickname = "지훈",
            fromStartAtMillis = m.startAtMillis, toStartAtMillis = m.startAtMillis + 30 * MIN,
            expiresAtMillis = m.startAtMillis - 10 * MIN,
            votes = listOf(
                TimeProposalVote("u3", "지훈", isHost = true, decision = VoteDecision.ACCEPTED),
                TimeProposalVote("u1", "민준", isHost = false, decision = VoteDecision.PENDING),
            ),
        ),
    )
}

// 호스트(민준): 확정 참가자가 있어 '일시' 줄에 '시간 변경 제안' 진입점이 보인다
@Preview(name = "상세 - 호스트, 제안 진입점", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewDetailHostEntry() = DetailWithFakes(MockMeetings.hostedClosed.id, null)

// 호스트(민준): 내가 한 제안을 기다리는 중
@Preview(name = "상세 - 호스트, 내가 제안함", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewDetailHostProposed() {
    val m = MockMeetings.hostedClosed
    DetailWithFakes(
        m.id,
        TimeProposal(
            id = "8", status = TimeProposalStatus.PENDING, proposerId = "u1", proposerNickname = "민준",
            fromStartAtMillis = m.startAtMillis, toStartAtMillis = m.startAtMillis - 40 * MIN,
            expiresAtMillis = m.startAtMillis - 70 * MIN,
            votes = listOf(
                TimeProposalVote("u1", "민준", isHost = true, decision = VoteDecision.ACCEPTED),
                TimeProposalVote("u2", "서연", isHost = false, decision = VoteDecision.ACCEPTED),
                TimeProposalVote("u3", "지훈", isHost = false, decision = VoteDecision.PENDING),
            ),
        ),
    )
}

// 호스트(민준): 시각 선택 시트를 연 상태
@Preview(name = "상세 - 시각 선택 시트", widthDp = 390, heightDp = 844)
@Composable
private fun PreviewDetailSheet() = DetailWithFakes(MockMeetings.hostedClosed.id, null, openSheet = true)
