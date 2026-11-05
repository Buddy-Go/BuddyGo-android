package com.example.myapplication.ui.meeting.notice

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.Notice
import com.example.myapplication.data.repository.FakeNoticeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

// 공지·댓글 규칙 (service-rules 9장)과 8a~8i 표시 글자
class NoticeStateTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    @Test
    fun 시간_표시_8d() {
        assertEquals("9/7 21:30", noticeTimeText(1788784200000L, seoul))
        assertEquals("9/5 14:10", noticeTimeText(1788585000000L, seoul))
    }

    @Test
    fun 수정된_공지는_수정됨_표시_8f() {
        val notice = Notice(createdAtMillis = 1788784200000L, isEdited = true)
        assertEquals("9/7 21:30 · 수정됨", notice.metaText(seoul))
        assertEquals("9/7 21:30", notice.copy(isEdited = false).metaText(seoul))
    }

    @Test
    fun 등록은_제목과_본문이_있어야_켜짐_8a_8b() {
        assertFalse(NoticeEditState().canSubmit)
        assertFalse(NoticeEditState(title = "제목").canSubmit)
        assertTrue(NoticeEditState(title = "제목", body = "본문").canSubmit)
        assertEquals("등록", NoticeEditState().submitLabel)
        assertEquals("저장", NoticeEditState(isEditMode = true).submitLabel)
        assertEquals("공지 수정", NoticeEditState(isEditMode = true).screenTitle)
        assertEquals("40 / 200", NoticeEditState(body = "a".repeat(40)).counterText)
    }

    @Test
    fun 공지_관리는_호스트만_읽기전용이면_불가() {
        assertTrue(NoticeListState(isHost = true).canManage)
        assertFalse(NoticeListState(isHost = false).canManage)
        assertFalse(NoticeListState(isHost = true, isReadOnly = true).canManage)
        assertFalse(NoticeDetailState(isReadOnly = true).canWriteComment)
    }

    @Test
    fun 내_댓글에만_메뉴_8f_8g() {
        val s = NoticeDetailState(myUserId = "u3")
        assertTrue(s.isMine(Comment(authorId = "u3")))
        assertFalse(s.isMine(Comment(authorId = "u1")))
        assertFalse(s.copy(isReadOnly = true).isMine(Comment(authorId = "u3")))
    }

    @Test
    fun 댓글_머리글과_전송_버튼_8h_8i() {
        assertEquals("댓글", NoticeDetailState().commentHeader)
        assertEquals("댓글 1", NoticeDetailState(comments = listOf(Comment())).commentHeader)
        assertFalse(NoticeDetailState(commentInput = "  ").canSend)
        assertTrue(NoticeDetailState(commentInput = "우산 챙겨갈게요").canSend)
    }

    @Test
    fun 가짜_저장소_작성_수정_댓글_삭제() = runBlocking {
        val repo = FakeNoticeRepository()
        val created = repo.createNotice("m-test", "제목", "본문").getOrThrow()
        assertEquals(listOf(created.id), repo.getNotices("m-test").getOrThrow().map { it.id })

        val updated = repo.updateNotice(created.id, "새 제목", "새 본문").getOrThrow()
        assertTrue(updated.isEdited)

        repo.addComment(created.id, "댓글")
        assertEquals(1, repo.getNotice(created.id).getOrThrow().commentCount)

        repo.deleteNotice(created.id)
        assertTrue(repo.getNotices("m-test").getOrThrow().isEmpty())
        assertTrue(repo.getComments(created.id).getOrThrow().isEmpty())
        assertTrue(repo.getNotice(created.id).isFailure)
    }

    @Test
    fun 예시_공지는_내가_호스트인_모임에_있음() = runBlocking {
        val notices = FakeNoticeRepository().getNotices(MockMeetings.hosted.id).getOrThrow()
        assertTrue(notices.isNotEmpty())
        assertEquals(MeetingRole.HOST, MockMeetings.hosted.roleOf(MockUsers.me.id))
    }
}
