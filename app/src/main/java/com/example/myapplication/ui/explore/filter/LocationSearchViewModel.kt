package com.example.myapplication.ui.explore.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationSearchViewModel(initialDistrict: String?) : ViewModel() {
    private val _state = MutableStateFlow(LocationSearchState(selected = initialDistrict, recent = RecentDistrictStore.recent.value))
    val state: StateFlow<LocationSearchState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            RecentDistrictStore.recent.collect { recent -> _state.update { it.copy(recent = recent) } }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }
    fun onClearQuery() = _state.update { it.copy(query = "") }
    fun onSelect(name: String) = _state.update { it.withToggled(name) }
    fun onRemoveRecent(name: String) = RecentDistrictStore.remove(name)
    fun onReset() = _state.update { it.cleared() }

    // 적용: 고른 곳을 최근 검색에 넣고 돌려준다. 고른 곳이 없으면 위치 조건을 푼다(null).
    fun onApply(): String? {
        val selected = _state.value.selected
        if (selected != null) RecentDistrictStore.add(selected)
        return selected
    }
}
