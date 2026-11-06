package com.example.myapplication.ui.mypage

import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.model.Sport
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class MyPageRulesTest {
    @Test
    fun 종목과_기록_수준을_한_줄로_보여_준다() {
        assertEquals("헬스 중급 · 러닝 5'30\"", profileSubtitle(MockUsers.me))
        assertEquals("러닝 5'45\" · 등산 전문", profileSubtitle(MockUsers.hostB))
    }

    @Test
    fun 기록이_없으면_종목_이름만_나온다() {
        assertEquals("헬스", profileSubtitle(MockUsers.me.copy(preferredSports = listOf(Sport.HEALTH), record = com.example.myapplication.data.model.ExerciseRecord())))
        assertEquals("", profileSubtitle(MockUsers.noRecord))
    }

    @Test
    fun 이름표_글자와_온도_표기() {
        assertEquals("민", avatarInitial(" 민준"))
        assertEquals("36.5°C", formatTemperature(36.5f))
        assertEquals("42.5°C", formatTemperature(42.5f))
    }

    @Test
    fun 후기_날짜는_월_일이다() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals("9/1", formatReviewDate(1_788_220_800_000L, utc))
    }

    @Test
    fun 판정과_알림_상태_문구() {
        assertEquals("좋음", verdictLabel(ReviewVerdict.GOOD))
        assertEquals("아쉬움", verdictLabel(ReviewVerdict.BAD))
        assertEquals("ON", notificationStatusText(true))
        assertEquals("OFF", notificationStatusText(false))
    }
}
