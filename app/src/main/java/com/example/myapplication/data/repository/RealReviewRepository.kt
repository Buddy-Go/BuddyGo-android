package com.example.myapplication.data.repository

import com.example.myapplication.data.api.BuddyApi
import com.example.myapplication.data.api.IsoTime
import com.example.myapplication.data.api.MeetingApi
import com.example.myapplication.data.api.ReviewApi
import com.example.myapplication.data.api.ReviewSubmitDto
import com.example.myapplication.data.api.apiCall
import com.example.myapplication.data.api.requireData
import com.example.myapplication.data.model.Review
import com.example.myapplication.data.model.ReviewVerdict
import kotlin.coroutines.cancellation.CancellationException

// 서버(BuddyGo-server)와 연결한 메이트 평가 저장소.
//  - 평가 화면의 태그 문구(8개)는 서버 태그(18개) 중 가장 가까운 것으로 바꿔 보낸다. (REVIEW_TAG_CODES)
//  - 평가 대상이 이미 메이트인지·신청했는지는 '함께 운동한 사람' 목록의 관계로 채운다.
class RealReviewRepository(
    private val reviewApi: ReviewApi,
    private val meetingApi: MeetingApi,
    private val buddyApi: BuddyApi,
) : ReviewRepository {

    override suspend fun getReviewSession(meetingId: String): Result<ReviewSession> {
        val id = meetingId.toLongOrNull() ?: return Result.failure(NoSuchElementException(meetingId))
        return apiCall {
            val targets = reviewApi.targets(id).requireData()
            val detail = meetingApi.detail(id).requireData()
            val relations = try {
                buddyApi.workoutPartners(null).requireData().associateBy { it.userId }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyMap()
            }
            ReviewSession(
                meetingId = meetingId,
                meetingTitle = detail.title,
                meetingStartAtMillis = IsoTime.parse(detail.startAt),
                placeName = detail.place.name,
                reviewDueAtMillis = IsoTime.parse(targets.deadlineAt),
                targets = targets.targets.map { t ->
                    val relation = relations[t.userId]?.relation
                    ReviewTargetInfo(
                        userId = t.userId.toString(),
                        nickname = t.nickname,
                        isReviewed = t.reviewed,
                        isMate = relation == "BUDDY",
                        isMateRequested = relation == "REQUEST_SENT",
                    )
                },
            )
        }
    }

    // 서버가 판정한 평가 대기 모임(체크인했고 평가할 사람이 남은 모임, 평가 기한 안)
    override suspend fun getPendingReviews(): Result<List<PendingReview>> = apiCall {
        reviewApi.pending().requireData().gatherings.map {
            PendingReview(it.gatheringId.toString(), it.title, IsoTime.parse(it.endAt), IsoTime.parse(it.deadlineAt), it.remainingCount.toInt())
        }
    }

    override suspend fun getReviewTags(): Result<List<ReviewTagOption>> = apiCall {
        val t = reviewApi.tags().requireData()
        t.positive.map { ReviewTagOption(it.code, it.label, true) } + t.negative.map { ReviewTagOption(it.code, it.label, false) }
    }

    override suspend fun submitReview(review: Review): Result<Unit> {
        val g = review.meetingId.toLongOrNull() ?: return Result.failure(NoSuchElementException(review.meetingId))
        val target = review.targetUserId.toLongOrNull() ?: return Result.failure(NoSuchElementException(review.targetUserId))
        val tagCodes = review.tags.mapNotNull { REVIEW_TAG_CODES[it] ?: serverCodeOf(it) }.distinct()
        val request = ReviewSubmitDto(
            targetId = target,
            rating = when (review.verdict) {
                ReviewVerdict.GOOD -> "GOOD"
                ReviewVerdict.NORMAL -> "NORMAL"
                ReviewVerdict.BAD -> "BAD"
            },
            tags = tagCodes,
            comment = review.comment?.trim()?.takeIf { it.isNotEmpty() },
        )
        return apiCall { reviewApi.submit(g, request) }.map { }
    }

    // 평가 화면의 문구가 아니라 서버 문구 그대로 온 태그면 서버 목록에서 코드를 찾는다.
    private var serverTags: Map<String, String>? = null

    private suspend fun serverCodeOf(label: String): String? {
        val map = serverTags ?: getReviewTags().getOrNull()?.associate { it.label to it.code }?.also { serverTags = it }
        return map?.get(label)
    }

    companion object {
        // 평가 화면의 태그 문구(ReviewTag.label) → 서버 태그 코드 (서버 ReviewTagType 18개)
        val REVIEW_TAG_CODES = mapOf(
            "시간 약속을 잘 지켜요" to "PUNCTUAL",
            "매너가 좋아요" to "GOOD_MANNERS",
            "친절하고 말투가 편안해요" to "KIND",
            "운동 분위기를 즐겁게 만들어줘요" to "FUN_ATMOSPHERE",
            "자세·운동법을 잘 알려줘요" to "GOOD_COACHING",
            "운동 수준이 비슷해서 잘 맞았어요" to "SIMILAR_LEVEL",
            "기구를 배려하며 같이 써요" to "SHARES_EQUIPMENT",
            "보조를 잘 해줘요" to "GOOD_SPOTTER",
            "소통이 잘 돼요" to "GOOD_COMMUNICATION",
            "또 같이 운동하고 싶어요" to "WANT_AGAIN",
            "시간 약속을 안 지켜요 (지각 포함)" to "NOT_PUNCTUAL",
            "운동 중 휴대폰만 봐요" to "ON_PHONE",
            "말투가 불편했어요" to "UNCOMFORTABLE_TONE",
            "운동 수준이 기준과 많이 달랐어요" to "LEVEL_MISMATCH",
            "기구를 독점하거나 정리를 안 해요" to "HOGS_EQUIPMENT",
            "운동 방식을 강요하거나 훈수를 둬요" to "UNWANTED_ADVICE",
            "소통이 잘 안 돼요" to "POOR_COMMUNICATION",
            "모임 설명과 달랐어요" to "NOT_AS_DESCRIBED",
        )
    }
}
