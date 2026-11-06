package com.example.myapplication.ui.explore

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.ui.common.UiState

data class ExploreState(
    val meetings: UiState<List<Meeting>> = UiState.Loading,
    val sheetExpanded: Boolean = true,   // 3-After(올림) / 3-2-After(내림)
    val selectedPinKey: String? = null,
    val favoriteOnly: Boolean = false,   // 상단 '찜' 칩
) {
    // 지도와 리스트에 보이는 모임: 모집중만, 찜 칩이 켜져 있으면 찜한 모임만, 거리순
    val visibleMeetings: List<Meeting>
        get() {
            val all = (meetings as? UiState.Success)?.data.orEmpty().onlyRecruiting()
            return (if (favoriteOnly) all.filter { it.isFavorite } else all).sortedByDistance()
        }

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
}
