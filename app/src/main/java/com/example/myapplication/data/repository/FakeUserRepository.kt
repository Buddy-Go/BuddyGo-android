package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockUsers
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
}
