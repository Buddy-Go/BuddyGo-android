package com.example.myapplication.ui.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.launch

// 활동 내역 (13-H, 13-H2, 13-P, 13-P2, 13-R)
// TODO(임시): 지금은 모임 전체 목록에서 내 역할로 골라낸다. 서버에 '내 활동' 조회가 생기면 그걸 쓴다.
class ActivityViewModel(
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    var state by mutableStateOf(ActivityState(now = System.currentTimeMillis()))
        private set

    fun onTabSelect(tab: ActivityTab) {
        state = state.copy(selectedTab = tab)
    }

    // 화면에 들어올 때마다 다시 불러온다 (개설·수락 결과가 바로 보이게).
    fun onResume() {
        viewModelScope.launch {
            val me = userRepository.getCurrentUser().getOrNull() ?: return@launch
            // TODO(임시): 실패하면 16f·16g 오류 화면 (공용 부품이 생기면)
            val meetings = meetingRepository.getMeetings().getOrNull() ?: return@launch
            state = buildActivityState(meetings, me.id, System.currentTimeMillis(), state.selectedTab)
        }
    }
}
