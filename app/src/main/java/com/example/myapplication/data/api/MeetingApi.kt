package com.example.myapplication.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// 모임·참여 신청·체크인 API. 모두 토큰이 필요하다.
interface MeetingApi {
    @GET("api/gatherings")
    suspend fun search(
        @Query("swLat") swLat: Double?,
        @Query("swLng") swLng: Double?,
        @Query("neLat") neLat: Double?,
        @Query("neLng") neLng: Double?,
        @Query("cursor") cursor: String?,
        @Query("size") size: Int,
    ): ApiResponse<SearchDto>

    // 지도 핀: 지번이 같은 모임을 핀 하나로 묶어 모임 수를 준다
    @GET("api/gatherings/pins")
    suspend fun pins(
        @Query("swLat") swLat: Double?,
        @Query("swLng") swLng: Double?,
        @Query("neLat") neLat: Double?,
        @Query("neLng") neLng: Double?,
    ): ApiResponse<PinsDto>

    // 확정 명단. 마감된 모임의 호스트·확정 참가자만 볼 수 있다.
    @GET("api/gatherings/{id}/participants")
    suspend fun participants(@Path("id") id: Long): ApiResponse<List<MemberDto>>

    @GET("api/gatherings/{id}")
    suspend fun detail(@Path("id") id: Long): ApiResponse<GatheringDetailDto>

    @POST("api/gatherings")
    suspend fun create(@Body request: CreateGatheringRequest): ApiResponse<GatheringDetailDto>

    @PATCH("api/gatherings/{id}")
    suspend fun update(@Path("id") id: Long, @Body request: UpdateGatheringRequest): ApiResponse<GatheringDetailDto>

    @DELETE("api/gatherings/{id}")
    suspend fun delete(@Path("id") id: Long): ApiResponse<Unit>

    @POST("api/gatherings/{id}/cancel")
    suspend fun cancel(@Path("id") id: Long, @Body request: CancelGatheringRequest): ApiResponse<Unit>

    @PUT("api/gatherings/{id}/like")
    suspend fun like(@Path("id") id: Long): ApiResponse<Unit>

    @DELETE("api/gatherings/{id}/like")
    suspend fun unlike(@Path("id") id: Long): ApiResponse<Unit>

    @POST("api/gatherings/{id}/applications")
    suspend fun apply(@Path("id") id: Long, @Body request: ApplyRequestDto): ApiResponse<ApplicationDto>

    @DELETE("api/gatherings/{id}/applications/me")
    suspend fun cancelApplication(@Path("id") id: Long): ApiResponse<Unit>

    @GET("api/gatherings/{id}/applications")
    suspend fun applicants(@Path("id") id: Long): ApiResponse<ApplicantsDto>

    @POST("api/gatherings/{id}/applications/{applicationId}/accept")
    suspend fun accept(@Path("id") id: Long, @Path("applicationId") applicationId: Long): ApiResponse<ApplicationDto>

    @POST("api/gatherings/{id}/applications/{applicationId}/reject")
    suspend fun reject(@Path("id") id: Long, @Path("applicationId") applicationId: Long): ApiResponse<ApplicationDto>

    @POST("api/gatherings/{id}/leave")
    suspend fun leave(@Path("id") id: Long): ApiResponse<ApplicationDto>

    @POST("api/gatherings/{id}/check-in")
    suspend fun checkIn(@Path("id") id: Long, @Body request: CheckInRequestDto): ApiResponse<CheckInDto>

    @GET("api/gatherings/{id}/check-ins")
    suspend fun checkIns(@Path("id") id: Long): ApiResponse<CheckInStatusDto>

    // 내 활동 내역 (개설한 모임, 참여한 모임). role = host | participant, period = upcoming | past
    @GET("api/users/me/gatherings")
    suspend fun myGatherings(
        @Query("role") role: String,
        @Query("period") period: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): ApiResponse<MyActivitiesDto>
}

// ---- 응답 ----

@Serializable
data class PinsDto(val pins: List<PinDto> = emptyList())

@Serializable
data class PinDto(val parcelKey: String = "", val gatheringCount: Long = 0, val latitude: Double = 0.0, val longitude: Double = 0.0)

@Serializable
data class SearchDto(val content: List<CardDto> = emptyList(), val nextCursor: String? = null)

@Serializable
data class CardDto(
    val gatheringId: Long,
    val title: String,
    val startAt: String,
    val endAt: String,
    val distanceMeters: Long? = null,
    val place: PlaceDto,
    val host: HostDto,
    val confirmedCount: Long = 0,
    val capacity: Int = 0,
    val liked: Boolean = false,
)

