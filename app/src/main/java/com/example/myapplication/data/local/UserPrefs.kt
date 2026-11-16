package com.example.myapplication.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 내 활동 지역(마이페이지 '위치 설정'). 구·동 하나. 저장된 값이 없으면 기본 지역으로 시작한다.
// 쓰임새(추천 순서 등)는 서버가 정해지면 붙인다. 지금은 마이페이지 표시와 위치 설정 화면의 처음 값으로만 쓴다.
// 서버에 저장할 곳이 없어 기기에 계정별로 저장한다. 로그아웃하면 화면에서만 기본값으로 돌리고,
// 같은 계정으로 다시 로그인하면 그 계정이 설정해 둔 지역을 다시 불러온다. 다른 사람에게는 보이지 않는다.
object ActivityAreaStore {
    private const val KEY_PREFIX = "activity_area:"
    const val DEFAULT_AREA = "마포구"

    private var store: KeyValueStore? = null
    private var userId: String? = null
    private val _area = MutableStateFlow<String?>(DEFAULT_AREA)
    val area: StateFlow<String?> = _area.asStateFlow()

    // 앱을 켤 때. 로그인이 유지된 상태면 그 계정의 지역을 불러온다.
    fun attach(store: KeyValueStore, userId: String?) {
        this.store = store
        switchUser(userId)
    }

    // 로그인한 계정이 바뀔 때(로그인 성공: 그 계정 id, 로그아웃: null).
    fun switchUser(userId: String?) {
        this.userId = userId
        val saved = userId?.let { store?.get(KEY_PREFIX + it) }
        _area.value = when {
            saved == null -> DEFAULT_AREA // 로그아웃했거나 이 계정은 아직 지역을 고른 적이 없다
            saved.isBlank() -> null // 지역을 해제해 둔 계정
            else -> saved
        }
    }

    // 빈 값이면 지역을 해제한다. 로그인한 계정이 있을 때만 기기에 남긴다.
    fun set(area: String?) {
        _area.value = area?.takeIf { it.isNotBlank() }
        userId?.let { store?.put(KEY_PREFIX + it, _area.value.orEmpty()) }
    }

    // 로그아웃: 화면에서는 기본값으로 돌리되, 계정에 저장한 값은 지우지 않는다.
    fun reset() = switchUser(null)
}

data class BlockedUser(val userId: String, val nickname: String)

// 차단한 사람. 메이트 목록·함께 운동한 사람에서 빼고, 마이페이지 '신고 · 차단 관리'에서 풀 수 있다.
object BlockedUsersStore {
    private const val KEY = "blocked_users"

    private var store: KeyValueStore? = null
    private val _blocked = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blocked: StateFlow<List<BlockedUser>> = _blocked.asStateFlow()

    fun attach(store: KeyValueStore) {
        this.store = store
        store.get(KEY)?.let { _blocked.value = decode(it) }
    }

    fun isBlocked(userId: String): Boolean = _blocked.value.any { it.userId == userId }

    fun block(userId: String, nickname: String) = change { list ->
        if (list.any { it.userId == userId }) list else list + BlockedUser(userId, nickname)
    }

    fun unblock(userId: String) = change { list -> list.filterNot { it.userId == userId } }

    fun clear() = change { emptyList() }

    private fun change(transform: (List<BlockedUser>) -> List<BlockedUser>) {
        _blocked.update(transform)
        store?.put(KEY, encode(_blocked.value))
    }

    // 한 줄에 한 명: 아이디와 닉네임을 탭으로 나눈다.
    internal fun encode(list: List<BlockedUser>): String =
        list.joinToString("\n") { "${it.userId.clean()}\t${it.nickname.clean()}" }

    internal fun decode(text: String): List<BlockedUser> = text.lines().mapNotNull { line ->
        val c = line.split('\t')
        if (c.size < 2 || c[0].isBlank()) null else BlockedUser(c[0], c[1])
    }

    private fun String.clean() = replace('\n', ' ').replace('\t', ' ')
}
