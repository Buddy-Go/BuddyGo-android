package com.example.myapplication

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class BuddyGoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 키가 비어 있어도 앱은 뜨고, 지도 화면만 그려지지 않는다.
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}
