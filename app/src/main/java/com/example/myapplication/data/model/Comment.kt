package com.example.myapplication.data.model

// 공지 댓글. 답글은 원댓글에만 한 단계로 단다(parentId가 있으면 답글).
data class Comment(
    val id: String = "",
    val noticeId: String = "",
    val authorId: String = "",
    val authorNickname: String = "",
    val body: String = "",
    val createdAtMillis: Long = 0L,
    val isAuthorHost: Boolean = false, // 호스트 배지·초록 이니셜 (8f) — 공지 화면(문진곤)에서 추가
    val parentId: String? = null, // 답글이면 원댓글 id
    val isDeleted: Boolean = false, // 답글이 남아 있어 '삭제된 댓글입니다'로만 남은 원댓글
) {
    val isReply: Boolean get() = parentId != null
}
