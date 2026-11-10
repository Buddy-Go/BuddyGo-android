package com.example.myapplication.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// 공지·댓글. 서버 주소에는 모임 id가 들어간다. 모두 토큰이 필요하다.
interface NoticeApi {
    @GET("api/gatherings/{g}/notices")
    suspend fun list(@Path("g") g: Long, @Query("page") page: Int, @Query("size") size: Int): ApiResponse<NoticeListDto>

    @GET("api/gatherings/{g}/notices/{n}")
    suspend fun detail(@Path("g") g: Long, @Path("n") n: Long): ApiResponse<NoticeDto>

    @POST("api/gatherings/{g}/notices")
    suspend fun create(@Path("g") g: Long, @Body request: NoticeCreateDto): ApiResponse<NoticeDto>

    @PATCH("api/gatherings/{g}/notices/{n}")
    suspend fun update(@Path("g") g: Long, @Path("n") n: Long, @Body request: NoticeUpdateDto): ApiResponse<NoticeDto>

    @DELETE("api/gatherings/{g}/notices/{n}")
    suspend fun delete(@Path("g") g: Long, @Path("n") n: Long): ApiResponse<Unit>

    @GET("api/gatherings/{g}/notices/{n}/comments")
    suspend fun comments(@Path("g") g: Long, @Path("n") n: Long, @Query("page") page: Int, @Query("size") size: Int): ApiResponse<CommentListDto>

    @POST("api/gatherings/{g}/notices/{n}/comments")
    suspend fun addComment(@Path("g") g: Long, @Path("n") n: Long, @Body request: CommentCreateDto): ApiResponse<CommentDto>

    @DELETE("api/gatherings/{g}/notices/{n}/comments/{c}")
    suspend fun deleteComment(@Path("g") g: Long, @Path("n") n: Long, @Path("c") c: Long): ApiResponse<Unit>
}

interface NotificationApi {
    @GET("api/notifications")
    suspend fun list(@Query("page") page: Int, @Query("size") size: Int): ApiResponse<NotificationListDto>

    @PATCH("api/notifications/{id}/read")
    suspend fun read(@Path("id") id: Long): ApiResponse<Unit>

    @PATCH("api/notifications/read-all")
    suspend fun readAll(): ApiResponse<Unit>
}

// 버디(앱에서는 '메이트')
interface BuddyApi {
    @GET("api/buddies")
    suspend fun buddies(): ApiResponse<List<BuddyDto>>

    @GET("api/buddies/workout-partners")
    suspend fun workoutPartners(@Query("q") q: String?): ApiResponse<List<WorkoutPartnerDto>>

    @DELETE("api/buddies/{userId}")
    suspend fun remove(@Path("userId") userId: Long): ApiResponse<Unit>

    @PATCH("api/buddies/{userId}/notification")
    suspend fun notification(@Path("userId") userId: Long, @Body request: BuddyNotificationDto): ApiResponse<BuddyNotificationResultDto>

    @GET("api/users/{userId}/activities")
    suspend fun activities(@Path("userId") userId: Long): ApiResponse<List<BuddyActivityCardDto>>

    @POST("api/buddy-requests")
    suspend fun send(@Body request: BuddyRequestCreateDto): ApiResponse<BuddyRequestDto>

    @GET("api/buddy-requests/received")
    suspend fun received(): ApiResponse<List<ReceivedBuddyRequestDto>>

    @POST("api/buddy-requests/{id}/accept")
    suspend fun accept(@Path("id") id: Long): ApiResponse<BuddyRequestDto>

    @POST("api/buddy-requests/{id}/reject")
    suspend fun reject(@Path("id") id: Long): ApiResponse<BuddyRequestDto>

    // 내가 보낸 대기 중인 신청 취소
    @DELETE("api/buddy-requests/{id}")
    suspend fun cancel(@Path("id") id: Long): ApiResponse<BuddyRequestDto>
}

interface ReviewApi {
    // 평가 대기 목록(체크인했고 아직 평가하지 않은 사람이 남은 모임)
    @GET("api/reviews/pending")
    suspend fun pending(): ApiResponse<PendingReviewsDto>

    // 평가 태그 문구 목록
    @GET("api/review-tags")
    suspend fun tags(): ApiResponse<ReviewTagsDto>

    @GET("api/gatherings/{g}/review-targets")
    suspend fun targets(@Path("g") g: Long): ApiResponse<ReviewTargetsDto>

    @POST("api/gatherings/{g}/reviews")
    suspend fun submit(@Path("g") g: Long, @Body request: ReviewSubmitDto): ApiResponse<ReviewResultDto>

    @GET("api/users/me/reviews")
    suspend fun myReviews(@Query("cursor") cursor: String?, @Query("size") size: Int): ApiResponse<ReceivedReviewsDto>

    @GET("api/users/{userId}/reviews")
    suspend fun userReviews(@Path("userId") userId: Long, @Query("cursor") cursor: String?, @Query("size") size: Int): ApiResponse<ReceivedReviewsDto>
}

// ---- 공지·댓글 ----

@Serializable
data class NoticeDto(
    val noticeId: Long,
    val gatheringId: Long,
    val authorId: Long,
    val authorNickname: String = "",
    val title: String = "",
    val content: String = "",
    val createdAt: String,
    val editedAt: String? = null,
    val edited: Boolean = false,
    val commentCount: Int? = null, // 서버가 내려주기 전 버전과도 같이 쓰도록 비어 있을 수 있다
)

@Serializable
data class NoticeListDto(val content: List<NoticeDto> = emptyList(), val hasNext: Boolean = false)

