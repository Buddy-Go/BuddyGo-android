package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.local.KeyValueStore
import com.example.myapplication.data.local.decodeStrings
import com.example.myapplication.data.local.encodeStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 위치 검색 화면 상태. 구·동은 하나만 고르고, '적용'을 눌러야 조건 설정에 넘어간다.
data class LocationSearchState(
    val query: String = "",
    val selected: String? = null,
    val recent: List<String> = emptyList(),
    val results: List<District> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
) {
    val isSearching: Boolean get() = query.isNotBlank()

    // 고른 곳을 다시 누르면 해제된다.
    fun withToggled(name: String) = copy(selected = if (selected == name) null else name)

    // 초기화: 고른 곳과 검색어를 모두 비운다. (최근 검색은 남긴다)
    fun cleared() = copy(query = "", selected = null)
}

// 최근 검색 구·동. 화면이 닫혀도 남도록 앱 안에 하나만 둔다.
// attach로 저장소를 붙이면 앱을 다시 켜도 남는다. 처음에는 비어 있다.
object RecentDistrictStore {
    private const val KEY = "recent_districts"
    private val SEED = emptyList<String>()

    private var store: KeyValueStore? = null
    private val _recent = MutableStateFlow(SEED)
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    // 앱 시작 때 한 번 부른다. 저장된 값이 있으면(비어 있는 목록도 저장된 값이다) 그것으로 바꾼다.
    fun attach(store: KeyValueStore) {
        this.store = store
        store.get(KEY)?.let { _recent.value = decodeStrings(it) }
    }

    fun add(name: String) = change { it.withRecent(name) }
    fun remove(name: String) = change { it - name }
    fun clear() = change { emptyList() }

    private fun change(transform: (List<String>) -> List<String>) {
        _recent.update(transform)
        store?.put(KEY, encodeStrings(_recent.value))
    }
}
