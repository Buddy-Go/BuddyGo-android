package com.example.myapplication.ui.meeting.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class MeetingCreateViewModel(
    private val userRepository: UserRepository,
    private val meetingRepository: MeetingRepository,
    private val editMeetingId: String? = null, // 있으면 새로 만들지 않고 이 모임을 고친다 (service-rules 4장)
    private val nowMillis: () -> Long = System::currentTimeMillis,
    // 장소 검색(10g) → 장소 확인(10g-3)에서 고른 장소를 받는다. 테스트에서는 비워 둔다.
    private val placeStore: PlaceSelectionStore = PlaceSelectionStore,
) : ViewModel() {

    var state by mutableStateOf(MeetingCreateState(dateOptions = nextDays(), selectedDateIndex = 0, editMeetingId = editMeetingId))
        private set

    init {
        // 장소 확인에서 '이 장소로 설정'하면 장소를 넣고 비운다. 수정 모드에서는 장소를 바꾸지 않는다.
        viewModelScope.launch {
            placeStore.confirmed.collect { picked ->
                if (picked != null && !state.isEditMode && !state.lockedCore) {
                    state = state.copy(place = picked)
                    placeStore.consumeConfirmed()
                }
            }
        }
        viewModelScope.launch {
            val myRecord = userRepository.getCurrentUser().getOrNull()?.record
            state = state.copy(hostGender = userRepository.getCurrentUser().getOrNull()?.gender)
            if (editMeetingId == null) {
                // '내 기록 표시'에 쓸 내 프로필 기록 (10d). 실패하면 기록 없음으로 둔다.
                if (myRecord != null) state = state.copy(healthCriteria = state.healthCriteria.copy(myRecord = myRecord))
            } else {
                loadForEdit(editMeetingId, myRecord ?: com.example.myapplication.data.model.ExerciseRecord())
            }
        }
    }

    // 수정 모드: 고칠 모임을 불러와 화면 값을 채운다. 못 불러오면 안내하고 빈 화면이 되지 않게 닫는다.
    private suspend fun loadForEdit(id: String, myRecord: com.example.myapplication.data.model.ExerciseRecord) {
        meetingRepository.getMeeting(id)
            .onSuccess { state = editStateOf(it, myRecord, nowMillis()) }
            .onFailure { state = state.copy(errorMessage = "모임을 불러오지 못했어요", updated = true) }
    }

    fun onErrorShown() {
        state = state.copy(errorMessage = null)
    }

    fun onTitleChange(title: String) {
        state = state.copy(title = title)
    }

    fun onDateSelect(index: Int) {
        if (state.lockedCore) return // 확정 참가자가 있으면 일시는 잠긴다
        if (index !in state.dateOptions.indices) return
        state = state.copy(selectedDateIndex = index)
    }

    // 달력: 5일 칩 밖의 날짜를 고른다. 디자인이 없어 Material 기본 달력을 쓴다. 오늘 이전 날짜는 고를 수 없다.
    fun onCalendarClick() {
        if (state.lockedCore) return
        state = state.copy(showCalendar = true)
    }

    fun onCalendarDismiss() {
        state = state.copy(showCalendar = false)
    }

    fun onCalendarPick(year: Int, month: Int, day: Int) {
        val (options, index) = withDate(state.dateOptions, dateOptionOf(year, month, day, nowMillis()))
        state = state.copy(dateOptions = options, selectedDateIndex = index, showCalendar = false)
    }

    fun onTimeClick() {
        if (state.lockedCore) return
        state = state.copy(openSheet = CreateSheet.TIME)
    }

    fun onCriteriaClick() {
        if (state.lockedCore) return
        state = state.copy(openSheet = CreateSheet.HEALTH_CRITERIA)
    }

    fun onCapacityClick() {
        if (state.lockedCore) return
        state = state.copy(openSheet = CreateSheet.CAPACITY)
    }

    // 정원·성별 적용. 호스트와 다른 성별 제한은 받지 않는다. (service-rules 4장 가정)
    fun onCapacityApply(capacity: Int, gender: GenderLimit) {
        if (capacity !in HEALTH_CAPACITY_RANGE || gender !in allowedGenderLimits(state.hostGender)) return
        state = state.copy(capacity = capacity, gender = gender, openSheet = null)
    }

    // 수정 모드에서 장소 행을 누르면 상세주소만 고친다. (장소 자체를 바꾸는 화면 10g는 아직 없다)
    fun onDetailAddressClick() {
        state = state.copy(openSheet = CreateSheet.DETAIL_ADDRESS)
    }

    fun onDetailAddressApply(detail: String) {
        val current = state.place ?: return
        state = state.copy(
            place = current.copy(detail = detail.trim().take(DETAIL_ADDRESS_MAX_LENGTH).ifEmpty { null }),
            openSheet = null,
        )
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
        if (state.isSubmitting) return
        if (state.isEditMode) return submitEdit()
        val meeting = state.toMeeting() ?: return
        state = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            meetingRepository.createMeeting(meeting)
                .onSuccess { id -> state = state.copy(isSubmitting = false, createdMeetingId = id) }
                // TODO(임시): 실패 안내는 공용 토스트(16j)가 생기면 그걸로 보여준다
                .onFailure { e -> state = state.copy(isSubmitting = false, errorMessage = e.message ?: "모임을 만들지 못했어요") }
        }
    }

    // 수정 완료: 서버가 규칙(진행중 이후 불가, 90분 규칙 등)을 판정해 어기면 안내한다.
    private fun submitEdit() {
        val id = state.editMeetingId ?: return
        val meeting = state.toUpdatedMeeting() ?: return
        state = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            meetingRepository.updateMeeting(id, meeting)
                .onSuccess { state = state.copy(isSubmitting = false, updated = true) }
                .onFailure { e -> state = state.copy(isSubmitting = false, errorMessage = e.message ?: "모임을 수정하지 못했어요") }
        }
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
