package com.example.myapplication.data.repository

import com.example.myapplication.data.model.User

interface UserRepository {
    suspend fun login(email: String, password: String, keepLogin: Boolean): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun getUser(userId: String): Result<User>

    // 회원가입 중복 검사 (service-rules 1장). true = 사용할 수 있음
    suspend fun isNicknameAvailable(nickname: String): Result<Boolean>
    suspend fun isEmailAvailable(email: String): Result<Boolean>
}
