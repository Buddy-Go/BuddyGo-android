package com.example.myapplication.data.api

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// 서버 시각(ISO-8601, 예: 2026-11-10T09:50:00Z)과 앱의 millis 사이를 바꾼다. minSdk 24라 java.time을 쓰지 않는다.
object IsoTime {
    private val PATTERN = Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$""")

    fun parse(text: String): Long {
        val m = PATTERN.matchEntire(text.trim()) ?: throw IllegalArgumentException("시각 형식이 올바르지 않아요: $text")
        val base = formatter("yyyy-MM-dd'T'HH:mm:ss").parse(m.groupValues[1])!!.time
        val fraction = m.groupValues[2].padEnd(3, '0').take(3).ifEmpty { "0" }.toLong()
        val zone = m.groupValues[3]
        val offsetMillis = if (zone.isEmpty() || zone == "Z") 0L else {
            val sign = if (zone[0] == '-') -1 else 1
            val digits = zone.substring(1).replace(":", "")
            sign * (digits.substring(0, 2).toLong() * 60 + digits.substring(2, 4).toLong()) * 60_000L
        }
        return base + fraction - offsetMillis
    }

    fun format(millis: Long): String = formatter("yyyy-MM-dd'T'HH:mm:ss'Z'").format(java.util.Date(millis))

    private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
}
