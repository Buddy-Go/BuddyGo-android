package com.example.myapplication.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 서버가 받는 종목(지금은 헬스뿐). 서버가 러닝·등산을 받게 되면 이 테스트도 같이 고친다.
class SportSupportTest {
    @Test
    fun 서버는_지금_헬스만_받는다() {
        assertTrue(SportSupport.isSupported(Sport.HEALTH))
        assertFalse(SportSupport.isSupported(Sport.RUNNING))
        assertFalse(SportSupport.isSupported(Sport.HIKING))
    }

    @Test
    fun 선택지는_정의된_순서의_지원_종목이다() {
        assertEquals(listOf(Sport.HEALTH), SportSupport.supported)
    }

    @Test
    fun 모델은_모든_종목을_그대로_다룬다() {
        // 화면만 막고 모델·ViewModel은 그대로여서, 서버가 지원하면 SportSupport만 바꾸면 된다
        assertEquals(3, Sport.entries.size)
    }
}
