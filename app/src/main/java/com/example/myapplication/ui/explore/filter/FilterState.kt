package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.ui.explore.onlyRecruiting

// 조건 설정 화면에서 바로 닫으면 적용되지 않는다. [draft]를 고치다가 '찾기'를 눌러야 FilterStore에 들어간다.
enum class FilterSheet { CRITERIA, DETAIL, TIME_RANGE, DATE_PICKER }

data class FilterState(
    val draft: MeetingFilter = MeetingFilter(),
    val meetings: List<Meeting> = emptyList(), // '모임 N개 찾기' 개수를 세기 위한 목록
    val nowMillis: Long = 0L,
    val openSheet: FilterSheet? = null,
    val sheetBackup: MeetingFilter? = null,   // 시트를 열 때의 조건. '적용' 없이 닫으면 이 값으로 되돌린다
    val myRecord: ExerciseRecord? = null,      // 기준의 초기값과 안내 문구에 쓰는 내 기록
) {
    // 지금 고른 조건으로 보게 될 모임 수 (탐색 지도와 같이 모집중 모임만 센다)
    val resultCount: Int get() = meetings.onlyRecruiting().count { draft.matches(it) }

    // 시트를 열 때의 조건을 기억해 둔다. 시트 안에서 고친 값은 바로 화면에 보이지만,
    // '적용' 없이 닫으면(밖을 누르거나 아래로 내림) 열기 전 값으로 돌아간다.
    fun withSheetOpened(sheet: FilterSheet): FilterState {
        // 시간 범위 시트는 값이 하나도 없으면 기본 범위에서 시작한다. 닫으면 백업으로 돌아가므로 적용 전에는 남지 않는다.
        val opened = if (sheet == FilterSheet.TIME_RANGE && draft.customRange == null)
            draft.withCustomRange(TimeOfDayRange.DEFAULT) else draft
        return copy(openSheet = sheet, sheetBackup = draft, draft = opened)
    }
    fun withSheetApplied() = copy(openSheet = null, sheetBackup = null)
    fun withSheetDismissed() = copy(draft = sheetBackup ?: draft, openSheet = null, sheetBackup = null)
}
