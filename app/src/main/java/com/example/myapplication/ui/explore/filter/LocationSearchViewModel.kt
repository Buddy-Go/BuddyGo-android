package com.example.myapplication.ui.explore.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationSearchViewModel(
    initialDistrict: String?,
    private val searcher: DistrictSearcher = KakaoDistrictSearcher(),
    private val debounceMs: Long = 300,
) : ViewModel() {
    private var searchJob: Job? = null

    private val _state = MutableStateFlow(LocationSearchState(selected = initialDistrict, recent = RecentDistrictStore.recent.value))
    val state: StateFlow<LocationSearchState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            RecentDistrictStore.recent.collect { recent -> _state.update { it.copy(recent = recent) } }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        search(query)
    }

    fun onClearQuery() = onQueryChange("")

    // 타이핑이 멈추면 검색한다. 이전 검색은 취소해서 늦게 온 옛 결과가 덮어쓰지 않는다.
    private fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), loading = false, failed = false) }
            return
        }
        _state.update { it.copy(loading = true, failed = false) }
        searchJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val result = runCatching { searcher.search(query.trim()) }
            if (result.exceptionOrNull() is CancellationException) throw result.exceptionOrNull()!!
            _state.update { it.copy(results = result.getOrDefault(emptyList()), loading = false, failed = result.isFailure) }
        }
    }
    // 고르면 그 지역의 중심 좌표를 기억해 둔다. 적용하면 탐색 화면의 지도가 그곳으로 옮겨 간다.
    // 같은 이름의 동이 여러 시·구에 있을 수 있어(예: 도원동) 결과에서 고를 때는 전체 이름(fullName)으로 구분한다.
    fun onSelect(name: String) {
        _state.value.results.firstOrNull { it.fullName == name }?.let { DistrictCenters.put(it.fullName, it.center) }
        _state.update { it.withToggled(name) }
    }
    fun onRemoveRecent(name: String) = RecentDistrictStore.remove(name)
    fun onReset() {
        searchJob?.cancel()
        _state.update { it.cleared().copy(results = emptyList(), loading = false, failed = false) }
    }

    // 지금 적용하면 어느 지역이 되는지. 고른 곳이 있으면 그곳, 없으면 검색어와 전체 이름이 똑같은 결과나 하나뿐인 결과를 고른 것으로 본다.
    // 검색 중인데 이것도 정할 수 없으면 null이다. (검색어가 없으면 고른 곳도 없는 것이 맞다)
    private fun resolveSelection(): String? {
        val current = _state.value
        current.selected?.let { return it }
        if (!current.isSearching || current.loading) return null
        val typed = current.query.normalizedName()
        return current.results.firstOrNull { it.fullName.normalizedName() == typed }?.fullName
            ?: current.results.singleOrNull()?.fullName
    }

    // 검색어를 쓰고 아무것도 고르지 않았는데 어느 지역인지도 정할 수 없다. 이때 적용하면 위치 조건이 말없이 풀리므로 막고 안내한다.
    val needsChoice: Boolean get() = _state.value.isSearching && resolveSelection() == null

    // 적용: 고른 곳을 최근 검색에 넣고 돌려준다. 검색어도 고른 곳도 없으면 위치 조건을 푼다(null).
    fun onApply(): String? {
        val selected = resolveSelection()
        if (selected != null) {
            _state.value.results.firstOrNull { it.fullName == selected }?.let { DistrictCenters.put(it.fullName, it.center) }
            RecentDistrictStore.add(selected)
        }
        return selected
    }
}

private fun String.normalizedName() = trim().split(Regex("\\s+")).joinToString(" ")
