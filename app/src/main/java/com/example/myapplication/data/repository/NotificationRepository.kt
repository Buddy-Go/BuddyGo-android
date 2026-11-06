package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Notification

// 알림 목록과 읽음 처리 (service-rules 12장). 알림이 만들어지는 시점과 대상은 서버가 정하고 앱은 받아서 보여 준다.
interface NotificationRepository {
    // 최신순 알림 목록
    suspend fun getNotifications(): Result<List<Notification>>

    // 알림을 읽음으로 바꾼다. 이미 읽은 알림에 다시 보내도 결과는 같다.
    suspend fun markAsRead(notificationId: String): Result<Unit>
}
