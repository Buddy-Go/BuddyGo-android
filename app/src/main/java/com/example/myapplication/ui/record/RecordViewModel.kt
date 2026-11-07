package com.example.myapplication.ui.record

import androidx.lifecycle.ViewModel
import com.example.myapplication.ui.explore.filter.snapWeight
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.launch
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.SignUpDraftStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// users가 있으면 '프로필 기록 수정'이다: 내 기록을 불러와 채우고 저장하면 프로필에 저장한다. (모임 신청·상세의 '기록 추가'·'기준 줄')
// 없으면 회원가입 중 입력이라 가입 입력값 저장소에 넣는다.
class RecordViewModel(sport: Sport = Sport.HEALTH, private val users: UserRepository? = null) : ViewModel() {
    // 저장해 둔 기록이 있으면 그 값으로 시작한다. (수정으로 들어온 경우)
    private val _state = MutableStateFlow(RecordState.from(SignUpDraftStore.draft.value.record.takeIf { users == null } ?: ExerciseRecord(), sport))
    val state: StateFlow<RecordState> = _state.asStateFlow()

    // 프로필 수정이면 저장된 내 기록으로 채운다. 아직 아무것도 손대지 않았을 때만 덮어쓴다.
    private var edited = false
    private var loadedRecord: ExerciseRecord = ExerciseRecord()

    init {
        if (users != null) {
            viewModelScope.launch {
                users.getCurrentUser().onSuccess { me ->
                    loadedRecord = me.record
                    if (!edited) _state.value = RecordState.from(me.record, sport)
                }
            }
        }
    }

    fun onTogglePart(part: BodyPart) = change {
        it.copy(parts = if (part in it.parts) it.parts - part else it.parts + part)
    }

    // delta는 −1 또는 +1. 한 번에 바뀌는 크기와 범위는 여기서 정한다.
    fun onStepBench(delta: Int) = change { it.copy(benchPressKg = stepWeight(it.benchPressKg, delta)) }
    fun onStepSquat(delta: Int) = change { it.copy(squatKg = stepWeight(it.squatKg, delta)) }
    fun onStepOverhead(delta: Int) = change { it.copy(overheadPressKg = stepWeight(it.overheadPressKg, delta)) }
    fun onStepPullUp(delta: Int) = change { it.copy(pullUpCount = (it.pullUpCount + delta).coerceIn(0, RecordState.MAX_PULL_UP)) }

    fun onSelectArm(level: SkillLevel) = change { it.copy(armLevel = level) }

    fun onSelectPace(sec: Int) = change { it.copy(runningPaceSec = sec) }

    fun onSelectHiking(style: HikingStyle) = change { it.copy(hikingStyle = style) }

    // 저장: 프로필 수정이면 내 기록에 저장하고, 가입 중이면 가입 입력값 저장소에 넣는다. (다른 종목 기록은 그대로)
    fun onSave() {
        val s = _state.value
        if (users == null) {
            SignUpDraftStore.update { it.copy(record = s.toRecord(it.record)) }
            _state.update { it.copy(saved = true) }
            return
        }
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            // 다른 종목 기록은 그대로 두고 이 종목만 바꾼다. 저장하지 못하면 화면에 남아 다시 시도할 수 있다.
            val record = users.getCurrentUser().getOrNull()?.record ?: loadedRecord
            users.updateRecord(s.toRecord(record))
                .onSuccess { _state.update { it.copy(saving = false, saved = true) } }
                .onFailure { _state.update { it.copy(saving = false, saveFailed = true) } }
        }
    }

    fun onSaveFailureShown() = _state.update { it.copy(saveFailed = false) }

    // 입력을 손댔다고 표시하고 값을 바꾼다. 프로필 기록이 늦게 도착해도 입력을 덮어쓰지 않는다.
    private fun change(transform: (RecordState) -> RecordState) {
        edited = true
        _state.update(transform)
    }

    // 5kg 눈금에서만 움직인다. 눈금에 안 맞는 옛 값은 가장 가까운 눈금으로 맞춘 뒤 한 칸 움직인다.
    private fun stepWeight(current: Int, delta: Int) =
        (snapWeight(current) + delta * RecordState.WEIGHT_STEP).coerceIn(0, RecordState.MAX_WEIGHT)
}
