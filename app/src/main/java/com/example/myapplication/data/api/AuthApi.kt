package com.example.myapplication.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// 인증 API. 모두 토큰 없이 부른다. (서버 SecurityConfig의 공개 경로)
interface AuthApi {
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): ApiResponse<TokenDto>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<TokenDto>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): ApiResponse<TokenDto>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): ApiResponse<Unit>

    @GET("api/auth/check-email")
    suspend fun checkEmail(@Query("email") email: String): ApiResponse<AvailabilityDto>

    @GET("api/auth/check-nickname")
    suspend fun checkNickname(@Query("nickname") nickname: String): ApiResponse<AvailabilityDto>
}

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val name: String,
    val nickname: String,
    val gender: String, // MALE, FEMALE
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class TokenDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val accessTokenExpiresIn: Long = 0, // 초
    val refreshTokenExpiresIn: Long = 0, // 초
)

@Serializable
data class AvailabilityDto(val available: Boolean)
