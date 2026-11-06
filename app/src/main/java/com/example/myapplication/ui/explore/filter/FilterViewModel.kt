package com.example.myapplication.ui.explore.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.MeetingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FilterViewModel(
    // TODO(임시) 서버 연결 전까지 Fake를 쓴다.
    private val repository: MeetingRepository = FakeMeetingRepository(),
    nowMillis: Long = System.currentTimeMillis(),
) : ViewModel() {
    // 지금 적용된 조건에서 시작한다.
    private val _state = MutableStateFlow(FilterState(draft = FilterStore.applied.value, nowMillis = nowMillis))
    val state: StateFlow<FilterState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMeetings().onSuccess { list -> _state.update { it.copy(meetings = list) } }
        }
    }

    // 종목은 하나만 고르고, 고른 종목을 다시 누르면 해제(모든 종목)된다.
    fun onSelectSport(sport: Sport) = change { it.withSport(if (it.sport == sport) null else sport) }

    // 날짜도 같은 날을 다시 누르면 해제(날짜 무관)된다.
    fun onSelectDate(startOfDayMillis: Long) =
        change { it.copy(dateMillis = if (it.dateMillis == startOfDayMillis) null else startOfDayMillis) }

    fun onToggleTimeSlot(slot: TimeSlot) = change {
        it.withTimeSlots(if (slot in it.timeSlots) it.timeSlots - slot else it.timeSlots + slot)
    }

    fun onClearTimeRange() = change { it.withCustomRange(null) }

    fun onReset() = change { MeetingFilter() }

    fun onOpenSheet(sheet: FilterSheet) = _state.update { it.copy(openSheet = sheet) }
    fun onCloseSheet() = _state.update { it.copy(openSheet = null) }

    // 화면에서 고른 조건을 적용한다. 탐색 화면이 곧바로 이 조건으로 바뀐다.
    fun onApply() = FilterStore.apply(_state.value.draft)

    private fun change(transform: (MeetingFilter) -> MeetingFilter) =
        _state.update { it.copy(draft = transform(it.draft)) }
}
