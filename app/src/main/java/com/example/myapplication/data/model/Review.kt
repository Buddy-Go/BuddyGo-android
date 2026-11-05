package com.example.myapplication.data.model

enum class ReviewVerdict { GOOD, NORMAL, BAD }

// 평가는 제출 후 수정·삭제할 수 없다. 태그 문구 목록은 개발하며 정한다.
data class Review(
    val id: String = "",
    val meetingId: String = "",
    val reviewerId: String = "",
    val targetUserId: String = "",
    val verdict: ReviewVerdict = ReviewVerdict.GOOD,
    val tags: List<String> = emptyList(),
    val comment: String? = null,
    val createdAtMillis: Long = 0L,
)
