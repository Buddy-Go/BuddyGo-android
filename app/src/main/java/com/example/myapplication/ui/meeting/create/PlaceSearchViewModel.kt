package com.example.myapplication.ui.meeting.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ui.explore.GeoPoint
import com.example.myapplication.ui.explore.LastKnownLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 10g 장소 검색, 10g-2 검색 결과
data class PlaceSearchState(
    val query: String = "",
    val results: List<PlaceResult> = emptyList(),
    val recent: List<PlaceResult> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val searchedNearby: Boolean = false, // '현재 위치 주변에서 찾기' 결과인지
) {
    // 검색어를 입력했거나 주변 찾기를 눌렀으면 결과를 보이고, 아니면 최근 장소를 보인다.
    val showsResults: Boolean get() = query.isNotBlank() || searchedNearby
    val isEmptyResult: Boolean get() = showsResults && !loading && !failed && results.isEmpty()
}

class PlaceSearchViewModel(
    private val searcher: PlaceSearcher = KakaoPlaceSearcher(),
    private val debounceMs: Long = 300,
) : ViewModel() {
    private var job: Job? = null
    private val _state = MutableStateFlow(PlaceSearchState(recent = PlaceSelectionStore.recent.value))
    val state: StateFlow<PlaceSearchState> = _state.asStateFlow()

    init {
        viewModelScope.launch { PlaceSelectionStore.recent.collect { r -> _state.update { it.copy(recent = r) } } }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, searchedNearby = false) }
        run(query, debounce = true, near = LastKnownLocation.point.value)
    }

    fun onClearQuery() = onQueryChange("")

    // '현재 위치 주변에서 찾기': 내 위치 가까운 헬스장을 찾는다. 위치를 못 얻으면(null) 실패로 안내한다.
    fun onNearby(location: GeoPoint?) {
        if (location == null) {
            _state.update { it.copy(failed = true, searchedNearby = true, results = emptyList(), loading = false, query = "") }
            return
        }
        LastKnownLocation.set(location)
        _state.update { it.copy(query = "", searchedNearby = true) }
        run(NEARBY_QUERY, debounce = false, near = location)
    }

    fun onSelect(place: PlaceResult) = PlaceSelectionStore.select(place)
    fun onRemoveRecent(place: PlaceResult) = PlaceSelectionStore.removeRecent(place)

    // 타이핑이 멈추면 검색한다. 이전 검색은 취소해서 늦게 온 옛 결과가 덮어쓰지 않는다.
    private fun run(query: String, debounce: Boolean, near: GeoPoint?) {
        job?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), loading = false, failed = false) }
            return
        }
        _state.update { it.copy(loading = true, failed = false) }
        job = viewModelScope.launch {
            if (debounce && debounceMs > 0) delay(debounceMs)
            val result = runCatching { searcher.search(query.trim(), near) }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            _state.update { it.copy(results = result.getOrDefault(emptyList()), loading = false, failed = result.isFailure) }
        }
    }

    companion object {
        const val NEARBY_QUERY = "헬스장"
    }
}
