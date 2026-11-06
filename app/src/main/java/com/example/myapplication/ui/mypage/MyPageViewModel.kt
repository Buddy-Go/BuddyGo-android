package com.example.myapplication.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.ProfileStats
import com.example.myapplication.data.model.ReceivedReview
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageData(
    val user: User,
    val stats: ProfileStats,
    val reviews: List<ReceivedReview>,
    val notificationEnabled: Boolean,
)

class MyPageViewModel(
    private val repository: UserRepository = Repositories.users,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<MyPageData>>(UiState.Loading)
    val state: StateFlow<UiState<MyPageData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = runCatching {
                val user = repository.getCurrentUser().getOrThrow()
                MyPageData(
                    user = user,
                    stats = repository.getProfileStats(user.id).getOrThrow(),
                    reviews = repository.getReceivedReviews(user.id).getOrThrow(),
                    notificationEnabled = repository.isNotificationEnabled().getOrThrow(),
                )
            }.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(if (it is IOException) ErrorType.NETWORK else ErrorType.SERVER) },
            )
        }
    }

    // 알림 설정 줄을 누르면 켜고 끈다. 화면에는 바로 반영하고, 저장에 실패하면 되돌린다.
    fun onToggleNotification() {
        val current = (_state.value as? UiState.Success)?.data ?: return
        val next = !current.notificationEnabled
        setEnabled(next)
        viewModelScope.launch {
            if (repository.setNotificationEnabled(next).isFailure) setEnabled(current.notificationEnabled)
        }
    }

    private fun setEnabled(value: Boolean) {
        _state.update { s -> if (s is UiState.Success) UiState.Success(s.data.copy(notificationEnabled = value)) else s }
    }

    fun onLogout(onDone: () -> Unit) {
        viewModelScope.launch {
            if (repository.logout().isSuccess) onDone()
        }
    }
}
