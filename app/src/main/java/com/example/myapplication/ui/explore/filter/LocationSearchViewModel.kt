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
    fun onSelect(name: String) = _state.update { it.withToggled(name) }
    fun onRemoveRecent(name: String) = RecentDistrictStore.remove(name)
    fun onReset() {
        searchJob?.cancel()
        _state.update { it.cleared().copy(results = emptyList(), loading = false, failed = false) }
    }

    // 적용: 고른 곳을 최근 검색에 넣고 돌려준다. 고른 곳이 없으면 위치 조건을 푼다(null).
    fun onApply(): String? {
        val selected = _state.value.selected
        if (selected != null) RecentDistrictStore.add(selected)
        return selected
    }
}
