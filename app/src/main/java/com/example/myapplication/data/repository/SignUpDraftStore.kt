package com.example.myapplication.data.repository

import com.example.myapplication.data.model.SignUpDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 회원가입 단계 사이에서 입력값을 넘기는 메모리 저장소. 화면 사이에는 ID만 넘기는 규칙(7번) 때문에
// 단계별 화면이 같은 입력값을 읽고 쓰도록 한 곳에 둔다. 앱을 종료하면 사라진다.
// 값이 바뀌면 흘러나오므로(StateFlow) 기록 화면에서 돌아왔을 때 프로필 화면이 자동으로 갱신된다.
object SignUpDraftStore {
    private val _draft = MutableStateFlow(SignUpDraft())
    val draft: StateFlow<SignUpDraft> = _draft.asStateFlow()

    fun update(change: (SignUpDraft) -> SignUpDraft) = _draft.update(change)

    fun clear() {
        _draft.value = SignUpDraft()
    }
}
