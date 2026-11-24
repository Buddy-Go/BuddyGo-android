package com.example.myapplication.push

import android.app.Activity
import android.app.Application
import android.os.Bundle

// 앱이 화면에 보이는 중인지(화면 하나라도 시작된 상태인지) 알려 준다.
// 푸시가 왔을 때 앱을 보고 있으면 시스템 알림 대신 앱 안 배너만 쓰기 위해 쓴다.
object AppForeground {
    @Volatile private var startedCount = 0
    val isForeground: Boolean get() = startedCount > 0

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { startedCount++ }
            override fun onActivityStopped(activity: Activity) { startedCount = (startedCount - 1).coerceAtLeast(0) }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
