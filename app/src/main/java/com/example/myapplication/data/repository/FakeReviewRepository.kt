package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.Review

// 목데이터로 응답한다. 앱이 켜져 있는 동안만 메모리에 남는다.
// 평가 대상은 MockUsers의 서연(u2)·지훈(u3)·유나(u4). 지훈은 이미 평가했고 메이트인 상태로 시작한다.
class FakeReviewRepository : ReviewRepository {

    override suspend fun getReviewSession(meetingId: String): Result<ReviewSession> {
        val base = sessions.getOrPut(meetingId) { sampleSession(meetingId) }
        return Result.success(
            base.copy(targets = base.targets.map { it.copy(isReviewed = it.isReviewed || (meetingId to it.userId) in reviewed) }),
        )
    }

    override suspend fun submitReview(review: Review): Result<Unit> {
        // 정의서: 제출한 평가는 수정·삭제할 수 없다
        val key = review.meetingId to review.targetUserId
        if (key in reviewed) return Result.failure(IllegalStateException("이미 평가했어요"))
        reviewed += key
        return Result.success(Unit)
    }

    companion object {
        private val sessions = mutableMapOf<String, ReviewSession>()
        private val reviewed = mutableSetOf<Pair<String, String>>()
        private const val DAY = 86_400_000L

        private fun sampleSession(meetingId: String): ReviewSession {
            val now = System.currentTimeMillis()
            return ReviewSession(
                meetingId = meetingId,
                meetingTitle = "등·이두 같이 치실 분",
                meetingStartAtMillis = now - 2 * DAY,
                placeName = "에이블짐 홍대점",
                reviewDueAtMillis = now + 5 * DAY,
                targets = listOf(
                    ReviewTargetInfo(MockUsers.noRecord.id, MockUsers.noRecord.nickname, isReviewed = false, isMate = false, isMateRequested = false),
                    ReviewTargetInfo(MockUsers.hostA.id, MockUsers.hostA.nickname, isReviewed = true, isMate = true, isMateRequested = false),
                    ReviewTargetInfo(MockUsers.hostB.id, MockUsers.hostB.nickname, isReviewed = false, isMate = false, isMateRequested = true),
                ),
            )
        }
    }
}
