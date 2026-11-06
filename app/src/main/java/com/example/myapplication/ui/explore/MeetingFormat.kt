package com.example.myapplication.ui.explore

import java.util.Calendar
import java.util.TimeZone

// 카드에 보이는 문구를 만드는 순수 함수. minSdk 24라 java.time은 쓰지 않는다.

// 1km 미만은 m, 이상은 km(소수 한 자리, 정수면 소수점 없음). 거리를 모르면 null.
fun formatDistance(meters: Int?): String? = when {
    meters == null -> null
    meters < 1000 -> "${meters}m"
    else -> {
        val tenths = (meters + 50) / 100 // 반올림해 0.1km 단위
        if (tenths % 10 == 0) "${tenths / 10}km" else "${tenths / 10}.${tenths % 10}km"
    }
}

// "오늘 오전 7:00", "내일 오후 7:30", 그 밖에는 "11월 8일 오전 9:00"
fun formatStartTime(
    startAtMillis: Long,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    val start = Calendar.getInstance(timeZone).apply { timeInMillis = startAtMillis }
    val today = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
    val dayText = when (dayIndex(start) - dayIndex(today)) {
        0L -> "오늘"
        1L -> "내일"
        else -> "${start.get(Calendar.MONTH) + 1}월 ${start.get(Calendar.DAY_OF_MONTH)}일"
    }
    val hour24 = start.get(Calendar.HOUR_OF_DAY)
    val period = if (hour24 < 12) "오전" else "오후"
    val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
    val minute = start.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$dayText $period $hour12:$minute"
}

// 연도가 달라도 하루 차이를 구할 수 있게 날짜를 일 수로 바꾼다.
private fun dayIndex(c: Calendar): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }
    return utc.timeInMillis / 86_400_000L
}
