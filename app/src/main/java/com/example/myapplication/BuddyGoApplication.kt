package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.local.ActivityAreaStore
import com.example.myapplication.data.local.AppStorage
import com.example.myapplication.data.local.BlockedUsersStore
import com.example.myapplication.ui.explore.filter.RecentDistrictStore
import com.example.myapplication.ui.meeting.create.PlaceSelectionStore
import com.kakao.vectormap.KakaoMapSdk

class BuddyGoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 키가 비어 있어도 앱은 뜨고, 지도 화면만 그려지지 않는다.
        runCatching { KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY) }
        // 최근 검색 지역과 최근 장소를 앱을 껐다 켜도 남긴다.
        AppStorage.init(this)
        RecentDistrictStore.attach(AppStorage.store)
        PlaceSelectionStore.attach(AppStorage.store)
        ActivityAreaStore.attach(AppStorage.store)
        BlockedUsersStore.attach(AppStorage.store)
    }
}
