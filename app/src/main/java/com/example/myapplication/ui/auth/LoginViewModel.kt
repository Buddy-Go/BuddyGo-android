package com.example.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.ApiException
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

enum class LoginError { INVALID_CREDENTIALS, TOO_MANY_ATTEMPTS, NETWORK, OTHER }

data class LoginState(
    val submitting: Boolean = false,
    val error: LoginError? = null,
)

class LoginViewModel(private val repository: UserRepository = Repositories.users) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onLogin(email: String, password: String, keepLogin: Boolean, onSuccess: () -> Unit) {
        if (_state.value.submitting) return
        if (email.isBlank() || password.isEmpty()) {
            _state.update { it.copy(error = LoginError.INVALID_CREDENTIALS) }
            return
        }
        _state.update { LoginState(submitting = true) }
        viewModelScope.launch {
            repository.login(email.trim(), password, keepLogin).fold(
                onSuccess = {
                    _state.update { LoginState() }
                    onSuccess()
                },
                onFailure = { e -> _state.update { LoginState(error = e.toLoginError()) } },
            )
        }
    }

    fun onInputChanged() {
        if (_state.value.error != null) _state.update { it.copy(error = null) }
    }
}

internal fun Throwable.toLoginError(): LoginError = when {
    this is ApiException && code == ApiException.INVALID_CREDENTIALS -> LoginError.INVALID_CREDENTIALS
    this is ApiException && code == ApiException.TOO_MANY_LOGIN_ATTEMPTS -> LoginError.TOO_MANY_ATTEMPTS
    this is ApiException && code == ApiException.INVALID_INPUT -> LoginError.INVALID_CREDENTIALS
    this is IOException -> LoginError.NETWORK
    else -> LoginError.OTHER
}

internal fun LoginError.message(): String = when (this) {
    LoginError.INVALID_CREDENTIALS -> "이메일 또는 비밀번호가 올바르지 않아요."
    LoginError.TOO_MANY_ATTEMPTS -> "로그인 시도가 너무 많아요. 잠시 후 다시 시도해주세요."
    LoginError.NETWORK -> "네트워크 연결을 확인하고 다시 시도해주세요."
    LoginError.OTHER -> "로그인하지 못했어요. 다시 시도해주세요."
}
