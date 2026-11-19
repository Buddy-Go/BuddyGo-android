package com.example.myapplication.push

import com.example.myapplication.data.api.Api
import com.example.myapplication.data.repository.Repositories
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// FCM 토큰을 받아 서버에 등록한다. 로그인한 상태에서만 등록하고, 실패해도 앱 동작에는 영향이 없다.
// 서버는 같은 토큰을 다시 받으면 이 계정으로 옮기기만 하므로 앱을 켤 때마다 불러도 된다.
object PushRegistrar {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun sync() {
        if (!Api.isInitialized || !Api.tokens.hasSession) return
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> register(token) }
        }
    }

    // 토큰이 새로 발급·갱신됐을 때(BuddyGoMessagingService)도 이걸 쓴다.
    fun register(token: String) {
        if (token.isBlank() || !Api.isInitialized || !Api.tokens.hasSession) return
        scope.launch { Repositories.users.registerPushToken(token) }
    }
}
