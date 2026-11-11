package com.example.myapplication.ui.meeting.create

import com.example.myapplication.data.local.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 장소 검색(10g) → 장소 확인(10g-3) → 개설·수정 화면으로 장소를 주고받는 곳. 화면 사이에는 값을 큰 덩어리로 넘기지 않는다.
// attach로 저장소를 붙이면 최근 장소가 앱을 다시 켜도 남는다. 저장된 값이 없는 첫 실행에만 예시 장소로 시작한다.
object PlaceSelectionStore {
    private const val KEY = "recent_places"

    // 지금 장소를 고르는 모임의 종목. 헬스는 가게 위치 그대로(10g-3), 러닝·등산은 지도에서 집결지를 지정한다(10g-4).
    var sport: com.example.myapplication.data.model.Sport = com.example.myapplication.data.model.Sport.HEALTH
    private var store: KeyValueStore? = null

    // 검색 결과에서 고른 장소. 장소 확인 화면이 읽는다.
    private val _pending = MutableStateFlow<PlaceResult?>(null)
    val pending: StateFlow<PlaceResult?> = _pending.asStateFlow()

    // 장소 확인에서 '이 장소로 설정'한 결과. 개설·수정 화면이 받아 가면 비운다.
    private val _confirmed = MutableStateFlow<PlaceSelection?>(null)
    val confirmed: StateFlow<PlaceSelection?> = _confirmed.asStateFlow()

    private val _recent = MutableStateFlow<List<PlaceResult>>(emptyList())
    val recent: StateFlow<List<PlaceResult>> = _recent.asStateFlow()

    // 앱 시작 때 한 번 부른다. 저장된 값이 있으면(비어 있는 목록도 저장된 값이다) 그것으로 바꾼다.
    fun attach(store: KeyValueStore) {
        this.store = store
        store.get(KEY)?.let { _recent.value = decodePlaces(it) }
    }

    private fun saveRecent() { store?.put(KEY, encodePlaces(_recent.value)) }

    fun select(place: PlaceResult) { _pending.value = place }

    fun confirm(selection: PlaceSelection) {
        _confirmed.value = selection
        _recent.update { it.withRecent(selection.toResult()) }
        saveRecent()
        _pending.value = null
    }

    fun consumeConfirmed() { _confirmed.value = null }

    // 로그아웃: 고르던 장소와 최근 장소를 비운다. 저장소가 붙어 있으면 저장된 값도 빈 목록이 된다.
    fun reset() {
        _pending.value = null
        _confirmed.value = null
        _recent.value = emptyList()
        saveRecent()
    }
    fun removeRecent(place: PlaceResult) {
        _recent.update { list -> list.filterNot { it.name == place.name && it.address == place.address } }
        saveRecent()
    }
}

// 최근 장소: 같은 장소는 앞으로 올리고 최대 RECENT_MAX개만 둔다.
fun List<PlaceResult>.withRecent(place: PlaceResult, max: Int = 5): List<PlaceResult> =
    (listOf(place) + filterNot { it.name == place.name && it.address == place.address }).take(max)

// 최근 장소를 글자로 바꾼다. 한 줄에 한 곳: 이름, 도로명 주소, 지번, 위도, 경도를 탭으로 나눈다. (거리는 매번 달라져 저장하지 않는다)
fun encodePlaces(list: List<PlaceResult>): String = list.joinToString("\n") { p ->
    listOf(p.name, p.address, p.lotAddress).joinToString("\t") { it.replace('\n', ' ').replace('\t', ' ') } +
        "\t${p.latitude}\t${p.longitude}"
}

// 깨진 줄(칸 수가 모자라거나 좌표가 숫자가 아님)은 건너뛴다. 저장된 값이 망가져도 앱이 죽지 않게 한다.
fun decodePlaces(text: String?): List<PlaceResult> = text.orEmpty().lines().mapNotNull { line ->
    val c = line.split('\t')
    if (c.size < 5) return@mapNotNull null
    val lat = c[3].toDoubleOrNull() ?: return@mapNotNull null
    val lng = c[4].toDoubleOrNull() ?: return@mapNotNull null
    if (c[0].isBlank()) null else PlaceResult(c[0], c[1], c[2], lat, lng)
}
