package com.example.myapplication.data.model

// 공지 댓글. 답글(대댓글)은 없다.
data class Comment(
    val id: String = "",
    val noticeId: String = "",
    val authorId: String = "",
    val authorNickname: String = "",
    val body: String = "",
    val createdAtMillis: Long = 0L,
)
