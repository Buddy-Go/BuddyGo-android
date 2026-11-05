package com.example.myapplication.data.repository

import com.example.myapplication.data.model.User

interface UserRepository {
    suspend fun login(email: String, password: String, keepLogin: Boolean): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun getUser(userId: String): Result<User>
}
