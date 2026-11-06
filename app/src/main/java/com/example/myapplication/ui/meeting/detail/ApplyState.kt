package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.common.UiState

// 6a 기준 일치, 6b 기준 미설정은 내 기록 요약이 있는지로 갈린다.
data class ApplyState(
    val meeting: UiState<Meeting> = UiState.Loading,
    val me: User? = null,
    val message: String = "",
    val submitting: Boolean = false,
    val failed: Boolean = false,        // 제출 실패 띠 (16i)
    val confirmEmpty: Boolean = false,  // "한마디 없이 신청할까요?" 확인창
    val done: Boolean = false,          // 신청 완료. 화면이 이를 보고 닫는다.
) {
    val loaded: Meeting? get() = (meeting as? UiState.Success)?.data

    // 호스트에게 전달되는 이 종목의 내 기록. 없으면 null(미설정)
    val recordSummary: String? get() = loaded?.let { myRecordSummary(it, me?.record) }

    val messageLengthText: String get() = "${message.length} / $MAX_MESSAGE_LENGTH"
}
