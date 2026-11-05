package com.example.myapplication.ui.meeting.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 호스트 영역(5a-H 계열)과 취소·삭제 규칙 (service-rules 4장)
class HostSectionStateTest {

    private val open = HostSectionState(
        hostNickname = "민준",
        confirmedNicknames = listOf("민준", "수아"),
        capacity = 3,
        isClosed = false,
        pendingApplicantCount = 2,
    )

    @Test
    fun 확정_명단_표시_5aH() {
        assertEquals("확정 2 / 3", open.confirmedLabel)
        assertEquals("민준 외 1명", open.rosterSummary)
        assertEquals("민준", open.copy(confirmedNicknames = listOf("민준")).rosterSummary)
        assertEquals(2f / 3f, open.progress, 0.001f)
        assertEquals("인원이 다 차면 자동으로 마감돼요 · 참가자에게는 마감 후 명단이 공개돼요", open.rosterHint)
        assertEquals("마감됐어요 · 참가자에게도 명단이 공개됐어요", open.copy(isClosed = true).rosterHint)
    }

    @Test
    fun 참가자_관리_버튼_강조는_신청이_있고_모집중일_때만() {
        assertTrue(open.highlightManageButton) // 5a-H
        assertFalse(open.copy(pendingApplicantCount = 0).highlightManageButton) // 5a-H2
        assertFalse(open.copy(isClosed = true).highlightManageButton) // 5a-H5
    }

    @Test
    fun 공지_없으면_작성_있으면_전체() {
        assertEquals("작성", open.noticeActionLabel)
        assertEquals("전체", open.copy(latestNotice = HostNoticePreview("n1", "9/7 21:30", "t", "b")).noticeActionLabel)
    }

    @Test
    fun 호스트만_있으면_삭제_확정자가_있으면_취소() {
        assertEquals(listOf(HostAction.EDIT, HostAction.DELETE), open.copy(confirmedNicknames = listOf("민준")).menuActions)
        assertEquals(listOf(HostAction.EDIT, HostAction.CANCEL), open.menuActions)
        assertEquals("확정된 메이트 1명에게 취소 알림이 가요", open.cancelNoticeText)
    }

    @Test
    fun 진행중_이후에는_수정_취소_삭제_없음() {
        assertTrue(open.copy(isStartedOrLater = true).menuActions.isEmpty())
    }

    @Test
    fun 취소_사유는_필수이고_100자까지() {
        assertFalse(canSubmitCancelReason(""))
        assertFalse(canSubmitCancelReason("   "))
        assertTrue(canSubmitCancelReason("일정이 생겼어요"))
        assertTrue(canSubmitCancelReason("가".repeat(100)))
        assertFalse(canSubmitCancelReason("가".repeat(101)))
    }
}
