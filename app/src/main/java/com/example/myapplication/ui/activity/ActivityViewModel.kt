package com.example.myapplication.ui.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// 활동 내역 (13-H, 13-H2, 13-P, 13-P2, 13-R)
class ActivityViewModel : ViewModel() {

    // TODO(임시): MeetingRepository(공용, 문찬주)에 '내 활동' 조회가 생기면 거기서 불러온다.
    // 지금은 13-H·13-P 디자인과 비슷한 예시 값(헬스만).
    var state by mutableStateOf(sampleActivityState(System.currentTimeMillis()))
        private set

    fun onTabSelect(tab: ActivityTab) {
        state = state.copy(selectedTab = tab)
    }

    // 화면에 다시 들어올 때 남은 시간 글자를 새로 계산한다.
    fun onResume() {
        state = state.copy(now = System.currentTimeMillis())
    }
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR

internal fun sampleActivityState(now: Long) = ActivityState(
    joinedTotal = 27,
    hostedTotal = 9,
    now = now,
    reviewPending = listOf(
        ReviewPendingItem("p1", "등·이두 같이 치실 분", endedAt = now - 2 * DAY, reviewDueAt = now + 5 * DAY),
    ),
    hosted = listOf(
        HostedMeetingItem("h1", "퇴근 후 가슴·팔 같이 치실 분", now + 7 * HOUR, 2, 3, HostedStatus.RECRUITING, pendingApplicantCount = 2, deadlineAt = now + 5 * HOUR + 30 * MINUTE),
        HostedMeetingItem("h2", "하체 데이 스쿼트 같이 해요", now + 3 * DAY, 3, 3, HostedStatus.CLOSED),
        HostedMeetingItem("h3", "어깨 오버헤드 프레스 루틴", now + 7 * DAY, 1, 3, HostedStatus.RECRUITING, pendingApplicantCount = 1, deadlineAt = now + 6 * DAY),
        HostedMeetingItem("h4", "등·이두 같이 치실 분", now - 2 * DAY, 3, 3, HostedStatus.ENDED),
        HostedMeetingItem("h5", "아침 가슴 운동", now - 9 * DAY, 2, 2, HostedStatus.ENDED),
    ),
    joined = listOf(
        JoinedMeetingItem("j1", "벤치 80kg 도전 같이 해요", now + DAY, "스포애니 합정점", 0.8, JoinedStatus.CONFIRMED),
        JoinedMeetingItem("j2", "퇴근 후 등 운동", now - 3 * DAY, "에이블짐 망원점", 3.0, JoinedStatus.ATTENDED),
        JoinedMeetingItem("j3", "주말 하체 루틴", now - 4 * DAY, "스포애니 합정점", null, JoinedStatus.NO_SHOW),
    ),
)
