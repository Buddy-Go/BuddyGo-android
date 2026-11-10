package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Review

// 메이트 평가 창구 (12a~12c). 평가 대상·기한·매너온도 반영은 서버가 판정한다 (service-rules 0장, 11장).
// 데이터 계약(docs/data-model.md)대로 모든 호출은 suspend이고 Result<T>를 돌려준다.
interface ReviewRepository {
    suspend fun getReviewSession(meetingId: String): Result<ReviewSession>

    // review.reviewerId·id·createdAtMillis는 서버가 채운다.
    suspend fun submitReview(review: Review): Result<Unit>

    // 평가 대기 목록(활동 탭 '평가 대기'). 서버가 체크인 여부·평가 기한·남은 사람 수를 판정해 준다.
    // 서버 값을 못 받는 저장소(목)는 failure이고, 화면은 모임 목록으로 직접 계산한다.
    suspend fun getPendingReviews(): Result<List<PendingReview>> = Result.failure(UnsupportedOperationException())

    // 서버의 평가 태그 문구 목록 (긍정, 부정)
    suspend fun getReviewTags(): Result<List<ReviewTagOption>> = Result.failure(UnsupportedOperationException())
}

data class PendingReview(
    val meetingId: String,
    val title: String,
    val endedAtMillis: Long,
    val reviewDueAtMillis: Long,
    val remainingCount: Int,
)

data class ReviewTagOption(val code: String, val label: String, val isPositive: Boolean)

// 한 모임의 평가 화면(12a~12c)에 필요한 값
data class ReviewSession(
    val meetingId: String,
    val meetingTitle: String,
    val meetingStartAtMillis: Long,
    val placeName: String,
    val reviewDueAtMillis: Long, // 평가 기한. 서버가 '종료 + 평가 기간'으로 계산
    val targets: List<ReviewTargetInfo>, // 나를 뺀, 체크인한 사람들
)

data class ReviewTargetInfo(
    val userId: String,
    val nickname: String,
    val isReviewed: Boolean, // 내가 이미 평가함
    val isMate: Boolean, // 이미 메이트
    val isMateRequested: Boolean, // 내가 메이트 신청을 보낸 상태
)
