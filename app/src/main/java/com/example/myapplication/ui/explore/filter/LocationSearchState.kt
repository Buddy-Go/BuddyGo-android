package com.example.myapplication.ui.explore.filter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 위치 검색 화면 상태. 구·동은 하나만 고르고, '적용'을 눌러야 조건 설정에 넘어간다.
data class LocationSearchState(
    val query: String = "",
    val selected: String? = null,
    val recent: List<String> = emptyList(),
) {
    val isSearching: Boolean get() = query.isNotBlank()
    val results: List<District> get() = DistrictCatalog.search(query)

    // 고른 곳을 다시 누르면 해제된다.
    fun withToggled(name: String) = copy(selected = if (selected == name) null else name)

    // 초기화: 고른 곳과 검색어를 모두 비운다. (최근 검색은 남긴다)
    fun cleared() = copy(query = "", selected = null)
}

// 최근 검색 구·동. 화면이 닫혀도 남도록 앱 안에 하나만 둔다.
// TODO(임시) 지금은 메모리에만 둔다. 앱을 다시 켜도 남기려면 저장소를 붙인다.
object RecentDistrictStore {
    private val _recent = MutableStateFlow(listOf("마포구", "강북구", "종로구", "영등포구"))
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    fun add(name: String) = _recent.update { it.withRecent(name) }
    fun remove(name: String) = _recent.update { it - name }
    fun clear() { _recent.value = emptyList() }
}
