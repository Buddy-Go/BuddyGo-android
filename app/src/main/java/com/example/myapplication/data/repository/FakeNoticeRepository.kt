package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.Notice
import com.example.myapplication.data.model.User

// 목데이터로 응답한다. 앱이 켜져 있는 동안만 메모리에 남는다.
// 여러 화면(목록·작성·상세)이 같은 데이터를 보도록 저장 공간은 companion object에 둔다.
// 예시 공지는 내가 호스트인 모임(MockMeetings.hosted)과 내가 확정된 모임(MockMeetings.closed)에 있다.
class FakeNoticeRepository(
    private val me: User = MockUsers.me,
) : NoticeRepository {

    override suspend fun getNotices(meetingId: String): Result<List<Notice>> = Result.success(
        notices.filter { it.meetingId == meetingId }.map(::withCount).sortedByDescending { it.createdAtMillis },
    )

    override suspend fun getNotice(noticeId: String): Result<Notice> =
        notices.find { it.id == noticeId }?.let { Result.success(withCount(it)) }
            ?: Result.failure(NoSuchElementException(noticeId))

    override suspend fun createNotice(meetingId: String, title: String, body: String): Result<Notice> {
        val notice = Notice("n${nextId++}", meetingId, title, body, System.currentTimeMillis())
        notices += notice
        return Result.success(notice)
    }

    override suspend fun updateNotice(noticeId: String, title: String, body: String): Result<Notice> {
        val index = notices.indexOfFirst { it.id == noticeId }
        if (index < 0) return Result.failure(NoSuchElementException(noticeId))
        notices[index] = notices[index].copy(title = title, body = body, isEdited = true)
        return Result.success(notices[index])
    }

    override suspend fun deleteNotice(noticeId: String): Result<Unit> {
        notices.removeAll { it.id == noticeId }
        comments.removeAll { it.noticeId == noticeId }
        return Result.success(Unit)
    }

    override suspend fun getComments(noticeId: String): Result<List<Comment>> =
        Result.success(comments.filter { it.noticeId == noticeId }.sortedBy { it.createdAtMillis })

    override suspend fun addComment(noticeId: String, body: String): Result<Comment> {
        val notice = notices.find { it.id == noticeId } ?: return Result.failure(NoSuchElementException(noticeId))
        val hostId = MockMeetings.all.find { it.id == notice.meetingId }?.hostId
        val comment = Comment(
            id = "c${nextId++}",
            noticeId = noticeId,
            authorId = me.id,
            authorNickname = me.nickname,
            body = body,
            createdAtMillis = System.currentTimeMillis(),
            isAuthorHost = me.id == hostId,
        )
        comments += comment
        return Result.success(comment)
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        comments.removeAll { it.id == commentId }
        return Result.success(Unit)
    }

    private fun withCount(n: Notice) = n.copy(commentCount = comments.count { it.noticeId == n.id })

    companion object {
        private var nextId = 100
        private const val MINUTE = 60_000L
        private val base = System.currentTimeMillis() - 2 * 24 * 60 * MINUTE
        private val hosted = MockMeetings.hosted.id // 내가 호스트 (u1 민준)
        private val joined = MockMeetings.closed.id // 내가 확정 (호스트 u3 지훈)

        private val notices = mutableListOf(
            Notice("n1", hosted, "집합 장소 변경", "비 예보가 있어서 정문 대신 후문 3층 프리웨이트 존에서 만나요.", base, isEdited = true),
            Notice("n2", hosted, "준비물 안내", "스트랩이랑 개인 수건 꼭 챙겨주세요. 물은 1L 이상 챙기시면 좋습니다.", base - 2 * 24 * 60 * MINUTE),
            Notice("n3", joined, "스쿼트 랙 예약했어요", "7시부터 2번 랙 잡아뒀어요. 늦으면 미리 말씀해 주세요.", base + 30 * MINUTE),
        )
        private val comments = mutableListOf(
            Comment("c1", "n1", "u2", "서연", "후문 3층 맞죠? 몇 시까지 가면 될까요?", base + 12 * MINUTE),
            Comment("c2", "n1", "u1", "민준", "네 6시 50분까지 와주시면 돼요", base + 20 * MINUTE, isAuthorHost = true),
            Comment("c3", "n1", "u3", "지훈", "확인했어요!", base + 35 * MINUTE),
            Comment("c4", "n3", "u3", "지훈", "랙 번호 바뀌면 다시 알려드릴게요", base + 40 * MINUTE, isAuthorHost = true),
        )
    }
}
