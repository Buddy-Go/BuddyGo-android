package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.SignUpDraft
import com.example.myapplication.data.model.User

class FakeUserRepository : UserRepository {
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
}
