package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// TODO: 색 값은 로그인(1) 스크린샷에서 추정한 값. 16번 프레임 캡션 기준 값으로 확정되면 교체한다.
object AppColor {
    val Accent = Color(0xFFA3E635)
    val AccentStrong = Color(0xFF4D7C0F)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF7F7F8)
    val TextPrimary = Color(0xFF18181B)
    val TextSecondary = Color(0xFF71717A)
    val TextDisabled = Color(0xFFA1A1AA)
    val Border = Color(0xFFE4E4E7)
    val IconMuted = Color(0xFF71717A)
}

object AppRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
}

object AppSpace {
    val md = 8.dp
    val xl = 12.dp
    val xxl = 16.dp
    val x4 = 20.dp
    val x5 = 24.dp
}
