package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.MeetingPlace
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    @Test
    fun 제목_일시_장소_종목과_정원을_네_줄로_적는다() {
        val m = MockMeetings.recruiting
        val lines = shareText(m, seoul).lines()
        assertEquals(4, lines.size)
        assertEquals(m.title, lines[0])
        assertEquals(formatDetailDateTime(m.startAtMillis, m.durationMinutes, seoul), lines[1])
        assertTrue(lines[2].startsWith(m.place.name))
        assertEquals("헬스 · 정원 ${m.capacity}명", lines[3])
    }

    @Test
    fun 상세주소가_있으면_장소_줄에_같이_적는다() {
        val m = MockMeetings.recruiting.copy(place = MockMeetings.recruiting.place.copy(detailAddress = "3층 프리웨이트 존"))
        assertTrue(shareText(m, seoul).lines()[2].contains("3층 프리웨이트 존"))
    }

    @Test
    fun 장소가_비어_있으면_장소_줄을_넣지_않는다() {
        val m = MockMeetings.recruiting.copy(place = MeetingPlace())
        assertEquals(3, shareText(m, seoul).lines().size)
    }

    @Test
    fun 호스트와_참가자_정보는_넣지_않는다() {
        val m = MockMeetings.closed
        val text = shareText(m, seoul)
        assertFalse(text.contains(m.hostNickname))
        assertTrue(m.members.none { text.contains(it.nickname) && it.nickname != m.title })
    }

    @Test
    fun 제목_앞뒤_공백은_정리한다() {
        assertEquals("제목", shareText(MockMeetings.recruiting.copy(title = "  제목 "), seoul).lines()[0])
    }
}
