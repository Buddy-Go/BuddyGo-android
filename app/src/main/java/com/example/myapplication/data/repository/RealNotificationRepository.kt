package com.example.myapplication.data.repository

import com.example.myapplication.data.api.IsoTime
import com.example.myapplication.data.api.NotificationApi
import com.example.myapplication.data.api.NotificationDto
import com.example.myapplication.data.api.apiCall
import com.example.myapplication.data.api.requireData
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.NotificationType

// 서버(BuddyGo-server)와 연결한 알림 저장소.
//  - 서버 알림에는 굵게 보일 앞부분(subject)이 따로 없어 message 한 줄만 보여 준다. (subject = null)
//  - 서버가 새로 만든 알림 종류를 앱이 모르면 그 알림은 목록에서 뺀다.
class RealNotificationRepository(private val api: NotificationApi) : NotificationRepository {

    override suspend fun getNotifications(): Result<List<Notification>> = apiCall {
        val all = mutableListOf<NotificationDto>()
        var page = 0
        while (page < MAX_PAGES) {
            val res = api.list(page, PAGE_SIZE).requireData()
            all += res.content
            if (!res.hasNext) break
            page++
        }
        all.mapNotNull { it.toNotification() }.sortedByDescending { it.createdAtMillis }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        val id = notificationId.toLongOrNull() ?: return Result.failure(NoSuchElementException(notificationId))
        return apiCall { api.read(id) }.map { }
    }

    override suspend fun markAllAsRead(): Result<Unit> = apiCall { api.readAll() }.map { }

    private fun NotificationDto.toNotification(): Notification? {
        val appType = type.toNotificationType() ?: return null
        // 공지 알림을 누르면 공지 화면으로 가는데, 공지 id만으로는 모임을 알 수 없어서 여기서 적어 둔다.
        if (noticeId != null && gatheringId != null) NoticeIndex.putNotice(noticeId.toString(), gatheringId.toString())
        return Notification(
            id = notificationId.toString(),
            type = appType,
            subject = null,
            message = message,
            meetingId = gatheringId?.toString(),
            noticeId = noticeId?.toString(),
            userId = actorId?.toString(),
            createdAtMillis = IsoTime.parse(createdAt),
            isRead = read,
        )
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_PAGES = 3
    }
}

// 서버 알림 종류 → 앱 알림 종류
internal fun String.toNotificationType(): NotificationType? = when (this) {
    "APPLICATION_RECEIVED" -> NotificationType.JOIN_REQUESTED
    "APPLICATION_ACCEPTED" -> NotificationType.JOIN_ACCEPTED
    "APPLICATION_REJECTED" -> NotificationType.JOIN_REJECTED
    "GATHERING_CLOSED" -> NotificationType.MEETING_CLOSED
    "MEMBER_LEFT_REOPENED" -> NotificationType.MEMBER_LEFT_BEFORE_DEADLINE
    "GATHERING_CANCELED_MEMBER_LEFT" -> NotificationType.MEMBER_LEFT_AFTER_DEADLINE
    "GATHERING_CANCELED_UNDER_CAPACITY" -> NotificationType.MEETING_CANCELED_UNDERFILLED
    "GATHERING_CANCELED_BY_HOST" -> NotificationType.MEETING_CANCELED_BY_HOST
    "CHECK_IN_OPENED" -> NotificationType.CHECKIN_OPENED
    "GATHERING_STARTED" -> NotificationType.MEETING_STARTED
    "GATHERING_FINISHED" -> NotificationType.MEETING_ENDED
    "NOTICE_CREATED", "NOTICE_UPDATED" -> NotificationType.NOTICE_WRITTEN
    "NOTICE_COMMENT_CREATED" -> NotificationType.NOTICE_COMMENTED // 호스트·댓글 단 참가자에게 같은 종류로 온다
    "BUDDY_REQUEST_RECEIVED" -> NotificationType.MATE_REQUESTED
    "BUDDY_REQUEST_ACCEPTED" -> NotificationType.MATE_ACCEPTED
    "BUDDY_GATHERING_CREATED" -> NotificationType.MATE_MEETING_CREATED
    "BUDDY_GATHERING_APPLIED" -> NotificationType.MATE_MEETING_JOINED
    else -> null
}
