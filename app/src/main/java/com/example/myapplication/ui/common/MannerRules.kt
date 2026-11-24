package com.example.myapplication.ui.common

// 매너온도 게이지의 값 규칙. 화면과 분리해 두어 단위 테스트로 확인한다.

const val MANNER_BASE = 36.5f // 처음 시작하는 온도(시작점 표시)
const val MANNER_MAX = 100f

// 게이지에서 온도가 차지하는 위치(0~1)
fun mannerFraction(temperature: Float): Float = (temperature / MANNER_MAX).coerceIn(0f, 1f)

// 얼굴 표정과 한 줄 설명을 정하는 단계
enum class MannerTier(val label: String, val smile: Float, val blush: Boolean) {
    FREEZING("많이 차가워요", -0.5f, false),   // 20 미만
    COOL("조금 쌀쌀해요", -0.05f, false),      // 20 ~ 36.4
    WARM("따뜻해요", 0.4f, false),             // 36.5 ~ 44.9
    TOASTY("아주 따뜻해요", 0.65f, false),      // 45 ~ 59.9
    HOT("뜨거워요", 0.9f, true),               // 60 이상
}

fun mannerTier(temperature: Float): MannerTier = when {
    temperature < 20f -> MannerTier.FREEZING
    temperature < MANNER_BASE -> MannerTier.COOL
    temperature < 45f -> MannerTier.WARM
    temperature < 60f -> MannerTier.TOASTY
    else -> MannerTier.HOT
}

// "36.5" (소수 첫째 자리, 반올림)
fun mannerNumber(temperature: Float): String {
    val tenths = Math.round(temperature * 10)
    return "${tenths / 10}.${Math.abs(tenths % 10)}"
}

// 시작점 대비 설명: "시작점보다 2.0° 높아요"
fun mannerDeltaText(temperature: Float): String {
    val tenths = Math.round((temperature - MANNER_BASE) * 10)
    val abs = Math.abs(tenths)
    val text = "${abs / 10}.${abs % 10}°"
    return when {
        tenths > 0 -> "시작점보다 $text 높아요"
        tenths < 0 -> "시작점보다 $text 낮아요"
        else -> "시작점과 같아요"
    }
}
