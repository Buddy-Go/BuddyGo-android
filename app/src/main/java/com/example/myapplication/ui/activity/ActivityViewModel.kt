package com.example.myapplication.ui.activity

import com.example.myapplication.data.model.Sport
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.ui.common.ErrorType
import kotlinx.coroutines.launch

// 활동 내역 (13-H, 13-H2, 13-P, 13-P2, 13-R)
// TODO(임시): 지금은 모임 전체 목록에서 내 역할로 골라낸다. 서버에 '내 활동' 조회가 생기면 그걸 쓴다.
class ActivityViewModel(
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    var state by mutableStateOf(ActivityState(now = System.currentTimeMillis()))
        private set

    // 13-H·13-P 종목 필터
    fun onSportFilterSelect(sport: Sport?) {
        state = state.copy(sportFilter = sport)
    }

    fun onTabSelect(tab: ActivityTab) {
        state = state.copy(selectedTab = tab)
    }

    // 화면에 들어올 때마다 다시 불러온다 (개설·수락 결과가 바로 보이게).
    fun onResume() {
        viewModelScope.launch {
            val me = userRepository.getCurrentUser().getOrNull()
            val meetings = meetingRepository.getMeetings().getOrNull()
            if (me == null || meetings == null) {
                state = state.copy(loadError = ErrorType.SERVER) // 16g. 실패 종류를 구분할 수 없어 서버 오류로 둔다
                return@launch
            }
            state = buildActivityState(meetings, me.id, System.currentTimeMillis(), state.selectedTab, state.sportFilter)
        }
    }
}
