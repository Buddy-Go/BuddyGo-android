package com.example.myapplication.data.api

import android.content.Context
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.local.SESSION_PREFS_NAME
import com.example.myapplication.data.local.SessionEvents
import com.example.myapplication.data.local.SharedPrefsKeyValueStore
import com.example.myapplication.data.local.TokenStore

// 앱 전체가 같이 쓰는 서버 통신. BuddyGoApplication에서 init을 한 번 부른다.
object Api {
    private var _tokens: TokenStore? = null
    private var _client: ApiClient? = null

    // JVM 단위 테스트처럼 앱이 뜨지 않은 곳에서는 false다.
    val isInitialized: Boolean get() = _client != null

    val tokens: TokenStore get() = checkNotNull(_tokens) { "Api.init(context)를 먼저 불러야 해요" }
    val client: ApiClient get() = checkNotNull(_client) { "Api.init(context)를 먼저 불러야 해요" }

    fun init(context: Context) {
        val store = TokenStore(SharedPrefsKeyValueStore(context, name = SESSION_PREFS_NAME))
        _tokens = store
        _client = ApiClient(
            baseUrl = BuildConfig.API_BASE_URL,
            tokens = store,
            onSessionExpired = SessionEvents::notifyExpired,
            debugLog = BuildConfig.DEBUG,
        )
    }
}
