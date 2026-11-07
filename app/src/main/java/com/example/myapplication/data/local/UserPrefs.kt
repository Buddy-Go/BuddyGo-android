package com.example.myapplication.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 내 활동 지역(마이페이지 '위치 설정'). 구·동 하나. 저장된 값이 없으면 기본 지역으로 시작한다.
// 쓰임새(추천 순서 등)는 서버가 정해지면 붙인다. 지금은 마이페이지 표시와 위치 설정 화면의 처음 값으로만 쓴다.
object ActivityAreaStore {
    private const val KEY = "activity_area"
    const val DEFAULT_AREA = "마포구"

    private var store: KeyValueStore? = null
    private val _area = MutableStateFlow<String?>(DEFAULT_AREA)
    val area: StateFlow<String?> = _area.asStateFlow()

    fun attach(store: KeyValueStore) {
        this.store = store
        store.get(KEY)?.let { _area.value = it.ifBlank { null } }
    }

    // 빈 값이면 지역을 해제한다.
    fun set(area: String?) {
        _area.value = area?.takeIf { it.isNotBlank() }
        store?.put(KEY, _area.value.orEmpty())
    }

    fun reset() = set(DEFAULT_AREA)
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
