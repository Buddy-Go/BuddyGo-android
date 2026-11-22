package com.example.myapplication.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BuddyGoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushRegistrar.register(token)
    }

    // 앱이 켜져 있을 때 온 푸시는 FCM이 알림을 띄워 주지 않아서 직접 띄운다.
    // (앱이 꺼져 있거나 뒤에 있으면 notification 페이로드는 시스템이 띄운다)
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        show(title, body)
    }

    private fun show(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        PushChannels.ensure(this)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // 중요도 높음 채널 + 높은 우선순위: 앱을 쓰는 중에도 화면 위에서 배너로 내려온다 (안드로이드 7 이하는 우선순위로 정한다)
        val notification = NotificationCompat.Builder(this, PushChannels.ALERTS)
            .setSmallIcon(R.drawable.ic_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
