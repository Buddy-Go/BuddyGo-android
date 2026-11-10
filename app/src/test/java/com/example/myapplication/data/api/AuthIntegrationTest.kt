package com.example.myapplication.data.api

import com.example.myapplication.data.local.InMemoryKeyValueStore
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.SignUpDraft
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.RealUserRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// 가짜 서버(MockWebServer)로 인증 흐름을 확인한다. 서버가 실제로 내려주는 JSON 모양 그대로 응답한다.
class AuthIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: TokenStore
    private lateinit var repo: RealUserRepository
    private var expiredCount = 0

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        tokens = TokenStore(InMemoryKeyValueStore())
        expiredCount = 0
        val client = ApiClient(server.url("/").toString(), tokens, onSessionExpired = { expiredCount++ })
        repo = RealUserRepository(client.authApi, client.userApi, tokens, client.reviewApi, FakeUserRepository())
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(body: String, status: Int = 200) =
        MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)

    private fun tokenBody(access: String, refresh: String) =
        """{"code":200,"message":"요청이 성공하였습니다.","data":{"accessToken":"$access","refreshToken":"$refresh","tokenType":"Bearer","accessTokenExpiresIn":1800,"refreshTokenExpiresIn":1209600}}"""

    private val meBody = """{"code":200,"message":"ok","data":{"userId":7,"email":"buddy@example.com","name":"김버디","nickname":"헬스버디","gender":"FEMALE","provider":"LOCAL","mannerTemperature":36.5,"pushEnabled":true,"stats":{"reviewCount":0,"hostedCount":0,"participatedCount":0,"topTags":[]}}}"""

    @Test
    fun login_saves_tokens_and_returns_me_with_bearer() = runTest {
        server.enqueue(json(tokenBody("a1", "r1")))
        server.enqueue(json(meBody))

        val user = repo.login(" buddy@example.com ", "password123", keepLogin = true).getOrThrow()

        assertEquals("7", user.id)
        assertEquals("헬스버디", user.nickname)
        assertEquals(Gender.FEMALE, user.gender)
        assertEquals(36.5f, user.mannerTemperature, 0.001f)
        assertEquals("a1", tokens.accessToken)
        val login = server.takeRequest()
        assertEquals("/api/auth/login", login.path)
        assertTrue(login.body.readUtf8().contains(""""email":"buddy@example.com""""))
        assertNull(login.getHeader("Authorization"))
        val me = server.takeRequest()
        assertEquals("/api/users/me", me.path)
        assertEquals("Bearer a1", me.getHeader("Authorization"))
    }

    @Test
    fun login_with_wrong_password_returns_A001() = runTest {
        server.enqueue(json("""{"code":"A001","message":"이메일 또는 비밀번호가 올바르지 않습니다."}""", 401))

        val e = repo.login("buddy@example.com", "bad", keepLogin = true).exceptionOrNull() as ApiException

        assertEquals(ApiException.INVALID_CREDENTIALS, e.code)
        assertEquals(401, e.httpStatus)
        assertNull(tokens.accessToken)
        assertEquals(0, expiredCount) // 로그인 실패는 '로그인이 풀림'이 아니다
    }

    @Test
    fun login_rolls_back_tokens_when_me_fails() = runTest {
        server.enqueue(json(tokenBody("a1", "r1")))
        server.enqueue(json("""{"code":"G001","message":"서버 오류가 발생했습니다."}""", 500))

        assertTrue(repo.login("buddy@example.com", "password123", keepLogin = true).isFailure)
        assertFalse(tokens.hasSession)
    }

    @Test
    fun network_failure_is_IOException() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val e = repo.login("buddy@example.com", "password123", keepLogin = true).exceptionOrNull()

        assertTrue(e is IOException)
    }

    @Test
    fun signUp_sends_server_fields_and_logs_in() = runTest {
        server.enqueue(json(tokenBody("a1", "r1"), 201))
        server.enqueue(json(meBody))

        val user = repo.signUp(
            SignUpDraft(name = " 김버디 ", nickname = "헬스버디", email = "buddy@example.com", password = "password123", gender = Gender.FEMALE),
        ).getOrThrow()

        assertEquals("헬스버디", user.nickname)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains(""""name":"김버디""""))
        assertTrue(body.contains(""""gender":"FEMALE""""))
        assertTrue(tokens.hasSession)
    }

    @Test
    fun signUp_duplicate_email_returns_U002() = runTest {
        server.enqueue(json("""{"code":"U002","message":"이미 사용 중인 이메일입니다."}""", 409))

        val e = repo.signUp(SignUpDraft(name = "a", nickname = "bb", email = "x@x.com", password = "password123", gender = Gender.MALE)).exceptionOrNull() as ApiException

        assertEquals(ApiException.DUPLICATE_EMAIL, e.code)
    }

    @Test
    fun availability_checks_read_the_flag() = runTest {
        server.enqueue(json("""{"code":200,"message":"ok","data":{"available":false}}"""))
        server.enqueue(json("""{"code":200,"message":"ok","data":{"available":true}}"""))

        assertEquals(false, repo.isEmailAvailable("a@b.com").getOrThrow())
        assertEquals(true, repo.isNicknameAvailable("헬스버디").getOrThrow())
        assertEquals("/api/auth/check-email?email=a%40b.com", server.takeRequest().path)
        assertTrue(server.takeRequest().path!!.startsWith("/api/auth/check-nickname?nickname="))
    }

    @Test
    fun expired_access_token_is_refreshed_once_and_request_retried() = runTest {
        tokens.save(TokenDto("old", "r1"), keepLogin = true)
        server.enqueue(json("""{"code":"A003","message":"만료된 토큰입니다."}""", 401))
        server.enqueue(json(tokenBody("new", "r2")))
        server.enqueue(json(meBody))

        val user = repo.getCurrentUser().getOrThrow()

        assertEquals("7", user.id)
        assertEquals("Bearer old", server.takeRequest().getHeader("Authorization"))
        val refresh = server.takeRequest()
        assertEquals("/api/auth/refresh", refresh.path)
        assertTrue(refresh.body.readUtf8().contains(""""refreshToken":"r1""""))
        assertEquals("Bearer new", server.takeRequest().getHeader("Authorization"))
        assertEquals("new", tokens.accessToken)
        assertEquals("r2", tokens.refreshToken)
        assertEquals(0, expiredCount)
    }

    @Test
    fun rejected_refresh_clears_session_and_notifies() = runTest {
        tokens.save(TokenDto("old", "r1"), keepLogin = true)
        server.enqueue(json("""{"code":"A003","message":"만료된 토큰입니다."}""", 401))
        server.enqueue(json("""{"code":"A004","message":"이미 사용된 리프레시 토큰입니다."}""", 401))

        val e = repo.getCurrentUser().exceptionOrNull() as ApiException

        assertEquals(401, e.httpStatus)
        assertFalse(tokens.hasSession)
        assertEquals(1, expiredCount)
    }

    @Test
    fun refresh_network_failure_keeps_session() = runTest {
        tokens.save(TokenDto("old", "r1"), keepLogin = true)
        server.enqueue(json("""{"code":"A003","message":"만료된 토큰입니다."}""", 401))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertTrue(repo.getCurrentUser().isFailure)
        assertTrue(tokens.hasSession)
        assertEquals(0, expiredCount)
    }

    @Test
    fun logout_clears_tokens_even_when_server_is_unreachable() = runTest {
        tokens.save(TokenDto("a1", "r1"), keepLogin = true)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertTrue(repo.logout().isSuccess)
        assertFalse(tokens.hasSession)
    }

    @Test
    fun logout_sends_refresh_token() = runTest {
        tokens.save(TokenDto("a1", "r1"), keepLogin = true)
        server.enqueue(json("""{"code":200,"message":"ok"}"""))

        repo.logout()

        val req = server.takeRequest()
        assertEquals("/api/auth/logout", req.path)
        assertTrue(req.body.readUtf8().contains(""""refreshToken":"r1""""))
    }

    @Test
    fun tokens_persist_only_when_keepLogin() {
        val disk = InMemoryKeyValueStore()
        TokenStore(disk).save(TokenDto("a", "r"), keepLogin = false)
        assertFalse(TokenStore(disk).hasSession)

        TokenStore(disk).save(TokenDto("a", "r"), keepLogin = true)
        assertTrue(TokenStore(disk).hasSession)
        assertEquals("a", TokenStore(disk).accessToken)

        TokenStore(disk).clear()
        assertFalse(TokenStore(disk).hasSession)
    }
}
