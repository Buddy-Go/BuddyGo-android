package com.example.myapplication.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.repository.NotificationRepository
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository = Repositories.notifications,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Notification>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Notification>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = repository.getNotifications().fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(if (it is IOException) ErrorType.NETWORK else ErrorType.SERVER) },
            )
        }
    }

    // 알림 하나를 눌렀을 때. 화면에는 바로 읽은 것으로 보여 주고, 서버에는 뒤따라 알린다.
    fun onNotificationClick(notification: Notification) {
        if (notification.isRead) return
        updateLocal { list -> list.map { if (it.id == notification.id) it.copy(isRead = true) else it } }
        viewModelScope.launch { repository.markAsRead(notification.id) }
    }

    // '모두 읽음'
    fun onMarkAllRead() {
        updateLocal { list -> list.map { it.copy(isRead = true) } }
        viewModelScope.launch { repository.markAllAsRead() }
    }

    private fun updateLocal(transform: (List<Notification>) -> List<Notification>) {
        _state.update { s -> if (s is UiState.Success) UiState.Success(transform(s.data)) else s }
    }
}
