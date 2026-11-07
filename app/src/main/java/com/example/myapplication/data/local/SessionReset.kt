package com.example.myapplication.data.local

import com.example.myapplication.ui.explore.LastKnownLocation
import com.example.myapplication.ui.explore.filter.FilterStore
import com.example.myapplication.ui.explore.filter.RecentDistrictStore
import com.example.myapplication.ui.meeting.create.PlaceSelectionStore
import com.example.myapplication.data.repository.SignUpDraftStore

// 로그아웃할 때 이전 사용자의 흔적을 지운다: 조건 설정, 마지막 위치, 최근 검색 지역·장소(기기에 저장된 것 포함), 가입 입력값, 활동 지역, 차단 목록.
// 다음에 로그인하는 사람이 앞사람의 검색 기록이나 조건을 보지 않게 한다.
object SessionReset {
    fun onLogout() {
        FilterStore.clear()
        LastKnownLocation.clear()
        RecentDistrictStore.clear()
        PlaceSelectionStore.reset()
        SignUpDraftStore.clear()
        ActivityAreaStore.reset()
        BlockedUsersStore.clear()
    }
}
