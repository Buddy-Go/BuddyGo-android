package com.example.myapplication.data.repository

import com.example.myapplication.data.model.SignUpDraft
import com.example.myapplication.data.model.User

// TODO(임시) 서버가 준비되면 UserApi로 연결한다.
class RealUserRepository : UserRepository {
    override suspend fun login(email: String, password: String, keepLogin: Boolean): Result<User> = TODO("서버 연결 후 구현")
    override suspend fun getCurrentUser(): Result<User> = TODO("서버 연결 후 구현")
    override suspend fun getUser(userId: String): Result<User> = TODO("서버 연결 후 구현")
    override suspend fun isNicknameAvailable(nickname: String): Result<Boolean> = TODO("서버 연결 후 구현")
    override suspend fun isEmailAvailable(email: String): Result<Boolean> = TODO("서버 연결 후 구현")
    override suspend fun signUp(draft: SignUpDraft): Result<User> = TODO("서버 연결 후 구현")
}
