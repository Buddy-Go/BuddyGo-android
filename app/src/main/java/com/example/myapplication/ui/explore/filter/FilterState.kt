package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.ui.explore.onlyRecruiting

// 조건 설정 화면에서 바로 닫으면 적용되지 않는다. [draft]를 고치다가 '찾기'를 눌러야 FilterStore에 들어간다.
enum class FilterSheet { CRITERIA, DETAIL, TIME_RANGE }

data class FilterState(
    val draft: MeetingFilter = MeetingFilter(),
    val meetings: List<Meeting> = emptyList(), // '모임 N개 찾기' 개수를 세기 위한 목록
    val nowMillis: Long = 0L,
    val openSheet: FilterSheet? = null,
) {
    // 지금 고른 조건으로 보게 될 모임 수 (탐색 지도와 같이 모집중 모임만 센다)
    val resultCount: Int get() = meetings.onlyRecruiting().count { draft.matches(it) }
}
