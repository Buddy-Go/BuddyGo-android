package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockNotifications
import com.example.myapplication.data.model.Notification

// 목데이터로 응답한다. 읽음 처리만 메모리에서 반영한다.
class FakeNotificationRepository(
    initial: List<Notification> = MockNotifications.all,
) : NotificationRepository {
    private val notifications = initial.toMutableList()

    override suspend fun getNotifications(): Result<List<Notification>> =
        Result.success(notifications.sortedByDescending { it.createdAtMillis })

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index < 0) return Result.failure(NoSuchElementException(notificationId))
        notifications[index] = notifications[index].copy(isRead = true)
        return Result.success(Unit)
    }

    override suspend fun markAllAsRead(): Result<Unit> {
        notifications.replaceAll { it.copy(isRead = true) }
        return Result.success(Unit)
    }
}
