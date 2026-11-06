package com.example.myapplication.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.SignUpDraftStore
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpInfoViewModel(
    private val repository: UserRepository = FakeUserRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(SignUpInfoState())
    val state: StateFlow<SignUpInfoState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }

    // 닉네임을 바꾸면 이전 중복 확인 결과는 무효가 된다.
    fun onNicknameChange(value: String) = _state.update { it.copy(nickname = value, nicknameCheck = NicknameCheck.Idle) }

    fun onNicknameCheck() {
        val nickname = _state.value.nickname.trim()
        if (nickname.isEmpty() || _state.value.nicknameCheck == NicknameCheck.Checking) return
        _state.update { it.copy(nicknameCheck = NicknameCheck.Checking) }
        viewModelScope.launch {
            val result = repository.isNicknameAvailable(nickname)
            _state.update { s ->
                // 확인하는 사이에 닉네임이 바뀌었으면 결과를 버린다.
                if (s.nickname.trim() != nickname) s
                else s.copy(
                    nicknameCheck = result.fold(
                        onSuccess = { ok -> if (ok) NicknameCheck.Available else NicknameCheck.Taken },
                        onFailure = { NicknameCheck.Failed },
                    ),
                )
            }
        }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailCheck = EmailCheck.Idle) }

    // 이메일 입력란을 벗어날 때 형식과 중복을 검사한다.
    fun onEmailFocusLost() {
        val email = _state.value.email.trim()
        if (email.isEmpty() || _state.value.emailCheck == EmailCheck.Checking) return
        if (!SignUpInfoState.isValidEmail(email)) {
            _state.update { it.copy(emailCheck = EmailCheck.InvalidFormat) }
            return
        }
        _state.update { it.copy(emailCheck = EmailCheck.Checking) }
        viewModelScope.launch {
            val result = repository.isEmailAvailable(email)
            _state.update { s ->
                if (s.email.trim() != email) s
                else s.copy(
                    emailCheck = result.fold(
                        onSuccess = { ok -> if (ok) EmailCheck.Available else EmailCheck.Taken },
                        onFailure = { EmailCheck.Failed },
                    ),
                )
            }
        }
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value) }
    fun onPasswordFocusLost() = _state.update { it.copy(passwordTouched = true) }
    fun onPasswordConfirmChange(value: String) = _state.update { it.copy(passwordConfirm = value) }
    fun onTogglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun onToggleConfirmVisible() = _state.update { it.copy(confirmVisible = !it.confirmVisible) }
    fun onGenderSelect(gender: Gender) = _state.update { it.copy(gender = gender) }

    // 입력값을 다음 단계로 넘기고, 넘어가도 되면 true
    fun onNext(): Boolean {
        val s = _state.value
        if (!s.canProceed) return false
        SignUpDraftStore.update {
            it.copy(
                name = s.name.trim(),
                nickname = s.nickname.trim(),
                email = s.email.trim(),
                password = s.password,
                gender = s.gender,
            )
        }
        return true
    }
}
