package com.example.myapplication.data.repository

import com.example.myapplication.data.api.AuthApi
import com.example.myapplication.data.api.FcmTokenDto
import com.example.myapplication.data.api.GymRecordsRequest
import com.example.myapplication.data.api.LoginRequest
import com.example.myapplication.data.api.IsoTime
import com.example.myapplication.data.api.MyProfileDto
import com.example.myapplication.data.api.NotificationSettingsRequestDto
import com.example.myapplication.data.api.ReceivedReviewDto
import com.example.myapplication.data.api.ReviewApi
import com.example.myapplication.data.api.StatsDto
import com.example.myapplication.data.api.RefreshTokenRequest
import com.example.myapplication.data.api.SignupRequest
import com.example.myapplication.data.api.UserApi
import com.example.myapplication.data.api.apiCall
import com.example.myapplication.data.api.requireData
import com.example.myapplication.data.local.ActivityAreaStore
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.ProfileStats
import com.example.myapplication.data.model.ReceivedReview
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.TagCount
import com.example.myapplication.data.model.SignUpDraft
import com.example.myapplication.data.model.User

// 서버(BuddyGo-server)와 연결한 사용자 저장소. 지금 붙인 것은 인증과 내 정보뿐이다.
// 헬스 운동 기록(내 기록 조회·저장), 프로필 숫자, 받은 후기, 다른 사용자 조회, 전체 알림 설정도 서버와 연결했다.
// 러닝 페이스·등산 성향은 서버에 저장할 곳이 없다.
// 아직 서버에 없는 기능(비밀번호 찾기)은
// fallback(목 저장소)이 그대로 맡는다. 기능을 붙일 때마다 아래에 override를 하나씩 더하고 fallback 의존을 줄인다.
class RealUserRepository(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokens: TokenStore,
    private val reviewApi: ReviewApi,
    private val fallback: UserRepository,
) : UserRepository by fallback {

    // 로그인하고 내 정보를 받아 온다. 내 정보를 못 받으면 로그인도 없던 일로 한다.
    override suspend fun login(email: String, password: String, keepLogin: Boolean): Result<User> =
        apiCall { authApi.login(LoginRequest(email.trim(), password)).requireData() }
            .mapCatching { tokens.save(it, keepLogin); fetchMeOrRollback() }

    // 가입과 동시에 로그인 상태가 된다. 서버 가입 API는 이름·닉네임·이메일·비밀번호·성별만 받는다.
    // 헬스 기록은 가입 직후 따로 저장한다. (실패해도 가입은 성공이다. 마이페이지에서 다시 입력할 수 있다)
    // 선호 종목(draft.preferredSports)은 서버에 저장할 곳이 없다.
    override suspend fun signUp(draft: SignUpDraft): Result<User> {
        val gender = draft.gender ?: return Result.failure(IllegalArgumentException("성별이 없어요"))
        val request = SignupRequest(
            email = draft.email.trim(),
            password = draft.password,
            name = draft.name.trim(),
            nickname = draft.nickname.trim(),
            gender = gender.name,
        )
        return apiCall { authApi.signup(request).requireData() }
            .mapCatching { tokens.save(it, keepLogin = true); fetchMeOrRollback() }
            .onSuccess {
                val gym = draft.record.toGymRecordDtos()
                if (gym.isNotEmpty()) apiCall { userApi.saveGymRecords(GymRecordsRequest(gym)) }
            }
            .map { it.copy(preferredSports = draft.preferredSports, record = draft.record) }
    }

    // 내 정보와 헬스 기록. 기록을 못 받아도 내 정보는 돌려준다.
    override suspend fun getCurrentUser(): Result<User> =
        apiCall { userApi.me().requireData() }.map { me ->
            val record = apiCall { userApi.gymProfile().requireData() }.getOrNull()?.records?.toExerciseRecord() ?: ExerciseRecord()
            me.toUser().copy(record = record)
        }

    // 헬스 기록만 서버에 저장한다. 서버는 보낸 부위가 내 기록 전체가 되므로 입력한 부위를 모두 보낸다.
    override suspend fun updateRecord(record: ExerciseRecord): Result<Unit> =
        apiCall { userApi.saveGymRecords(GymRecordsRequest(record.toGymRecordDtos())) }.map { }

    // 다른 사용자의 공개 정보. 이름·이메일·운동 기록은 공개되지 않아 비어 있다.
    override suspend fun getUser(userId: String): Result<User> {
        if (userId == tokens.userId) return getCurrentUser()
        val id = userId.toLongOrNull() ?: return Result.failure(NoSuchElementException(userId))
        return apiCall { userApi.profile(id).requireData() }.map {
            User(id = it.userId.toString(), nickname = it.nickname, mannerTemperature = it.mannerTemperature.toFloat())
        }
    }

    // 프로필 숫자(후기 수, 개설한 모임 수, 많이 받은 태그). 내 것은 내 정보에, 남의 것은 프로필 조회에 들어 있다.
    override suspend fun getProfileStats(userId: String): Result<ProfileStats> {
        if (userId == tokens.userId) return apiCall { userApi.me().requireData().stats }.map { it.toProfileStats() }
        val id = userId.toLongOrNull() ?: return Result.failure(NoSuchElementException(userId))
        return apiCall { userApi.profile(id).requireData().stats }.map { it.toProfileStats() }
    }

    // 받은 후기(최신순). 서버는 아쉬움 평가를 평가자 없이 내려주므로 '익명'으로 보여 준다.
    override suspend fun getReceivedReviews(userId: String): Result<List<ReceivedReview>> = apiCall {
        val mine = userId == tokens.userId
        val other = if (mine) 0L else userId.toLongOrNull() ?: throw NoSuchElementException(userId)
        val all = mutableListOf<ReceivedReviewDto>()
        var cursor: String? = null
        var pages = 0
        do {
            val res = (if (mine) reviewApi.myReviews(cursor, REVIEW_PAGE_SIZE) else reviewApi.userReviews(other, cursor, REVIEW_PAGE_SIZE)).requireData()
            all += res.content
            cursor = res.nextCursor
            pages++
        } while (cursor != null && pages < MAX_REVIEW_PAGES)
        all.map { r ->
            ReceivedReview(
                id = r.reviewId.toString(),
                reviewerNickname = r.evaluator?.nickname ?: "익명",
                sport = Sport.HEALTH,
                verdict = when (r.rating) { "NORMAL" -> ReviewVerdict.NORMAL; "BAD" -> ReviewVerdict.BAD; else -> ReviewVerdict.GOOD },
                comment = r.comment?.takeIf { it.isNotBlank() },
                tags = r.tags.map { it.label },
                createdAtMillis = IsoTime.parse(r.createdAt),
            )
        }.sortedByDescending { it.createdAtMillis }
    }

    override suspend fun isNotificationEnabled(): Result<Boolean> = apiCall { userApi.me().requireData().pushEnabled }

    override suspend fun setNotificationEnabled(enabled: Boolean): Result<Unit> =
        apiCall { userApi.changeNotificationSettings(NotificationSettingsRequestDto(enabled)) }.map { }

    override suspend fun isEmailAvailable(email: String): Result<Boolean> =
        apiCall { authApi.checkEmail(email.trim()).requireData().available }

    override suspend fun isNicknameAvailable(nickname: String): Result<Boolean> =
        apiCall { authApi.checkNickname(nickname.trim()).requireData().available }

    // 서버에 세션 폐기를 알리되, 실패해도 이 기기의 로그인은 반드시 푼다. (네트워크가 없어도 로그아웃은 된다)
    // 서버에 비밀번호 재설정 API가 없다. 보낸 척하지 않고 지원하지 않는다고 알린다.
    override val supportsPasswordReset: Boolean get() = false

    override suspend fun requestPasswordReset(email: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("비밀번호 찾기는 아직 준비 중이에요"))

    override suspend fun registerPushToken(token: String): Result<Unit> =
        apiCall { userApi.registerFcmToken(FcmTokenDto(token)) }.onSuccess { tokens.setPushToken(token) }.map { }

    override suspend fun logout(): Result<Unit> {
        val refreshToken = tokens.refreshToken
        // 푸시 토큰은 로그인 상태에서만 지울 수 있다. 실패해도 로그아웃은 계속한다.
        tokens.pushToken?.let { push -> apiCall { userApi.deleteFcmToken(FcmTokenDto(push)) } }
        tokens.clear()
        if (refreshToken != null) apiCall { authApi.logout(RefreshTokenRequest(refreshToken)) }
        return Result.success(Unit)
    }

    private suspend fun fetchMeOrRollback(): User {
        val me = apiCall { userApi.me().requireData() }
        me.onFailure { tokens.clear() }
        val user = me.getOrThrow().toUser()
        tokens.setUserId(user.id)
        ActivityAreaStore.switchUser(user.id) // 이 계정이 설정해 둔 활동 지역을 불러온다
        return user
    }
}

private const val REVIEW_PAGE_SIZE = 50
private const val MAX_REVIEW_PAGES = 3

internal fun StatsDto.toProfileStats() = ProfileStats(
    receivedReviewCount = reviewCount.toInt(),
    hostedMeetingCount = hostedCount.toInt(),
    topTags = topTags.map { TagCount(it.label, it.count.toInt()) },
)

internal fun MyProfileDto.toUser(): User = User(
    id = userId.toString(),
    name = name,
    nickname = nickname,
    email = email,
    gender = Gender.valueOf(gender),
    mannerTemperature = mannerTemperature.toFloat(),
    // 선호 종목과 운동 기록은 운동 프로필 API를 붙일 때 채운다.
)
