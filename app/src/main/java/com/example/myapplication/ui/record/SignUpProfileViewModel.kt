package com.example.myapplication.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.SignUpDraftStore
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpProfileViewModel(
    private val repository: UserRepository = FakeUserRepository(),
) : ViewModel() {
    private data class Submit(val submitting: Boolean = false, val error: Boolean = false, val completed: Boolean = false)

    private val submit = MutableStateFlow(Submit())

    // 선택·기록은 입력값 저장소가 기준이다. 기록 화면에서 돌아오면 자동으로 갱신된다.
    val state: StateFlow<SignUpProfileState> = combine(SignUpDraftStore.draft, submit) { draft, s ->
        SignUpProfileState(
            sports = draft.preferredSports.toSet(),
            record = draft.record,
            submitting = s.submitting,
            error = s.error,
            completed = s.completed,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SignUpProfileState())

    fun onToggleSport(sport: Sport) {
        SignUpDraftStore.update { draft ->
            if (sport in draft.preferredSports) {
                draft.copy(
                    preferredSports = draft.preferredSports - sport,
                    record = draft.record.clearedFor(sport), // 해제하면 그 종목 기록도 지운다
                )
            } else {
                draft.copy(preferredSports = draft.preferredSports + sport)
            }
        }
    }

    // 가입 완료. skip = true(건너뛰기)이면 종목과 기록을 비운 채로 가입한다.
    fun onSubmit(skip: Boolean) {
        if (submit.value.submitting) return
        if (skip) SignUpDraftStore.update { it.copy(preferredSports = emptyList(), record = it.record.clearedFor(Sport.HEALTH).clearedFor(Sport.RUNNING).clearedFor(Sport.HIKING)) }
        submit.update { Submit(submitting = true) }
        viewModelScope.launch {
            repository.signUp(SignUpDraftStore.draft.value).fold(
                onSuccess = {
                    SignUpDraftStore.clear()
                    submit.update { Submit(completed = true) }
                },
                onFailure = { submit.update { Submit(error = true) } },
            )
        }
    }
}
