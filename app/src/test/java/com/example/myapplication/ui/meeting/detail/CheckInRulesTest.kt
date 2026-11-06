package com.example.myapplication.ui.meeting.detail

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInRulesTest {
    @Test
    fun 체크인은_오십_미터_안에서만_할_수_있다() {
        assertTrue(canCheckIn(0))
        assertTrue(canCheckIn(40))
        assertTrue("경계 포함", canCheckIn(50))
        assertFalse(canCheckIn(51))
        assertFalse(canCheckIn(320))
    }

    @Test
    fun 범위_안이면_체크인할까요_창이다() {
        val t = checkInTextsOf(CheckInPrompt.Ready(40))
        assertEquals("체크인할까요?", t.title)
        assertEquals("모임 장소에서 40m", t.distanceLine)
        assertEquals("지금 체크인할 수 있어요", t.message)
        assertEquals("체크인", t.confirmText)
        assertEquals("아니요", t.dismissText)
        assertTrue(t.confirmEnabled)
        assertTrue(t.canRefresh)
    }

    @Test
    fun 거리가_1km_넘으면_km로_줄여_보인다() {
        assertEquals("모임 장소에서 10.7km", checkInTextsOf(CheckInPrompt.Ready(10655)).distanceLine)
        assertEquals("모임 장소에서 999m", checkInTextsOf(CheckInPrompt.Ready(999)).distanceLine)
    }

    @Test
    fun 범위_밖이면_아직_조금_멀어요_창이고_체크인_버튼이_막힌다() {
        val t = checkInTextsOf(CheckInPrompt.Ready(320))
        assertEquals("아직 조금 멀어요", t.title)
        assertEquals("모임 장소에서 320m", t.distanceLine)
        assertEquals("반경 50m 안에서 체크인할 수 있어요", t.message)
        assertEquals("닫기", t.dismissText)
        assertFalse(t.confirmEnabled)
        assertTrue("거리를 다시 잴 수 있다", t.canRefresh)
    }

    @Test
    fun 오십_미터_바로_안과_밖의_문구가_갈린다() {
        assertEquals("체크인할까요?", checkInTextsOf(CheckInPrompt.Ready(50)).title)
        assertEquals("아직 조금 멀어요", checkInTextsOf(CheckInPrompt.Ready(51)).title)
    }

    @Test
    fun 위치를_가져오는_중에는_체크인할_수_없다() {
        val t = checkInTextsOf(CheckInPrompt.Locating)
        assertFalse(t.confirmEnabled)
        assertNull(t.distanceLine)
    }

    @Test
    fun 위치를_못_얻으면_권한_안내와_다시_시도를_준다() {
        val t = checkInTextsOf(CheckInPrompt.NoLocation)
        assertEquals("위치를 확인할 수 없어요", t.title)
        assertEquals("다시 시도", t.confirmText)
        assertTrue(t.confirmEnabled)
        assertTrue(t.message!!.contains("위치 권한"))
    }

    @Test
    fun 보내는_중에는_거리를_보여_주고_버튼이_막힌다() {
        val t = checkInTextsOf(CheckInPrompt.Submitting(30))
        assertEquals("모임 장소에서 30m", t.distanceLine)
        assertFalse(t.confirmEnabled)
        assertFalse(t.canRefresh)
    }

    @Test
    fun 보내지_못하면_다시_시도를_준다() {
        val t = checkInTextsOf(CheckInPrompt.Failed)
        assertEquals("체크인하지 못했어요", t.title)
        assertEquals("다시 시도", t.confirmText)
        assertTrue(t.confirmEnabled)
    }

    @Test
    fun 체크인_시각은_오전_오후와_분을_두_자리로_쓴다() {
        val seoul = TimeZone.getTimeZone("Asia/Seoul")
        fun at(h: Int, m: Int) = Calendar.getInstance(seoul).apply { clear(); set(2026, 8, 8, h, m) }.timeInMillis
        assertEquals("오전 7:02", formatCheckInTime(at(7, 2), seoul))
        assertEquals("오후 12:00", formatCheckInTime(at(12, 0), seoul))
        assertEquals("오전 12:05", formatCheckInTime(at(0, 5), seoul))
        assertEquals("오후 11:59", formatCheckInTime(at(23, 59), seoul))
    }
}