@Serializable
data class PlaceDto(
    val name: String = "",
    val roadAddress: String? = null,
    val jibunAddress: String? = null,
    val detailAddress: String? = null,
    val regionCode: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

@Serializable
data class HostDto(
    val userId: Long,
    val nickname: String = "",
    val mannerTemperature: Double = 0.0,
    val reviewCount: Long = 0,
)

@Serializable
data class GatheringDetailDto(
    val gatheringId: Long,
    val sport: String = "GYM",
    val title: String = "",
    val description: String? = null,
    val status: String = "RECRUITING", // RECRUITING, CLOSED, IN_PROGRESS, FINISHED, CANCELED
    val startAt: String,
    val endAt: String,
    val durationMinutes: Long = 0,
    val deadlineAt: String? = null,
    val capacity: Int = 0,
    val confirmedCount: Long = 0,
    val pendingCount: Long? = null,
    val genderRule: String = "ANY", // ANY, FEMALE_ONLY, MALE_ONLY
    val place: PlaceDto,
    val host: HostDto,
    val gym: GymCriteriaDto? = null,
    val viewer: ViewerDto = ViewerDto(),
    val members: List<MemberDto>? = null,
    val cancellation: CancellationDto? = null,
)

@Serializable
data class ViewerDto(
    val role: String = "NONE", // HOST, PARTICIPANT, APPLICANT, NONE
    val applicationId: Long? = null,
    val liked: Boolean = false,
)

@Serializable
data class MemberDto(
    val userId: Long,
    val nickname: String = "",
    val mannerTemperature: Double = 0.0,
    val host: Boolean = false,
)

@Serializable
data class CancellationDto(val type: String? = null, val reason: String? = null, val canceledAt: String? = null)

@Serializable
data class GymCriteriaDto(val parts: List<GymPartDto> = emptyList(), val match: GymMatchDto? = null)

@Serializable
data class GymPartDto(
    val bodyPart: String,
    val recordShown: Boolean = false,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val armLevel: String? = null,
)

@Serializable
data class GymMatchDto(
    val result: String? = null, // MATCH, PARTIAL, MISMATCH
    val mismatchedParts: List<String> = emptyList(),
    val partsWithoutMyRecord: List<String> = emptyList(),
)

@Serializable
data class ApplicationDto(val applicationId: Long, val status: String = "", val gatheringStatus: String = "")

@Serializable
data class ApplicantsDto(val confirmed: List<MemberDto> = emptyList(), val pending: List<ApplicantDto> = emptyList())

@Serializable
data class ApplicantDto(
    val applicationId: Long,
    val userId: Long,
    val nickname: String = "",
    val mannerTemperature: Double = 0.0,
    val message: String? = null,
    val appliedAt: String,
    val gymRecords: List<GymRecordDto> = emptyList(),
)

@Serializable
data class CheckInDto(val attendance: String = "", val checkedInAt: String, val distanceMeters: Int = 0)

@Serializable
data class CheckInStatusDto(
    val me: MyCheckInDto? = null,
    val members: List<MemberCheckInDto>? = null,
    val radiusMeters: Int = 0,
)

@Serializable
data class MyCheckInDto(val attendance: String? = null, val checkedInAt: String? = null, val distanceMeters: Int? = null)

@Serializable
data class MemberCheckInDto(
    val userId: Long,
    val nickname: String = "",
    val host: Boolean = false,
    val attendance: String? = null,
    val checkedInAt: String? = null,
)

// 체크인 범위 밖(C003) 응답에 공통 에러 형식 말고 더 붙는 값
@Serializable
data class OutOfRangeBody(val distanceMeters: Int = 0, val radiusMeters: Int = 0)

@Serializable
data class MyActivitiesDto(val content: List<MyActivityDto> = emptyList(), val page: Int = 0, val totalPages: Int = 0)

@Serializable
data class MyActivityDto(
    val gatheringId: Long,
    val title: String = "",
    val sport: String = "GYM",
    val placeName: String = "",
    val address: String? = null,
    val detailAddress: String? = null,
    val startAt: String,
    val endAt: String,
    val hostId: Long,
    val hostNickname: String = "",
    val confirmedCount: Long = 0,
    val capacity: Int = 0,
    val gatheringStatus: String = "RECRUITING",
    val participationStatus: String? = null, // 참여한 모임에서만. PENDING, CONFIRMED
)

// ---- 요청 ----

@Serializable
data class CreateGatheringRequest(
    val title: String,
    val description: String?,
    val startAt: String,
    val durationMinutes: Int,
    val capacity: Int,
    val genderRule: String,
    val place: PlaceRequestDto,
    val detailAddress: String?,
    val gym: GymCriteriaRequestDto,
)

// 보낸 항목만 바꾼다. null(안 보냄)은 그대로 둔다.
@Serializable
data class UpdateGatheringRequest(
    val title: String? = null,
    val description: String? = null,
    val detailAddress: String? = null,
    val startAt: String? = null,
    val durationMinutes: Int? = null,
    val capacity: Int? = null,
    val genderRule: String? = null,
    val place: PlaceRequestDto? = null,
    val gym: GymCriteriaRequestDto? = null,
)

@Serializable
data class PlaceRequestDto(
    val kakaoPlaceId: String? = null,
    val name: String,
    val roadAddress: String? = null,
    val jibunAddress: String,
    val regionCode: String,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class GymCriteriaRequestDto(val parts: List<String>, val showMyRecord: Boolean)

@Serializable
data class CancelGatheringRequest(val reason: String)

// 한마디만 보낸다. gymRecords를 생략하면 서버가 지금 프로필 기록을 쓴다.
@Serializable
data class ApplyRequestDto(val message: String? = null)

@Serializable
data class CheckInRequestDto(val latitude: Double, val longitude: Double)
