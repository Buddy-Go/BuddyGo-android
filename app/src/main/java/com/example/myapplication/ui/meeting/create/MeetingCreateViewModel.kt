package com.example.myapplication.ui.meeting.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class MeetingCreateViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {

    var state by mutableStateOf(MeetingCreateState(dateOptions = nextDays(), selectedDateIndex = 0))
        private set

    init {
        // '내 기록 표시'에 쓸 내 프로필 기록 (10d). 실패하면 기록 없음으로 둔다.
        viewModelScope.launch {
            userRepository.getCurrentUser().onSuccess { me ->
                state = state.copy(healthCriteria = state.healthCriteria.copy(myRecord = me.record))
            }
        }
    }

    fun onTitleChange(title: String) {
        state = state.copy(title = title)
    }

    fun onDateSelect(index: Int) {
        if (index !in state.dateOptions.indices) return
        state = state.copy(selectedDateIndex = index)
    }

    fun onCalendarClick() {
        // TODO(임시): 5일 뒤 날짜를 고르는 달력 디자인이 없음. 팀에 질문.
    }

    fun onTimeClick() {
        state = state.copy(openSheet = CreateSheet.TIME)
    }

    fun onCriteriaClick() {
        state = state.copy(openSheet = CreateSheet.HEALTH_CRITERIA)
    }

    fun onCapacityClick() {
        // TODO(임시): 정원·성별 시트 디자인이 없음. 팀에 질문.
    }

    fun onDescriptionClick() {
        state = state.copy(openSheet = CreateSheet.DESCRIPTION)
    }

    fun onSheetDismiss() {
        state = state.copy(openSheet = null)
    }

    // 10f 확인
    fun onTimeApply(startMinutes: Int, durationMinutes: Int) {
        // TODO(임시): '시작은 지금부터 1시간 30분 이후' 검사는 서버 판정 결과를 받아 표시 (service-rules 0장, 4장)
        state = state.copy(startMinutes = startMinutes, durationMinutes = durationMinutes, openSheet = null)
    }

    // 10d 적용
    fun onHealthCriteriaApply(parts: List<BodyPart>, showMyRecord: Boolean) {
        if (parts.isEmpty()) return
        state = state.copy(
            healthCriteria = state.healthCriteria.copy(parts = parts, showMyRecord = showMyRecord),
            openSheet = null,
        )
    }

    // 10e 적용
    fun onDescriptionApply(description: String) {
        state = state.copy(description = description.take(DESCRIPTION_MAX_LENGTH), openSheet = null)
    }

    fun onSubmit() {
        if (!state.canSubmit) return
        // TODO(임시): 공용 모델·MeetingRepository가 정해지면 개설 요청 연결
    }

    private fun nextDays(count: Int = 5): List<DateOption> {
        val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")
        val cal = Calendar.getInstance()
        return List(count) { i ->
            val option = DateOption(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH),
                topLabel = if (i == 0) "오늘" else weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1],
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
            option
        }
    }
}
