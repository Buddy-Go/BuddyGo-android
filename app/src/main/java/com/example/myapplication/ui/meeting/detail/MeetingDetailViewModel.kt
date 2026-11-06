package com.example.myapplication.ui.meeting.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.CheckInOutOfRangeException
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.distanceMeters
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.NoticeRepository
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.GeoPoint
import com.example.myapplication.ui.explore.LastKnownLocation
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MeetingDetailViewModel(
    private val meetingId: String,
    private val repository: MeetingRepository = Repositories.meetings,
    private val userRepository: UserRepository = Repositories.users,
    private val noticeRepository: NoticeRepository = Repositories.notices,
    nowMillis: Long = System.currentTimeMillis(),
) : ViewModel() {
    private val _state = MutableStateFlow(MeetingDetailState(nowMillis = nowMillis, myLocation = LastKnownLocation.point.value))
    val state: StateFlow<MeetingDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(meeting = UiState.Loading) }
        viewModelScope.launch {
            // 내가 누구인지 알아야 역할(호스트·확정·대기)을 판단할 수 있다.
            val me = userRepository.getCurrentUser().getOrNull()
            _state.update { it.copy(myUserId = me?.id.orEmpty(), myGender = me?.gender, myRecord = me?.record) }
            val result = repository.getMeeting(meetingId)
            result.onSuccess { loadHostSection(it) }
            _state.update {
                it.copy(
                    meeting = result.fold(
                        onSuccess = { meeting -> UiState.Success(meeting) },
                        onFailure = { e -> UiState.Error(if (e is IOException) ErrorType.NETWORK else ErrorType.SERVER) },
                    ),
                )
            }
        }
    }

    // 찜: 먼저 화면에 반영하고 저장한다. 실패하면 되돌리고 안내한다.
    fun onToggleFavorite() {
        val before = _state.value.loaded?.isFavorite ?: return
        _state.update { it.withFavorite(!before) }
        viewModelScope.launch {
            repository.setFavorite(meetingId, !before).onFailure {
                _state.update { state -> state.withFavorite(before).copy(message = "찜을 저장하지 못했어요. 다시 시도해주세요.") }
            }
        }
    }

    // 신청 취소: 대기 중에는 언제든 취소할 수 있고 매너온도에 영향이 없다. (service-rules 5장)
    fun onCancelApplication() {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.cancelApplication(meetingId)
                .onSuccess { reloadMeeting() }
                .onFailure { _state.update { s -> s.copy(message = "신청을 취소하지 못했어요. 다시 시도해주세요.") } }
            _state.update { it.copy(submitting = false) }
        }
    }

    // 신청 화면에서 신청을 마치고 돌아왔다: 화면은 그대로 두고 모임 정보만 새로 받는다.
    fun onApplied() {
        viewModelScope.launch { reloadMeeting() }
    }

    // 안내를 보여 준 뒤 지운다.
    fun onMessageShown() = _state.update { it.copy(message = null) }
    fun onNoticeShown() = _state.update { it.copy(notice = null) }

    // ---- 나가기 ----

    // ⋮ 메뉴의 '나가기'를 눌렀다. 나갈 수 없는 사람이면 아무것도 하지 않는다.
    fun onLeaveClick() {
        val plan = _state.value.leavePlan ?: return
        _state.update { it.copy(leaveDialog = plan) }
    }

    fun onLeaveDismiss() = _state.update { it.copy(leaveDialog = null) }

    // 확인창의 '나가기'. 성공하면 내 역할이 없어지도록 모임 정보를 새로 받는다. (취소되는 경우도 서버가 상태를 바꿔 준다)
    fun onLeaveConfirm() {
        if (_state.value.submitting || _state.value.leaveDialog == null) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.leave(meetingId)
                .onSuccess {
                    _state.update { it.copy(leaveDialog = null, notice = "모임에서 나갔어요") }
                    reloadMeeting()
                }
                .onFailure { _state.update { it.copy(leaveDialog = null, message = "나가지 못했어요. 다시 시도해주세요.") } }
            _state.update { it.copy(submitting = false) }
        }
    }

    // ---- 체크인 (9a, 9b, 9c) ----

    // 체크인하기를 눌렀다. 화면이 위치 권한을 확인해 내 위치를 가져오고 onCheckInLocation으로 알려 준다.
    fun onCheckInStart() = _state.update { it.copy(checkIn = CheckInPrompt.Locating) }

    // 내 위치를 받았다. 못 얻었으면(null) 안내하고, 얻었으면 모임 장소와의 거리를 재서 9a/9b를 보여 준다.
    fun onCheckInLocation(point: GeoPoint?) {
        val meeting = _state.value.loaded ?: return
        if (_state.value.checkIn == null) return // 그 사이 사용자가 창을 닫았다
        if (point == null) {
            _state.update { it.copy(checkIn = CheckInPrompt.NoLocation, checkInPoint = null) }
            return
        }
        LastKnownLocation.set(point)
        val meters = distanceMeters(point, meeting.place.latitude, meeting.place.longitude)
        _state.update { it.copy(checkIn = CheckInPrompt.Ready(meters), checkInPoint = point, myLocation = point) }
    }

    // 확인창의 '체크인'. 범위 안일 때만 서버에 보낸다. 서버가 잰 거리가 범위 밖이면 그 거리로 9b를 보여 준다.
    fun onCheckInConfirm() {
        val s = _state.value
        val ready = s.checkIn as? CheckInPrompt.Ready
        val point = s.checkInPoint
        if (ready == null || point == null || !canCheckIn(ready.distanceMeters)) return
        _state.update { it.copy(checkIn = CheckInPrompt.Submitting(ready.distanceMeters)) }
        viewModelScope.launch {
            repository.checkIn(meetingId, point.latitude, point.longitude)
                .onSuccess {
                    _state.update { it.copy(checkIn = null, checkInPoint = null, notice = "체크인했어요") }
                    reloadMeeting()
                }
                .onFailure { e ->
                    val next = if (e is CheckInOutOfRangeException) CheckInPrompt.Ready(e.distanceMeters) else CheckInPrompt.Failed
                    _state.update { it.copy(checkIn = next) }
                }
        }
    }

    fun onCheckInDismiss() = _state.update { it.copy(checkIn = null, checkInPoint = null) }

    fun onOpenSheet(sheet: DetailSheet) = _state.update { it.copy(openSheet = sheet) }
    fun onCloseSheet() = _state.update { it.copy(openSheet = null) }

    // 화면은 그대로 두고 모임 정보만 다시 받는다.
    private suspend fun reloadMeeting() {
        repository.getMeeting(meetingId).onSuccess { meeting ->
            loadHostSection(meeting)
            _state.update { it.copy(meeting = UiState.Success(meeting)) }
        }
    }

    // 다른 화면(공지, 참가자 관리)에서 돌아왔을 때 새로 받는다.
    fun onResume() {
        if (_state.value.loaded == null) return
        viewModelScope.launch { reloadMeeting() }
    }

    // ---- 호스트 영역 (5a-H 계열) ----

    // 내가 호스트일 때만 확정 명단 이름과 마지막 공지를 받아 호스트 영역 값을 만든다.
    private suspend fun loadHostSection(meeting: Meeting) {
        if (meeting.roleOf(_state.value.myUserId) != MeetingRole.HOST) {
            _state.update { it.copy(hostSection = null) }
            return
        }
        val names = meeting.confirmedIds.mapNotNull { id ->
            userRepository.getUser(id).getOrNull()?.let { id to it.nickname }
        }.toMap()
        val latest = noticeRepository.getNotices(meetingId).getOrDefault(emptyList()).maxByOrNull { it.createdAtMillis }
        _state.update { it.copy(hostSection = hostSectionStateOf(meeting, orderedConfirmedNames(meeting, names), latest)) }
    }

    // ⋯ 메뉴 항목. 수정은 화면이 수정 화면으로 이동하므로 여기서는 할 일이 없다.
    fun onHostAction(action: HostAction) {
        when (action) {
            HostAction.EDIT -> Unit
            HostAction.CANCEL -> _state.update { it.copy(hostDialog = HostDialog.CANCEL) }
            HostAction.DELETE -> _state.update { it.copy(hostDialog = HostDialog.DELETE) }
        }
    }

    fun onHostDialogDismiss() = _state.update { it.copy(hostDialog = null) }

    // 5a-X2 취소: 확정 참가자(호스트 제외)가 있을 때. 사유는 필수다.
    fun onCancelMeetingConfirm(reason: String) {
        if (_state.value.submitting || !canSubmitCancelReason(reason)) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.cancelMeeting(meetingId, reason.trim())
                .onSuccess {
                    _state.update { it.copy(hostDialog = null, notice = "모임을 취소했어요") }
                    reloadMeeting()
                }
                .onFailure { _state.update { it.copy(hostDialog = null, message = "모임을 취소하지 못했어요. 다시 시도해주세요.") } }
            _state.update { it.copy(submitting = false) }
        }
    }

    // 5a-X3 삭제: 확정 참가자(호스트 제외)가 없을 때. 성공하면 화면을 닫는다.
    fun onDeleteMeetingConfirm() {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.deleteMeeting(meetingId)
                .onSuccess { _state.update { it.copy(hostDialog = null, deleted = true) } }
                .onFailure { _state.update { it.copy(hostDialog = null, message = "모임을 삭제하지 못했어요. 다시 시도해주세요.") } }
            _state.update { it.copy(submitting = false) }
        }
    }
}
