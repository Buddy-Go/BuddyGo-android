package com.example.myapplication.ui.common

import com.example.myapplication.data.model.Sport

// 종목 이름. 탐색, 조건 설정, 모임 상세, 가입 기록, 마이페이지에서 함께 쓴다.
fun sportLabel(sport: Sport): String = when (sport) {
    Sport.HEALTH -> "헬스"
    Sport.RUNNING -> "러닝"
    Sport.HIKING -> "등산"
}
