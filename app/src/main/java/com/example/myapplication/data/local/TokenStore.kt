package com.example.myapplication.data.local

import com.example.myapplication.data.api.TokenDto

// 토큰을 담는 설정 파일 이름. res/xml 의 백업 규칙(backup_rules, data_extraction_rules)이 이 파일을 백업에서 뺀다.
const val SESSION_PREFS_NAME = "buddygo_session"

// 액세스·리프레시 토큰을 들고 있는 곳. 메모리에 두고, '로그인 상태 유지'일 때만 기기에도 저장한다.
// 유지하지 않으면 앱을 완전히 끌 때 로그인이 풀린다.
class TokenStore(private val persist: KeyValueStore) {
    @Volatile private var access: String? = persist.get(KEY_ACCESS).orNull()
    @Volatile private var refresh: String? = persist.get(KEY_REFRESH).orNull()
    @Volatile private var keep: Boolean = persist.get(KEY_KEEP) == "1"
    @Volatile private var user: String? = persist.get(KEY_USER).orNull()

    // 로그인한 사용자의 id. 모임 응답이 '내가 누구인지'를 id로 알려 주지 않아서 앱이 따로 들고 있는다.
    val userId: String? get() = user

    // 서버에 등록한 푸시 토큰. 로그아웃할 때 서버에서 지우려고 기억한다. ('로그인 상태 유지'와 상관없이 기기에 남긴다)
    @Volatile private var push: String? = persist.get(KEY_PUSH).orNull()
    val pushToken: String? get() = push

    @Synchronized
    fun setPushToken(token: String?) {
        push = token
        persist.put(KEY_PUSH, token.orEmpty())
    }

    val accessToken: String? get() = access
    val refreshToken: String? get() = refresh

    // 로그인한 적이 있고 아직 로그아웃·만료되지 않았다.
    val hasSession: Boolean get() = refresh != null

    @Synchronized
    fun save(tokens: TokenDto, keepLogin: Boolean) {
        keep = keepLogin
        access = tokens.accessToken
        refresh = tokens.refreshToken
        write()
    }

    // 토큰 재발급 결과를 반영한다. '로그인 상태 유지' 선택은 그대로 둔다.
    @Synchronized
    fun update(tokens: TokenDto) {
        access = tokens.accessToken
        refresh = tokens.refreshToken
        write()
    }

    @Synchronized
    fun setUserId(id: String) {
        user = id
        write()
    }

    @Synchronized
    fun clear() {
        access = null
        refresh = null
        user = null
        keep = false
        push = null
        persist.put(KEY_PUSH, "")
        write()
    }

    private fun write() {
        persist.put(KEY_ACCESS, if (keep) access.orEmpty() else "")
        persist.put(KEY_REFRESH, if (keep) refresh.orEmpty() else "")
        persist.put(KEY_KEEP, if (keep) "1" else "")
        persist.put(KEY_USER, if (keep) user.orEmpty() else "")
    }

    private fun String?.orNull() = this?.takeIf { it.isNotBlank() }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_KEEP = "keep_login"
        const val KEY_USER = "user_id"
        const val KEY_PUSH = "push_token"
    }
}
