package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiException
import com.example.myapplication.data.api.ApiJson
import com.example.myapplication.data.api.ApplyRequestDto
import com.example.myapplication.data.api.CancelGatheringRequest
import com.example.myapplication.data.api.CardDto
import com.example.myapplication.data.api.CheckInRequestDto
import com.example.myapplication.data.api.CreateGatheringRequest
import com.example.myapplication.data.api.GymCriteriaRequestDto
import com.example.myapplication.data.api.IsoTime
import com.example.myapplication.data.api.MeetingApi
import com.example.myapplication.data.api.OutOfRangeBody
import com.example.myapplication.data.api.PlaceRequestDto
import com.example.myapplication.data.api.RegionLookup
import com.example.myapplication.data.api.UpdateGatheringRequest
import com.example.myapplication.data.api.apiCall
import com.example.myapplication.data.api.requireData
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.data.model.CheckInOutOfRangeException
import com.example.myapplication.data.model.CheckInResult
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.JoinRequestStatus
import com.example.myapplication.data.model.MapBounds
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingMember
import com.example.myapplication.data.model.MeetingPlace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException

// 서버(BuddyGo-server)와 연결한 모임 저장소.
//  - 서버는 헬스 모임만 지원한다. 러닝·등산 모임은 만들 수 없고(실패), 목록에도 나오지 않는다.
//  - 서버 목록(검색) 응답의 카드에는 기준·설명이 없어서, 카드마다 상세를 받아(동시에 몇 개씩) 채운다. 상세를 못 받은 카드는 카드 정보만 쓴다.
class RealMeetingRepository(
    private val api: MeetingApi,
    private val tokens: TokenStore,
    private val regions: RegionLookup,
) : MeetingRepository {

    private val meId: String? get() = tokens.userId

    override suspend fun getMeetings(area: MapBounds?): Result<List<Meeting>> = apiCall {
        val cards = searchAll(area)
        coroutineScope {
            val gate = Semaphore(DETAIL_PARALLELISM)
            cards.map { card ->
                async { gate.withPermit { orNull { loadMeeting(card.gatheringId, withExtras = false) } ?: card.toMeeting() } }
            }.awaitAll()
        }
    }

    // 내가 개설했거나 신청·참여한 모임 전부. 모집중이 아닌 모임(마감·종료·취소)도 포함한다. 활동 내역 화면용.
    override suspend fun getMyMeetings(): Result<List<Meeting>> = apiCall {
        val activities = buildList {
            for (role in listOf("host", "participant")) for (period in listOf("upcoming", "past")) {
                var page = 0
                while (page < MAX_ACTIVITY_PAGES) {
                    val res = api.myGatherings(role, period, page, ACTIVITY_PAGE_SIZE).requireData()
                    addAll(res.content)
                    page++
                    if (page >= res.totalPages) break
                }
            }
        }.distinctBy { it.gatheringId }
        coroutineScope {
            val gate = Semaphore(DETAIL_PARALLELISM)
            activities.map { a ->
                async { gate.withPermit { orNull { loadMeeting(a.gatheringId, withExtras = true) } ?: a.toMeeting(meId) } }
            }.awaitAll()
        }
    }

    override suspend fun getMeeting(meetingId: String): Result<Meeting> = withId(meetingId) { id ->
        apiCall { loadMeeting(id, withExtras = true) }
    }

    override suspend fun getPins(area: MapBounds?): Result<List<MeetingPin>> = apiCall {
        val b = area ?: KOREA
        api.pins(b.south, b.west, b.north, b.east).requireData().pins.map {
            MeetingPin(it.parcelKey, it.gatheringCount.toInt(), it.latitude, it.longitude)
        }
    }

    override suspend fun getParticipants(meetingId: String): Result<List<MeetingMember>> = withId(meetingId) { id ->
        apiCall { api.participants(id).requireData() }.map { list -> list.map { MeetingMember(it.userId.toString(), it.nickname) } }
    }

    override suspend fun setFavorite(meetingId: String, favorite: Boolean): Result<Unit> = withId(meetingId) { id ->
        apiCall { if (favorite) api.like(id) else api.unlike(id) }.map { }
    }

    override suspend fun apply(meetingId: String, message: String?): Result<Unit> = withId(meetingId) { id ->
        apiCall { api.apply(id, ApplyRequestDto(message?.trim()?.takeIf { it.isNotEmpty() })) }.map { }
    }

    override suspend fun cancelApplication(meetingId: String): Result<Unit> = withId(meetingId) { id ->
        apiCall { api.cancelApplication(id) }.map { }
    }

    override suspend fun leave(meetingId: String): Result<Unit> = withId(meetingId) { id ->
        apiCall { api.leave(id) }.map { }
    }

    // 범위 밖(C003)이면 서버가 잰 거리를 CheckInOutOfRangeException에 담아 돌려준다.
    override suspend fun checkIn(meetingId: String, latitude: Double, longitude: Double): Result<CheckInResult> = withId(meetingId) { id ->
        apiCall { api.checkIn(id, CheckInRequestDto(latitude, longitude)).requireData() }
            .map { CheckInResult(IsoTime.parse(it.checkedInAt)) }
            .recoverCatching { e ->
                if (e is ApiException && e.code == CHECK_IN_OUT_OF_RANGE) {
                    val distance = e.rawBody?.let { runCatching { ApiJson.decodeFromString<OutOfRangeBody>(it).distanceMeters }.getOrNull() } ?: 0
                    throw CheckInOutOfRangeException(distance)
                }
                throw e
            }
    }

    override suspend fun getJoinRequests(meetingId: String): Result<List<JoinRequest>> = withId(meetingId) { id ->
        apiCall { api.applicants(id).requireData() }.map { res ->
            res.pending.map { a ->
                JoinRequest(
                    id = a.applicationId.toString(),
                    meetingId = meetingId,
                    applicantId = a.userId.toString(),
                    applicantNickname = a.nickname,
                    applicantMannerTemperature = a.mannerTemperature.toFloat(),
                    applicantRecord = a.gymRecords.toExerciseRecord(),
                    message = a.message?.takeIf { it.isNotBlank() },
                    status = JoinRequestStatus.PENDING,
                    requestedAtMillis = IsoTime.parse(a.appliedAt),
                )
            }
        }
    }

    override suspend fun accept(meetingId: String, requestId: String): Result<Unit> = withId(meetingId) { id ->
        val applicationId = requestId.toLongOrNull() ?: return@withId Result.failure(NoSuchElementException(requestId))
        apiCall { api.accept(id, applicationId) }.map { }
    }

    override suspend fun reject(meetingId: String, requestId: String): Result<Unit> = withId(meetingId) { id ->
        val applicationId = requestId.toLongOrNull() ?: return@withId Result.failure(NoSuchElementException(requestId))
        apiCall { api.reject(id, applicationId) }.map { }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<String> {
        val health = meeting.criteria as? MeetingCriteria.Health
            ?: return Result.failure(UnsupportedOperationException("서버는 아직 헬스 모임만 만들 수 있어요"))
        return apiCall {
            val request = CreateGatheringRequest(
                title = meeting.title.trim(),
                description = meeting.description.trim().takeIf { it.isNotEmpty() },
                startAt = IsoTime.format(meeting.startAtMillis),
                durationMinutes = meeting.durationMinutes,
                capacity = meeting.capacity,
                genderRule = meeting.genderLimit.toServer(),
                place = placeRequest(meeting.place),
                detailAddress = meeting.place.detailAddress?.trim()?.takeIf { it.isNotEmpty() },
                gym = health.toRequest(),
            )
            api.create(request).requireData().gatheringId.toString()
        }
    }

    // 바뀐 항목만 보낸다. 서버는 확정 참가자(호스트 제외)가 있을 때 제목·설명·상세주소 말고 다른 항목이 오면 거절하기 때문이다.
    override suspend fun updateMeeting(meetingId: String, meeting: Meeting): Result<Unit> = withId(meetingId) { id ->
        apiCall {
            val current = api.detail(id).requireData().toMeeting(meId)
            val health = meeting.criteria as? MeetingCriteria.Health
            val currentHealth = current.criteria as? MeetingCriteria.Health
            val placeChanged = meeting.place.latitude != current.place.latitude || meeting.place.longitude != current.place.longitude ||
                meeting.place.name != current.place.name
            val gymChanged = health != null && currentHealth != null &&
                (health.parts != currentHealth.parts || (health.hostRecord != null) != (currentHealth.hostRecord != null))
            val request = UpdateGatheringRequest(
                title = meeting.title.trim().takeIf { it != current.title },
                description = meeting.description.trim().takeIf { it != current.description },
                detailAddress = (meeting.place.detailAddress?.trim().orEmpty()).takeIf { it != current.place.detailAddress.orEmpty() },
                startAt = IsoTime.format(meeting.startAtMillis).takeIf { meeting.startAtMillis != current.startAtMillis },
                durationMinutes = meeting.durationMinutes.takeIf { it != current.durationMinutes },
                capacity = meeting.capacity.takeIf { it != current.capacity },
                genderRule = meeting.genderLimit.toServer().takeIf { meeting.genderLimit != current.genderLimit },
                place = if (placeChanged) placeRequest(meeting.place) else null,
                gym = if (gymChanged) health?.toRequest() else null,
            )
            if (request != UpdateGatheringRequest()) api.update(id, request).requireData()
        }.map { }
    }

    override suspend fun cancelMeeting(meetingId: String, reason: String): Result<Unit> = withId(meetingId) { id ->
        apiCall { api.cancel(id, CancelGatheringRequest(reason.trim())) }.map { }
    }

    override suspend fun deleteMeeting(meetingId: String): Result<Unit> = withId(meetingId) { id ->
        apiCall { api.delete(id) }.map { }
    }

    // ---- 내부 ----

    // 서버는 검색 영역(지도 범위·구·동·핀 중 하나)이 꼭 있어야 한다(없으면 GA016). 영역이 없으면 대한민국 전체를 덮는 범위를 보낸다.
    private suspend fun searchAll(area: MapBounds?): List<CardDto> {
        val bounds = area ?: KOREA
        val all = mutableListOf<CardDto>()
        var cursor: String? = null
        repeat(MAX_SEARCH_PAGES) {
            val res = api.search(bounds.south, bounds.west, bounds.north, bounds.east, cursor, SEARCH_PAGE_SIZE).requireData()
            all += res.content
            cursor = res.nextCursor
            if (cursor == null) return all
        }
        return all
    }

    // 상세 한 건. withExtras면 호스트의 신청자 id와 체크인 현황까지 받아 채운다. (못 받아도 상세는 돌려준다)
    private suspend fun loadMeeting(id: Long, withExtras: Boolean): Meeting {
        val detail = api.detail(id).requireData()
        if (!withExtras) return detail.toMeeting(meId)
        val role = detail.viewer.role
        val pendingIds = if (role == "HOST" && (detail.pendingCount ?: 0) > 0) {
            orNull { api.applicants(id).requireData().pending.map { it.userId.toString() } }.orEmpty()
        } else emptyList()
        val wantsCheckIns = role in setOf("HOST", "PARTICIPANT") && detail.status in setOf("CLOSED", "IN_PROGRESS", "FINISHED")
        val checkIns = if (wantsCheckIns) orNull { api.checkIns(id).requireData() } else null
        return detail.toMeeting(meId, pendingIds, checkIns)
    }

    private suspend fun placeRequest(place: MeetingPlace): PlaceRequestDto {
        val jibun = place.lotAddress.ifBlank { place.address }
        val region = regions.regionCode(place.latitude, place.longitude)
            ?: throw IllegalStateException("장소의 지역 코드를 찾지 못했어요")
        return PlaceRequestDto(
            kakaoPlaceId = place.kakaoPlaceId?.takeIf { it.isNotBlank() },
            name = place.name,
            roadAddress = place.address.takeIf { it.isNotBlank() && it != jibun },
            jibunAddress = jibun,
            regionCode = region,
            latitude = place.latitude,
            longitude = place.longitude,
        )
    }

    private fun MeetingCriteria.Health.toRequest() =
        GymCriteriaRequestDto(parts = parts.map { it.name }.sorted(), showMyRecord = hostRecord != null)

    private inline fun <T> withId(meetingId: String, block: (Long) -> Result<T>): Result<T> {
        val id = meetingId.toLongOrNull() ?: return Result.failure(NoSuchElementException(meetingId))
        return block(id)
    }

    private suspend fun <T> orNull(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        val KOREA = MapBounds(south = 33.0, west = 124.5, north = 38.7, east = 132.0)
        const val CHECK_IN_OUT_OF_RANGE = "C003"
        const val SEARCH_PAGE_SIZE = 50
        const val MAX_SEARCH_PAGES = 4
        const val ACTIVITY_PAGE_SIZE = 50
        const val MAX_ACTIVITY_PAGES = 3
        const val DETAIL_PARALLELISM = 6
    }
}
