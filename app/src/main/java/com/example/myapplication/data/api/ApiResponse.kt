package com.example.myapplication.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// 서버 공통 성공 응답: { "code": 200, "message": "...", "data": ... }. data가 없으면 키 자체가 빠진다.
@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
)

// 서버 공통 에러 응답: { "code": "A001", "message": "...", "errors": { "field": "사유" } }. errors는 입력값 검증 실패(G002)일 때만 온다.
@Serializable
data class ApiErrorBody(
    val code: String = "",
    val message: String = "",
    val errors: Map<String, String>? = null,
)

// 서버가 에러 응답으로 돌려준 실패. 네트워크가 끊긴 경우는 이 예외가 아니라 IOException 그대로 돌려준다.
class ApiException(
    val httpStatus: Int,
    val code: String,
    message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    val rawBody: String? = null, // 에러 응답 원문. 체크인 범위 밖(C003)처럼 공통 형식에 필드가 더 붙는 응답을 읽을 때 쓴다
) : Exception(message) {
    // 서버 에러 코드 (service 문서가 아니라 서버 코드의 *ErrorType 기준)
    companion object {
        const val INVALID_CREDENTIALS = "A001"
        const val INVALID_TOKEN = "A002"
        const val EXPIRED_TOKEN = "A003"
        const val REFRESH_TOKEN_REUSED = "A004"
        const val TOO_MANY_LOGIN_ATTEMPTS = "A005"
        const val DUPLICATE_EMAIL = "U002"
        const val DUPLICATE_NICKNAME = "U003"
        const val INVALID_INPUT = "G002"
    }
}

// 응답에서 data를 꺼낸다. 성공인데 data가 비어 있으면 서버와 약속이 어긋난 것이라 실패로 본다.
fun <T> ApiResponse<T>.requireData(): T =
    data ?: throw IOException("서버 응답에 data가 없어요 (code=$code)")

// Retrofit 호출을 Result로 바꾼다. 서버 에러는 ApiException, 네트워크 문제는 IOException으로 돌려준다.
// 취소(CancellationException)는 삼키지 않고 그대로 던진다.
suspend fun <T> apiCall(json: Json = ApiJson, block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    Result.failure(e.toApiException(json))
} catch (e: Exception) {
    Result.failure(e)
}

internal fun HttpException.toApiException(json: Json = ApiJson): ApiException {
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val body = raw?.let { runCatching { json.decodeFromString<ApiErrorBody>(it) }.getOrNull() }
    return ApiException(
        httpStatus = code(),
        code = body?.code.orEmpty(),
        message = body?.message?.takeIf { it.isNotBlank() } ?: message(),
        fieldErrors = body?.errors.orEmpty(),
        rawBody = raw,
    )
}

val ApiJson = Json {
    ignoreUnknownKeys = true // 서버가 필드를 추가해도 앱이 깨지지 않게 한다
    explicitNulls = false
    coerceInputValues = true
}
