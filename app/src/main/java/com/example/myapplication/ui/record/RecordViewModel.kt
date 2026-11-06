package com.example.myapplication.ui.record

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.SignUpDraftStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RecordViewModel(sport: Sport = Sport.HEALTH) : ViewModel() {
    // 저장해 둔 기록이 있으면 그 값으로 시작한다. (수정으로 들어온 경우)
    private val _state = MutableStateFlow(RecordState.from(SignUpDraftStore.draft.value.record, sport))
    val state: StateFlow<RecordState> = _state.asStateFlow()

    fun onTogglePart(part: BodyPart) = _state.update {
        it.copy(parts = if (part in it.parts) it.parts - part else it.parts + part)
    }

    // delta는 −1 또는 +1. 한 번에 바뀌는 크기와 범위는 여기서 정한다.
    fun onStepBench(delta: Int) = _state.update { it.copy(benchPressKg = stepWeight(it.benchPressKg, delta)) }
    fun onStepSquat(delta: Int) = _state.update { it.copy(squatKg = stepWeight(it.squatKg, delta)) }
    fun onStepOverhead(delta: Int) = _state.update { it.copy(overheadPressKg = stepWeight(it.overheadPressKg, delta)) }
    fun onStepPullUp(delta: Int) = _state.update { it.copy(pullUpCount = (it.pullUpCount + delta).coerceIn(0, RecordState.MAX_PULL_UP)) }

    fun onSelectArm(level: SkillLevel) = _state.update { it.copy(armLevel = level) }

    fun onSelectPace(sec: Int) = _state.update { it.copy(runningPaceSec = sec) }

    fun onSelectHiking(style: HikingStyle) = _state.update { it.copy(hikingStyle = style) }

    // 저장: 입력값을 회원가입 입력값 저장소에 넣는다. (다른 종목 기록은 그대로)
    fun onSave() {
        val s = _state.value
        SignUpDraftStore.update { it.copy(record = s.toRecord(it.record)) }
        _state.update { it.copy(saved = true) }
    }

    private fun stepWeight(current: Int, delta: Int) =
        (current + delta * RecordState.WEIGHT_STEP).coerceIn(0, RecordState.MAX_WEIGHT)
}
