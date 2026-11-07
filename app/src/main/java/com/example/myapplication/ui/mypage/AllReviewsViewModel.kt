package com.example.myapplication.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.MateRepository
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.UiState
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 받은 후기 전체. 내 후기는 UserRepository에서, 메이트의 후기는 메이트 프로필에서 받아 같은 한 줄 모델로 바꾼다.
class AllReviewsViewModel(
    private val mateUserId: String? = null,   // 없으면 내 후기
    private val users: UserRepository = Repositories.users,
    private val mates: MateRepository? = null,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<ReviewRow>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ReviewRow>>> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val result: Result<List<ReviewRow>> = if (mateUserId == null) {
                users.getCurrentUser().mapCatching { me -> users.getReceivedReviews(me.id).getOrThrow().map { it.toRow() } }
            } else {
                (mates ?: return@launch).getProfile(mateUserId).map { p -> p.recentReviews.map { it.toRow() } }
            }
            _state.value = result.fold(
                onSuccess = { UiState.Success(it.newestFirst()) },
                onFailure = { UiState.Error(if (it is IOException) ErrorType.NETWORK else ErrorType.SERVER) },
            )
        }
    }
}
