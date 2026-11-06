package com.example.myapplication.ui.mypage

import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.explore.filter.paceText
import com.example.myapplication.ui.record.hikingStyleLabel
import com.example.myapplication.ui.record.skillLevelLabel
import com.example.myapplication.ui.record.sportLabel
import java.util.Calendar
import java.util.TimeZone

// 마이페이지의 표시 규칙. 화면 없이 테스트할 수 있게 순수 함수로 둔다.

// 이름 아래 한 줄: 고른 종목과 내 기록 수준. "헬스 중급 · 러닝 5'30\" · 등산 취미"
// 활동 지역은 쓰임새가 정해지지 않아 넣지 않는다. (service-rules 1장)
fun profileSubtitle(user: User): String {
    val parts = user.preferredSports.map { sport ->
        val detail = when (sport) {
            Sport.HEALTH -> user.record.armLevel?.let(::skillLevelLabel)
            Sport.RUNNING -> user.record.runningPaceSec?.let(::paceText)
            Sport.HIKING -> user.record.hikingStyle?.let(::hikingStyleLabel)
        }
        listOfNotNull(sportLabel(sport), detail).joinToString(" ")
    }
    return parts.joinToString(" · ")
}

// 동그란 이름표 글자: 닉네임 첫 글자
fun avatarInitial(nickname: String): String = nickname.trim().take(1)

fun formatTemperature(value: Float): String = "%.1f°C".format(value)

// "9/1"
fun formatReviewDate(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
    Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        .let { "${it.get(Calendar.MONTH) + 1}/${it.get(Calendar.DAY_OF_MONTH)}" }

fun verdictLabel(verdict: ReviewVerdict): String = when (verdict) {
    ReviewVerdict.GOOD -> "좋음"
    ReviewVerdict.NORMAL -> "보통"
    ReviewVerdict.BAD -> "아쉬움"
}

fun notificationStatusText(enabled: Boolean): String = if (enabled) "ON" else "OFF"
