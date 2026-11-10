package com.example.myapplication.data.api

import com.example.myapplication.data.local.TokenStore
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

// 서버 통신 한 벌. 앱에서는 Api(싱글톤)로 쓰고, 테스트에서는 가짜 서버 주소로 직접 만든다.
//  - authApi: 토큰 없이 부르는 인증 API (로그인·가입·재발급·로그아웃·중복 확인)
//  - 그 밖의 API(userApi 등): 모든 요청에 액세스 토큰을 붙이고, 401이면 리프레시 토큰으로 한 번 재발급해서 다시 보낸다.
class ApiClient(
    baseUrl: String,
    private val tokens: TokenStore,
    private val onSessionExpired: () -> Unit,
    debugLog: Boolean = false,
) {
    private val bare: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .apply {
            if (debugLog) {
                // 비밀번호·토큰이 로그에 남지 않게 본문은 찍지 않고 주소와 상태만 찍는다.
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    private val retrofitBare = retrofit(baseUrl, bare)

    val authApi: AuthApi = retrofitBare.create(AuthApi::class.java)

    private val authed: OkHttpClient = bare.newBuilder()
        .addInterceptor(BearerInterceptor(tokens))
        .authenticator(TokenAuthenticator(tokens, authApi, onSessionExpired))
        .build()

    private val retrofitAuthed = retrofit(baseUrl, authed)

    val userApi: UserApi = retrofitAuthed.create(UserApi::class.java)
    val meetingApi: MeetingApi = retrofitAuthed.create(MeetingApi::class.java)
    val noticeApi: NoticeApi = retrofitAuthed.create(NoticeApi::class.java)
    val notificationApi: NotificationApi = retrofitAuthed.create(NotificationApi::class.java)
    val buddyApi: BuddyApi = retrofitAuthed.create(BuddyApi::class.java)
    val reviewApi: ReviewApi = retrofitAuthed.create(ReviewApi::class.java)

    // 다른 도메인 API를 붙일 때: api.create(MeetingApi::class.java)
    fun <T> create(service: Class<T>): T = retrofitAuthed.create(service)

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.toHttpUrl())
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
        .build()
}

internal class BearerInterceptor(private val tokens: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.accessToken ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().withBearer(token))
    }
}

// 401(토큰 만료)을 받으면 리프레시 토큰으로 새 토큰을 받아 같은 요청을 한 번 더 보낸다.
//  - 여러 요청이 동시에 401을 받아도 재발급은 한 번만 한다. (서버는 한 번 쓴 리프레시 토큰을 다시 쓰면 A004로 막는다)
//  - 재발급이 서버에서 거절되면(401) 로그인이 풀린 것이라 토큰을 지우고 알린다.
//  - 네트워크가 끊겨 재발급을 못 한 경우는 로그인을 풀지 않고, 요청만 네트워크 오류로 끝낸다.
internal class TokenAuthenticator(
    private val tokens: TokenStore,
    private val authApi: AuthApi,
    private val onSessionExpired: () -> Unit,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponseCount() >= 2) return null // 재발급해도 401이면 더 시도하지 않는다
        val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()

        synchronized(lock) {
            // 기다리는 사이 다른 요청이 이미 재발급했으면 그 토큰으로 다시 보낸다.
            val current = tokens.accessToken
            if (current != null && current != usedToken) return response.request.withBearer(current)

            val refreshToken = tokens.refreshToken ?: return null
            val fresh = try {
                runBlocking { authApi.refresh(RefreshTokenRequest(refreshToken)).data }
            } catch (e: HttpException) {
                if (e.code() == 401) expire() // 만료·무효·재사용. 그 밖의 오류(429, 5xx)는 로그인을 유지한다.
                return null
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                return null
            } ?: return null

            tokens.update(fresh)
            return response.request.withBearer(fresh.accessToken)
        }
    }

    private fun expire() {
        tokens.clear()
        onSessionExpired()
    }

    private fun Response.priorResponseCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

private fun Request.withBearer(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()
