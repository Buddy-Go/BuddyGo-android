package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Notification

// TODO(임시) 서버가 준비되면 연결한다.
class RealNotificationRepository : NotificationRepository {
    override suspend fun getNotifications(): Result<List<Notification>> = TODO("서버 연결 후 구현")
    override suspend fun markAsRead(notificationId: String): Result<Unit> = TODO("서버 연결 후 구현")
    override suspend fun markAllAsRead(): Result<Unit> = TODO("서버 연결 후 구현")
}
