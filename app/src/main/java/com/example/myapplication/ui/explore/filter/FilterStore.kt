package com.example.myapplication.ui.explore.filter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 탐색 화면이 쓰는 '적용된 조건'. 조건 설정 화면과 탐색 화면의 종목 칩이 같이 쓴다.
// 화면이 바뀌어도 값이 남아야 해서 앱 안에 하나만 둔다. (SignUpDraftStore와 같은 방식)
object FilterStore {
    private val _applied = MutableStateFlow(MeetingFilter())
    val applied: StateFlow<MeetingFilter> = _applied.asStateFlow()

    fun apply(filter: MeetingFilter) {
        _applied.value = filter
    }

    fun update(change: (MeetingFilter) -> MeetingFilter) = _applied.update(change)

    // 테스트와 초기화 용도
    fun clear() {
        _applied.value = MeetingFilter()
    }
}
