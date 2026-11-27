package com.example.myapplication.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// 시작 시각 변경 제안. 호스트·확정 참가자만 쓸 수 있다(그 외 TP002). 모두 토큰이 필요하다.
interface TimeProposalApi {
    @POST("api/gatherings/{g}/time-proposals")
    suspend fun create(@Path("g") g: Long, @Body request: TimeProposalCreateRequest): ApiResponse<TimeProposalDto>

    // 진행 중인 제안. 없으면 data가 빠진다.
    @GET("api/gatherings/{g}/time-proposals/current")
    suspend fun current(@Path("g") g: Long): ApiResponse<TimeProposalDto>

    @POST("api/gatherings/{g}/time-proposals/{p}/accept")
    suspend fun accept(@Path("g") g: Long, @Path("p") p: Long): ApiResponse<TimeProposalDto>

    @POST("api/gatherings/{g}/time-proposals/{p}/reject")
    suspend fun reject(@Path("g") g: Long, @Path("p") p: Long): ApiResponse<TimeProposalDto>

    @DELETE("api/gatherings/{g}/time-proposals/{p}")
    suspend fun withdraw(@Path("g") g: Long, @Path("p") p: Long): ApiResponse<Unit>
}

@Serializable
data class TimeProposalCreateRequest(val startAt: String)

@Serializable
data class TimeProposalDto(
    val proposalId: Long,
    val status: String = "PENDING",
    val proposerId: Long = 0,
    val proposerNickname: String = "",
    val fromStartAt: String,
    val toStartAt: String,
    val expiresAt: String,
    val createdAt: String? = null,
    val resolvedAt: String? = null,
    val votes: List<TimeProposalVoteDto> = emptyList(),
)

@Serializable
data class TimeProposalVoteDto(
    val userId: Long,
    val nickname: String = "",
    val host: Boolean = false,
    val decision: String = "PENDING",
    val respondedAt: String? = null,
)
