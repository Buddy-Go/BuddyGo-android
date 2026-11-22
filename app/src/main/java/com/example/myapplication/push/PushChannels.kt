package com.example.myapplication.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

// 알림 채널. 중요도를 '높음'으로 해야 알림이 화면 위에서 내려오는 배너(헤드업)로 뜬다.
// 채널 중요도는 한 번 만들면 바꿀 수 없어서, 예전 '기본' 채널(buddygo_default) 대신 새 채널을 쓴다.
// 앱이 꺼져 있거나 뒤에 있을 때 오는 푸시도 이 채널로 오도록 AndroidManifest의
// com.google.firebase.messaging.default_notification_channel_id 에 같은 id를 넣었다.
object PushChannels {
    const val ALERTS = "buddygo_alerts"
    private const val OLD_DEFAULT = "buddygo_default"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(ALERTS, "BuddyGo 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "신청·수락·공지·메이트 신청 같은 소식을 화면 위 배너로 알려줘요"
            },
        )
        manager.deleteNotificationChannel(OLD_DEFAULT) // 배너가 뜨지 않던 예전 채널은 지운다
    }
}
