package com.example.myapplication.data.model

// 공지 댓글. 답글(대댓글)은 없다.
data class Comment(
    val id: String = "",
    val noticeId: String = "",
    val authorId: String = "",
    val authorNickname: String = "",
    val body: String = "",
    val createdAtMillis: Long = 0L,
    val isAuthorHost: Boolean = false, // 호스트 배지·초록 이니셜 (8f) — 공지 화면(문진곤)에서 추가
)
