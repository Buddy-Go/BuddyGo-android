package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.ProfileStats
import com.example.myapplication.data.model.ReceivedReview
import com.example.myapplication.data.model.SignUpDraft
import com.example.myapplication.data.model.User

class FakeUserRepository(
    private val stats: Map<String, ProfileStats> = mapOf(MockUsers.me.id to MockUsers.meStats),
    private val reviews: Map<String, List<ReceivedReview>> = mapOf(MockUsers.me.id to MockUsers.meReviews),
) : UserRepository {
    private var notificationEnabled = true

    override suspend fun login(email: String, password: String, keepLogin: Boolean): Result<User> =
        Result.success(MockUsers.me)

    override suspend fun getCurrentUser(): Result<User> = Result.success(MockUsers.me)

    override suspend fun getUser(userId: String): Result<User> =
        MockUsers.all.firstOrNull { it.id == userId }
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException(userId))

    override suspend fun isNicknameAvailable(nickname: String): Result<Boolean> =
        Result.success(MockUsers.all.none { it.nickname == nickname.trim() })

    override suspend fun isEmailAvailable(email: String): Result<Boolean> =
        Result.success(MockUsers.all.none { it.email.equals(email.trim(), ignoreCase = true) })

    // 서버 규칙(중복 등)은 흉내 내지 않고, 입력값으로 사용자를 만들어 돌려준다.
    override suspend fun signUp(draft: SignUpDraft): Result<User> = Result.success(
        User(
            id = "u-new", name = draft.name, nickname = draft.nickname, email = draft.email,
            gender = draft.gender ?: com.example.myapplication.data.model.Gender.MALE,
            preferredSports = draft.preferredSports, record = draft.record,
        ),
    )

    // 기록이 없는 사용자는 0과 빈 목록이다.
    override suspend fun getProfileStats(userId: String): Result<ProfileStats> =
        Result.success(stats[userId] ?: ProfileStats())

    override suspend fun getReceivedReviews(userId: String): Result<List<ReceivedReview>> =
        Result.success((reviews[userId] ?: emptyList()).sortedByDescending { it.createdAtMillis })

    override suspend fun isNotificationEnabled(): Result<Boolean> = Result.success(notificationEnabled)

    override suspend fun setNotificationEnabled(enabled: Boolean): Result<Unit> {
        notificationEnabled = enabled
        return Result.success(Unit)
    }

    override suspend fun logout(): Result<Unit> = Result.success(Unit)
}
