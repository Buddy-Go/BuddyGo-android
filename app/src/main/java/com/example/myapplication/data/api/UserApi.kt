package com.example.myapplication.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

// 사용자 API. 토큰이 필요하다.
interface UserApi {
    @GET("api/users/me")
    suspend fun me(): ApiResponse<MyProfileDto>

    // 다른 사용자의 공개 프로필(매너온도, 활동 요약, 나와의 버디 관계)
    @GET("api/users/{userId}")
    suspend fun profile(@Path("userId") userId: Long): ApiResponse<UserProfileDto>

    // 전체 알림 ON/OFF
    @PATCH("api/users/me/notification-settings")
    suspend fun changeNotificationSettings(@Body request: NotificationSettingsRequestDto): ApiResponse<NotificationSettingsDto>

    // 푸시 토큰(FCM) 등록·삭제. 삭제는 본문이 있는 DELETE다.
    @PUT("api/users/me/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenDto): ApiResponse<Unit>

    @HTTP(method = "DELETE", path = "api/users/me/fcm-token", hasBody = true)
    suspend fun deleteFcmToken(@Body request: FcmTokenDto): ApiResponse<Unit>

    // 내 헬스 기록. 다섯 부위를 항상 담고 미설정 부위는 recorded=false다.
    @GET("api/users/me/profile")
    suspend fun gymProfile(): ApiResponse<GymProfileDto>

    // 보낸 부위가 내 기록 전체가 된다. 빠진 부위는 지워진다.
    @PUT("api/users/me/gym-records")
    suspend fun saveGymRecords(@Body request: GymRecordsRequest): ApiResponse<GymProfileDto>
}

@Serializable
data class FcmTokenDto(val fcmToken: String)

@Serializable
data class GymProfileDto(val records: List<GymRecordDto> = emptyList())

// recordValue: 가슴·하체·어깨는 1RM(kg), 등은 턱걸이 횟수. armLevel은 팔만 쓴다.
@Serializable
data class GymRecordDto(
    val bodyPart: String,
    val recorded: Boolean = true,
    val recordValue: Double? = null,
    val armLevel: String? = null,
)

@Serializable
data class GymRecordsRequest(val records: List<GymRecordDto>)

// 서버 MyProfileResponse에서 지금 앱이 쓰는 값만 받는다. (stats 등은 해당 기능을 붙일 때 추가)
@Serializable
data class MyProfileDto(
    val userId: Long,
    val email: String,
    val name: String,
    val nickname: String,
    val gender: String, // MALE, FEMALE
    val provider: String = "LOCAL", // LOCAL, KAKAO
    val mannerTemperature: Double = 0.0,
    val pushEnabled: Boolean = true,
    val stats: StatsDto = StatsDto(),
)