@Serializable
data class CommentDto(
    val commentId: Long,
    val noticeId: Long,
    val authorId: Long,
    val authorNickname: String = "",
    val content: String = "",
    val createdAt: String,
    val authorHost: Boolean? = null, // 댓글 작성자가 모임 호스트인지. 비어 있을 수 있다
)

@Serializable
data class CommentListDto(val content: List<CommentDto> = emptyList(), val hasNext: Boolean = false)

@Serializable
data class NoticeCreateDto(val title: String, val content: String)

@Serializable
data class NoticeUpdateDto(val title: String? = null, val content: String? = null)

@Serializable
data class CommentCreateDto(val content: String)

// ---- 알림 ----

@Serializable
data class NotificationDto(
    val notificationId: Long,
    val type: String = "",
    val message: String = "",
    val createdAt: String,
    val read: Boolean = false,
    val actorId: Long? = null,
    val gatheringId: Long? = null,
    val noticeId: Long? = null,
    val buddyRequestId: Long? = null,
)

@Serializable
data class NotificationListDto(val content: List<NotificationDto> = emptyList(), val hasNext: Boolean = false, val unreadCount: Long = 0)

// ---- 버디 ----

@Serializable
data class BuddyDto(
    val userId: Long,
    val nickname: String = "",
    val mannerTemperature: Double = 0.0,
    val activity: BuddyActivityDto? = null,
    val notifyGatheringCreated: Boolean = true,
    val notifyGatheringApplied: Boolean = true,
)

@Serializable
data class BuddyActivityDto(val gatheringId: Long, val placeName: String = "", val startAt: String, val inProgress: Boolean = false)

@Serializable
data class WorkoutPartnerDto(
    val userId: Long,
    val nickname: String = "",
    val workoutCount: Long = 0,
    val lastWorkoutAt: String? = null,
    val relation: String = "NONE", // NONE, BUDDY, REQUEST_SENT, REQUEST_RECEIVED
    val requestId: Long? = null,
)

@Serializable
data class BuddyActivityCardDto(
    val gatheringId: Long,
    val title: String = "",
    val placeName: String = "",
    val startAt: String,
    val inProgress: Boolean = false,
    val confirmedCount: Long = 0,
    val capacity: Int = 0,
)

@Serializable
data class ReceivedBuddyRequestDto(
    val requestId: Long,
    val userId: Long,
    val nickname: String = "",
    val mannerTemperature: Double = 0.0,
    val participationCount: Long = 0,
    val requestedAt: String? = null,
)

@Serializable
data class BuddyRequestDto(val requestId: Long, val status: String = "")

@Serializable
data class BuddyRequestCreateDto(val receiverId: Long)

@Serializable
data class BuddyNotificationDto(val gatheringCreated: Boolean? = null, val gatheringApplied: Boolean? = null)

@Serializable
data class BuddyNotificationResultDto(val userId: Long = 0, val gatheringCreated: Boolean = true, val gatheringApplied: Boolean = true)

// ---- 후기 ----

@Serializable
data class PendingReviewsDto(val gatherings: List<PendingGatheringDto> = emptyList())

@Serializable
data class PendingGatheringDto(
    val gatheringId: Long,
    val title: String = "",
    val startAt: String,
    val endAt: String,
    val deadlineAt: String,
    val remainingCount: Long = 0,
)

@Serializable
data class ReviewTagsDto(val positive: List<TagDto> = emptyList(), val negative: List<TagDto> = emptyList())

@Serializable
data class ReviewTargetsDto(
    val gatheringId: Long,
    val deadlineAt: String,
    val reviewable: Boolean = true,
    val targets: List<ReviewTargetDto> = emptyList(),
)

@Serializable
data class ReviewTargetDto(val userId: Long, val nickname: String = "", val host: Boolean = false, val reviewed: Boolean = false)

@Serializable
data class ReviewSubmitDto(val targetId: Long, val rating: String, val tags: List<String>, val comment: String? = null)

@Serializable
data class ReviewResultDto(val reviewId: Long = 0)

@Serializable
data class ReceivedReviewsDto(val content: List<ReceivedReviewDto> = emptyList(), val nextCursor: String? = null)

@Serializable
data class ReceivedReviewDto(
    val reviewId: Long,
    val rating: String = "GOOD", // GOOD, NORMAL, BAD
    val comment: String? = null,
    val tags: List<TagDto> = emptyList(),
    val createdAt: String,
    val evaluator: EvaluatorDto? = null, // 아쉬움 평가는 null
)

@Serializable
data class TagDto(val code: String = "", val label: String = "")

@Serializable
data class EvaluatorDto(val userId: Long, val nickname: String = "")

// ---- 프로필 ----

@Serializable
data class UserProfileDto(
    val userId: Long,
    val nickname: String = "",
    val mannerTemperature: Double = 0.0,
    val me: Boolean = false,
    val stats: StatsDto = StatsDto(),
    val buddy: BuddyStatusDto = BuddyStatusDto(),
)

@Serializable
data class StatsDto(
    val reviewCount: Long = 0,
    val hostedCount: Long = 0,
    val participatedCount: Long = 0,
    val topTags: List<TagCountDto> = emptyList(),
)

@Serializable
data class TagCountDto(val code: String = "", val label: String = "", val polarity: String = "", val count: Long = 0)

@Serializable
data class BuddyStatusDto(val relation: String = "NONE", val requestId: Long? = null, val canRequest: Boolean = false)

@Serializable
data class NotificationSettingsDto(val pushEnabled: Boolean = true)

@Serializable
data class NotificationSettingsRequestDto(val pushEnabled: Boolean)
