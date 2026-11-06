package com.example.myapplication.ui.explore.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FilterViewModel(
    // TODO(임시) 서버 연결 전까지 Fake를 쓴다.
    private val repository: MeetingRepository = FakeMeetingRepository(),
    private val userRepository: UserRepository = FakeUserRepository(),
    nowMillis: Long = System.currentTimeMillis(),
) : ViewModel() {
    // 지금 적용된 조건에서 시작한다.
    private val _state = MutableStateFlow(FilterState(draft = FilterStore.applied.value, nowMillis = nowMillis))
    val state: StateFlow<FilterState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMeetings().onSuccess { list -> _state.update { it.copy(meetings = list) } }
        }
        viewModelScope.launch {
            userRepository.getCurrentUser().onSuccess { user -> _state.update { it.copy(myRecord = user.record) } }
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

    // 달력(날짜 선택 창)에서 날짜를 고르면 그 날이 조건이 되고 창이 닫힌다.
    // 선택 창이 돌려주는 값은 UTC 그날 0시라서 우리 시간대의 그날 0시로 바꿔 넣는다.
    fun onPickDate(utcMidnightMillis: Long) {
        change { it.copy(dateMillis = localStartOfDay(utcMidnightMillis)) }
        onApplySheet()
    }

    fun onClearTimeRange() = change { it.withCustomRange(null) }

    fun onReset() = change { MeetingFilter() }

    fun onOpenSheet(sheet: FilterSheet) = _state.update { it.withSheetOpened(sheet) }
    fun onApplySheet() = _state.update { it.withSheetApplied() }
    fun onDismissSheet() = _state.update { it.withSheetDismissed() }

    // ---- 기준 시트 (4d) ----

    fun onTogglePart(part: BodyPart) = change { it.copy(health = it.health.withPartToggled(part)) }

    // 종목에 맞는 기준의 스위치를 켜고 끈다. 처음 켜면 내 기록으로 초기값이 채워진다.
    fun onSetCriteriaOn(on: Boolean) {
        val record = _state.value.myRecord
        change {
            when (it.sport) {
                Sport.HEALTH -> it.copy(health = it.health.withCriteriaOn(on, record))
                Sport.RUNNING -> it.copy(running = it.running.withCriteriaOn(on, record))
                Sport.HIKING -> it.copy(hiking = it.hiking.withCriteriaOn(on, record))
                null -> it
            }
        }
    }

    // '내 기준으로 설정': 직접 바꾼 값을 내 기록 기준으로 되돌린다.
    fun onResetToMine() {
        val record = _state.value.myRecord
        change { it.copy(health = it.health.withMyCriteria(record)) }
    }

    fun onSetRange(part: BodyPart, range: IntRange) = change { it.copy(health = it.health.withRange(part, range)) }
    fun onSetArmLevel(level: SkillLevel) = change { it.copy(health = it.health.withArmLevel(level)) }
    fun onSetPaceRange(range: IntRange) = change { it.copy(running = it.running.withPaceRange(range)) }
    fun onSetHikingStyle(style: HikingStyle) = change { it.copy(hiking = it.hiking.withStyle(style)) }

    // 위치 검색(4g)에서 돌아오면 고른 구·동을 조건에 넣는다. null이면 위치 조건을 푼다.
    fun onSetDistrict(district: String?) = change { it.copy(district = district?.takeIf { name -> name.isNotBlank() }) }

    // ---- 시간 범위 시트 (4f) ----

    fun onSetTimeStart(minute: Int) = change { f -> f.withCustomRange((f.customRange ?: TimeOfDayRange.DEFAULT).withStart(minute)) }
    fun onSetTimeEnd(minute: Int) = change { f -> f.withCustomRange((f.customRange ?: TimeOfDayRange.DEFAULT).withEnd(minute)) }

    // ---- 상세 조건 시트 (4e) ----

    fun onSetGender(gender: GenderLimit) = change { it.withGender(gender) }
    fun onSetMannerOn(on: Boolean) = change { it.withMannerOn(on) }
    fun onSetMinManner(degrees: Int) = change { it.withMinManner(degrees) }

    // 화면에서 고른 조건을 적용한다. 탐색 화면이 곧바로 이 조건으로 바뀐다.
    fun onApply() = FilterStore.apply(_state.value.draft)

    private fun change(transform: (MeetingFilter) -> MeetingFilter) =
        _state.update { it.copy(draft = transform(it.draft)) }
}
