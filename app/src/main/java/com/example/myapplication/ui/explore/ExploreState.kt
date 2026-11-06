package com.example.myapplication.ui.explore

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.explore.filter.MeetingFilter
import com.example.myapplication.ui.common.UiState

data class ExploreState(
    val meetings: UiState<List<Meeting>> = UiState.Loading,
    val sheetExpanded: Boolean = true,   // 3-After(올림) / 3-2-After(내림)
    val selectedPinKey: String? = null,
    val favoriteOnly: Boolean = false,   // 상단 '찜' 칩
    // 적용된 검색 조건. 상단 종목 칩과 조건 설정 화면이 같은 값을 쓴다. (FilterStore)
    val filter: MeetingFilter = MeetingFilter(),
    // 사용자가 지도를 움직인 뒤 '이 지역 재검색'을 보여 주기 위한 표시
    val cameraMoved: Boolean = false,
) {
    // 지도와 리스트에 보이는 모임: 모집중만, 찜 칩이 켜져 있으면 찜한 모임만, 거리순
    val visibleMeetings: List<Meeting>
        get() {
            val all = (meetings as? UiState.Success)?.data.orEmpty().onlyRecruiting()
            return all
                .filter { filter.matches(it) }
                .filter { !favoriteOnly || it.isFavorite }
                .sortedByDistance()
        }

    // 상단 종목 칩에 표시할 종목
    val sportChip: Sport? get() = filter.sport

    // 위치 조건(구·동)이 걸려 있으면 검색 영역이 고정된다.
    val hasLocationCondition: Boolean get() = filter.district != null

    val pins: List<PinGroup> get() = visibleMeetings.groupIntoPins()

    // 목록에서 사라진 핀(필터·갱신 때문에)은 선택하지 않은 것으로 본다.
    val selectedPin: PinGroup? get() = pins.firstOrNull { it.key == selectedPinKey }

    // 3-3-After: 모임이 하나뿐인 핀을 고르면 떠 있는 카드만 보인다.
    val floatingCard: Meeting? get() = selectedPin?.takeIf { !it.isMultiple }?.meetings?.single()

    // 3-5: 모임이 여러 개인 핀을 고르면 시트에 그 지번의 모임만 나온다.
    val sheetMeetings: List<Meeting>
        get() = selectedPin?.takeIf { it.isMultiple }?.meetings ?: visibleMeetings

    val sheetTitlePlace: String?
        get() = selectedPin?.takeIf { it.isMultiple }?.meetings?.first()?.place?.name

    fun isSelected(pin: PinGroup) = pin.key == selectedPinKey

    // 지도를 움직이면 '이 지역 재검색'이 나타난다. 위치 조건이 있어도 결과는 그대로이고 버튼만 보인다.
    fun withCameraMoved() = copy(cameraMoved = true)

    // 재검색: 위치 조건이 풀리고 지도 범위 기준이 된다. 다른 조건(종목·찜 등)은 유지한다. (2장)
    fun afterResearch() = copy(
        cameraMoved = false,
        filter = filter.copy(district = null),
        selectedPinKey = null,
    )

    // 찜을 켜거나 끈다. 서버 응답을 기다리지 않고 먼저 화면에 보여 준다.
    fun withFavoriteToggled(meetingId: String): ExploreState {
        val current = (meetings as? UiState.Success)?.data?.firstOrNull { it.id == meetingId } ?: return this
        return withFavoriteSet(meetingId, !current.isFavorite)
    }

    // 찜 값을 정해서 넣는다. 서버 저장이 실패하면 이 함수로 원래 값으로 되돌린다.
    fun withFavoriteSet(meetingId: String, favorite: Boolean): ExploreState {
        val current = meetings as? UiState.Success ?: return this
        return copy(
            meetings = UiState.Success(
                current.data.map { if (it.id == meetingId) it.copy(isFavorite = favorite) else it },
            ),
        )
    }
}
