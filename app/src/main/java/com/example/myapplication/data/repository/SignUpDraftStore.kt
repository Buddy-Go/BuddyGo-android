package com.example.myapplication.data.repository

import com.example.myapplication.data.model.SignUpDraft

// 회원가입 단계 사이에서 입력값을 넘기는 메모리 저장소. 화면 사이에는 ID만 넘기는 규칙(7번) 때문에
// 단계별 화면이 같은 입력값을 읽고 쓰도록 한 곳에 둔다. 앱을 종료하면 사라진다.
object SignUpDraftStore {
    var draft: SignUpDraft = SignUpDraft()
        private set

    fun update(change: (SignUpDraft) -> SignUpDraft) {
        draft = change(draft)
    }

    fun clear() {
        draft = SignUpDraft()
    }
}
