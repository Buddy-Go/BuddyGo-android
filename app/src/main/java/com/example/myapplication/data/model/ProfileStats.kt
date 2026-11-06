package com.example.myapplication.data.model

// 프로필(마이페이지 14, 메이트 프로필 15)에 보이는 숫자와 후기. 매너온도는 User에 있다.
data class TagCount(val tag: String, val count: Int)

data class ProfileStats(
    val receivedReviewCount: Int = 0,
    val hostedMeetingCount: Int = 0,
    val topTags: List<TagCount> = emptyList(), // 많이 받은 순
)

// 내가 받은 후기 한 건. 한마디와 태그는 공개된다. (service-rules 11장)
data class ReceivedReview(
    val id: String = "",
    val reviewerNickname: String = "",
    val sport: Sport = Sport.HEALTH,
    val verdict: ReviewVerdict = ReviewVerdict.GOOD,
    val comment: String? = null,
    val tags: List<String> = emptyList(),
    val createdAtMillis: Long = 0L,
)
