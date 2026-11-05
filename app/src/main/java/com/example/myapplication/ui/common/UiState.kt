package com.example.myapplication.ui.common

// 화면 데이터의 로딩/성공/에러. 빈 목록은 Success(emptyList())로 표현한다.
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val type: ErrorType = ErrorType.SERVER) : UiState<Nothing>
}

// 16f 네트워크 없음, 16g 서버 오류
enum class ErrorType { NETWORK, SERVER }
