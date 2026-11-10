package com.example.myapplication.data.repository

import com.example.myapplication.data.api.BuddyApi
import com.example.myapplication.data.api.BuddyNotificationDto
import com.example.myapplication.data.api.BuddyRequestCreateDto
import com.example.myapplication.data.api.IsoTime
import com.example.myapplication.data.api.MeetingApi
import com.example.myapplication.data.api.ReviewApi
import com.example.myapplication.data.api.UserApi
import com.example.myapplication.data.api.apiCall
import com.example.myapplication.data.api.requireData
import com.example.myapplication.data.model.MateActivity
import com.example.myapplication.data.model.MateActivityKind
import com.example.myapplication.data.model.MateProfile
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.data.model.MateRequest
import com.example.myapplication.data.model.MateSummary
import com.example.myapplication.data.model.ProfileReview
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.TagCount
import com.example.myapplication.data.model.WorkedOutPerson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

// 서버(BuddyGo-server)와 연결한 메이트(서버 이름: 버디) 저장소.
//  - 메이트 목록의 '다음 활동'은 서버가 장소·시각만 줘서, 모임 상세를 받아 제목과 인원을 채운다. (못 받으면 장소 이름을 제목으로 쓴다)
//  - 서버는 종목이 헬스뿐이라 '함께 운동한 종목'과 '자주 하는 운동'은 헬스로 채운다.
class RealMateRepository(
    private val buddyApi: BuddyApi,
    private val userApi: UserApi,
    private val meetingApi: MeetingApi,
    private val reviewApi: ReviewApi,
) : MateRepository {

    override suspend fun getMates(): Result<List<MateSummary>> = apiCall {
        val buddies = buddyApi.buddies().requireData()
        coroutineScope {
            buddies.map { b ->
                async {
                    val activity = b.activity?.let { a ->
                        val detail = orNull { meetingApi.detail(a.gatheringId).requireData() }
                        MateActivity(
                            meetingId = a.gatheringId.toString(),
                            kind = when {
                                a.inProgress -> MateActivityKind.EXERCISING_NOW
                                detail?.host?.userId == b.userId -> MateActivityKind.HOSTING
                                else -> MateActivityKind.JOINED
                            },
                            title = detail?.title ?: a.placeName,
                            placeName = a.placeName,
                            startAtMillis = IsoTime.parse(a.startAt),
                            confirmedCount = detail?.confirmedCount?.toInt() ?: 0,
                            capacity = detail?.capacity ?: 0,
                        )
                    }
                    MateSummary(b.userId.toString(), b.nickname, activity)
                }
            }.awaitAll()
        }
    }

    override suspend fun getWorkedOut(): Result<List<WorkedOutPerson>> = apiCall {
        buddyApi.workoutPartners(null).requireData().map { p ->
            WorkedOutPerson(
                userId = p.userId.toString(),
                nickname = p.nickname,
                lastWorkedOutAtMillis = p.lastWorkoutAt?.let(IsoTime::parse) ?: 0L,
                relation = p.relation.toMateRelation(),
                sports = setOf(Sport.HEALTH),
            )
        }
    }

    override suspend fun getRequests(): Result<List<MateRequest>> = apiCall {
        buddyApi.received().requireData().map {
            MateRequest(it.requestId.toString(), it.userId.toString(), it.nickname, it.mannerTemperature.toFloat(), it.participationCount.toInt())
        }
    }

    override suspend fun acceptRequest(requestId: String): Result<Unit> = withLong(requestId) { id -> apiCall { buddyApi.accept(id) }.map { } }

    override suspend fun rejectRequest(requestId: String): Result<Unit> = withLong(requestId) { id -> apiCall { buddyApi.reject(id) }.map { } }

    override suspend fun sendRequest(userId: String): Result<Unit> = withLong(userId) { id ->
        apiCall { buddyApi.send(BuddyRequestCreateDto(id)) }.map { }
    }

    override suspend fun cancelRequest(requestId: String): Result<Unit> = withLong(requestId) { id -> apiCall { buddyApi.cancel(id) }.map { } }

    override suspend fun getProfile(userId: String): Result<MateProfile> = withLong(userId) { id ->
        apiCall {
            val profile = userApi.profile(id).requireData()
            val relation = profile.buddy.relation.toMateRelation()
            val isBuddy = profile.buddy.relation == "BUDDY"
            val notify = if (isBuddy) orNull { buddyApi.buddies().requireData().firstOrNull { it.userId == id } } else null
            val activities = if (isBuddy) orNull {
                buddyApi.activities(id).requireData().map { a ->
                    MateActivity(
                        meetingId = a.gatheringId.toString(),
                        kind = if (a.inProgress) MateActivityKind.EXERCISING_NOW else MateActivityKind.JOINED,
                        title = a.title,
                        placeName = a.placeName,
                        startAtMillis = IsoTime.parse(a.startAt),
                        confirmedCount = a.confirmedCount.toInt(),
                        capacity = a.capacity,
                    )
                }
            }.orEmpty() else emptyList()
            val reviews = orNull { reviewApi.userReviews(id, null, REVIEW_PREVIEW).requireData().content }.orEmpty()
            MateProfile(
                userId = profile.userId.toString(),
                nickname = profile.nickname,
                mannerTemperature = profile.mannerTemperature.toFloat(),
                topTags = profile.stats.topTags.map { TagCount(it.label, it.count.toInt()) },
                relation = relation,
                workedOutTogether = profile.buddy.canRequest || profile.buddy.relation != "NONE",
                activities = activities,
                reviewCount = profile.stats.reviewCount.toInt(),
                recentReviews = reviews.mapNotNull { r ->
                    val author = r.evaluator ?: return@mapNotNull null // 아쉬움 평가는 평가자를 숨긴다
                    ProfileReview(author.userId.toString(), author.nickname, IsoTime.parse(r.createdAt), r.tags.firstOrNull()?.label, r.comment.orEmpty())
                },
                notificationEnabled = notify?.let { it.notifyGatheringCreated || it.notifyGatheringApplied } ?: true,
                frequentSports = listOf(Sport.HEALTH),
            )
        }
    }

    override suspend fun removeMate(userId: String): Result<Unit> = withLong(userId) { id -> apiCall { buddyApi.remove(id) }.map { } }

    // 서버는 '개설 알림'과 '신청 알림'을 따로 두지만 앱은 하나의 스위치라 둘을 같이 바꾼다.
    override suspend fun setMateNotification(userId: String, enabled: Boolean): Result<Unit> = withLong(userId) { id ->
        apiCall { buddyApi.notification(id, BuddyNotificationDto(gatheringCreated = enabled, gatheringApplied = enabled)) }.map { }
    }

    private inline fun <T> withLong(id: String, block: (Long) -> Result<T>): Result<T> {
        val v = id.toLongOrNull() ?: return Result.failure(NoSuchElementException(id))
        return block(v)
    }

    private suspend fun <T> orNull(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val REVIEW_PREVIEW = 5
    }
}

internal fun String.toMateRelation(): MateRelation = when (this) {
    "BUDDY" -> MateRelation.MATE
    "REQUEST_SENT" -> MateRelation.REQUESTED
    else -> MateRelation.NONE
}
