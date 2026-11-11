package com.example.myapplication.data.model

// 서버(BuddyGo-server)가 지금 받아 주는 종목. 서버에 러닝·등산이 생기면 여기만 늘리면 화면이 열린다.
// 서버에 없는 종목으로 모임을 만들거나 선호 종목·기록을 저장하면 실패하거나 조용히 사라지기 때문에,
// 화면은 이 값으로 해당 종목을 '준비 중'으로 막는다. (모델과 ViewModel은 모든 종목을 그대로 다룬다)
object SportSupport {
    val serverSports: Set<Sport> = setOf(Sport.HEALTH)

    fun isSupported(sport: Sport): Boolean = sport in serverSports

    // 선택지로 보여 줄 수 있는 종목 (정의된 순서). 화면은 러닝·등산을 아예 보여 주지 않는다.
    val supported: List<Sport> get() = Sport.entries.filter(::isSupported)

    // 종목이 둘 이상이어야 고를 거리가 있다. 하나뿐이면 종목 선택·필터 칩은 숨긴다.
    val hasChoice: Boolean get() = supported.size > 1
}
