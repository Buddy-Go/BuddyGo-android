package com.example.myapplication.data.model

data class Notice(
    val id: String = "",
    val meetingId: String = "",
    val title: String = "",
    val body: String = "",
    val createdAtMillis: Long = 0L,
    val isEdited: Boolean = false, // 수정하면 '수정됨' 표시
    val commentCount: Int = 0, // 목록(8d)의 댓글 수 — 공지 화면(문진곤)에서 추가
)

// 공지 본문 최대 글자 수. service-rules 9장에서 '200자 - 미정'이라 바뀌면 여기만 고친다.
const val NOTICE_BODY_MAX_LENGTH = 200
