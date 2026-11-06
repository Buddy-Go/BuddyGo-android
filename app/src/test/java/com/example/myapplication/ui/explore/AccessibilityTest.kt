package com.example.myapplication.ui.explore

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityTest {
    @Test
    fun 찜_버튼은_누르면_일어날_일을_알려_준다() {
        assertEquals("찜하기", favoriteLabel(false))
        assertEquals("찜 해제", favoriteLabel(true))
    }

    @Test
    fun 알림_벨은_읽지_않은_알림이_있는지_함께_알려_준다() {
        assertEquals("알림", notificationLabel(false))
        assertEquals("알림, 읽지 않은 알림이 있어요", notificationLabel(true))
    }
}
