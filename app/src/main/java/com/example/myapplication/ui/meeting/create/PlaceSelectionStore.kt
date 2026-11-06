package com.example.myapplication.ui.meeting.create

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 장소 검색(10g) → 장소 확인(10g-3) → 개설·수정 화면으로 장소를 주고받는 곳. 화면 사이에는 값을 큰 덩어리로 넘기지 않는다.
// TODO(임시) 최근 장소는 메모리에만 둔다. 앱을 다시 켜도 남기려면 저장소를 붙인다.
object PlaceSelectionStore {
    private const val RECENT_MAX = 5

    // 검색 결과에서 고른 장소. 장소 확인 화면이 읽는다.
    private val _pending = MutableStateFlow<PlaceResult?>(null)
    val pending: StateFlow<PlaceResult?> = _pending.asStateFlow()

    // 장소 확인에서 '이 장소로 설정'한 결과. 개설·수정 화면이 받아 가면 비운다.
    private val _confirmed = MutableStateFlow<PlaceSelection?>(null)
    val confirmed: StateFlow<PlaceSelection?> = _confirmed.asStateFlow()

    private val _recent = MutableStateFlow(
        listOf(SamplePlaces.all[0], SamplePlaces.all[4]),
    )
    val recent: StateFlow<List<PlaceResult>> = _recent.asStateFlow()

    fun select(place: PlaceResult) { _pending.value = place }

    fun confirm(selection: PlaceSelection) {
        _confirmed.value = selection
        _recent.update { it.withRecent(selection.toResult()) }
        _pending.value = null
    }

    fun consumeConfirmed() { _confirmed.value = null }
    fun removeRecent(place: PlaceResult) = _recent.update { list -> list.filterNot { it.name == place.name && it.address == place.address } }
}

// 최근 장소: 같은 장소는 앞으로 올리고 최대 RECENT_MAX개만 둔다.
fun List<PlaceResult>.withRecent(place: PlaceResult, max: Int = 5): List<PlaceResult> =
    (listOf(place) + filterNot { it.name == place.name && it.address == place.address }).take(max)
