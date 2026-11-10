package com.example.myapplication.data.repository

import com.example.myapplication.data.api.CommentCreateDto
import com.example.myapplication.data.api.CommentDto
import com.example.myapplication.data.api.IsoTime
import com.example.myapplication.data.api.NoticeApi
import com.example.myapplication.data.api.NoticeCreateDto
import com.example.myapplication.data.api.NoticeDto
import com.example.myapplication.data.api.NoticeUpdateDto
import com.example.myapplication.data.api.apiCall
import com.example.myapplication.data.api.requireData
import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.Notice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

// 공지·댓글이 어느 모임에 속하는지 기억해 두는 곳. 서버 주소에는 모임 id가 들어가는데,
// 앱 화면은 공지 id(댓글 id)만 넘기므로 목록·알림·작성 때 본 값을 여기에 적어 둔다.
object NoticeIndex {
    private val meetingOfNotice = ConcurrentHashMap<String, String>()
    private val noticeOfComment = ConcurrentHashMap<String, String>()

    fun putNotice(noticeId: String, meetingId: String) { meetingOfNotice[noticeId] = meetingId }
    fun meetingOf(noticeId: String): String? = meetingOfNotice[noticeId]
    fun putComment(commentId: String, noticeId: String) { noticeOfComment[commentId] = noticeId }
    fun noticeOf(commentId: String): String? = noticeOfComment[commentId]
    fun clear() { meetingOfNotice.clear(); noticeOfComment.clear() }
}

// 서버(BuddyGo-server)와 연결한 공지·댓글 저장소.
//  - 서버가 공지 응답에 댓글 수(commentCount)를, 댓글 응답에 호스트 여부(authorHost)를 준다.
//    값이 비어 있는 서버(이전 버전)면 댓글을 받아 세고, 공지 작성자(=호스트)와 비교한다.
//  - 서버 목록은 쪽(page)으로 나뉘어 있어 앱은 전부 이어 받는다.
class RealNoticeRepository(private val api: NoticeApi) : NoticeRepository {

    override suspend fun getNotices(meetingId: String): Result<List<Notice>> = withLong(meetingId) { g ->
        apiCall {
            val all = mutableListOf<NoticeDto>()
            var page = 0
            while (page < MAX_PAGES) {
                val res = api.list(g, page, PAGE_SIZE).requireData()
                all += res.content
                if (!res.hasNext) break
                page++
            }
            all.forEach { NoticeIndex.putNotice(it.noticeId.toString(), meetingId) }
            coroutineScope {
                val gate = Semaphore(PARALLELISM)
                all.map { n -> async { gate.withPermit { n.toNotice(commentCount = n.commentCount ?: countComments(g, n.noticeId)) } } }.awaitAll()
            }
        }
    }

    override suspend fun getNotice(noticeId: String): Result<Notice> = withNotice(noticeId) { g, n ->
        apiCall { api.detail(g, n).requireData().let { it.toNotice(commentCount = it.commentCount ?: countComments(g, n)) } }
    }

    override suspend fun createNotice(meetingId: String, title: String, body: String): Result<Notice> = withLong(meetingId) { g ->
        apiCall { api.create(g, NoticeCreateDto(title.trim(), body.trim())).requireData() }.map {
            NoticeIndex.putNotice(it.noticeId.toString(), meetingId)
            it.toNotice(0)
        }
    }

    override suspend fun updateNotice(noticeId: String, title: String, body: String): Result<Notice> = withNotice(noticeId) { g, n ->
        apiCall { api.update(g, n, NoticeUpdateDto(title.trim(), body.trim())).requireData() }.map { it.toNotice(it.commentCount ?: countComments(g, n)) }
    }

    override suspend fun deleteNotice(noticeId: String): Result<Unit> = withNotice(noticeId) { g, n ->
        apiCall { api.delete(g, n) }.map { }
    }

    // 오래된 순. 호스트가 쓴 댓글에는 호스트 표시를 붙인다. (공지는 호스트만 쓰므로 공지 작성자가 호스트다)
    override suspend fun getComments(noticeId: String): Result<List<Comment>> = withNotice(noticeId) { g, n ->
        apiCall {
            val all = mutableListOf<CommentDto>()
            var page = 0
            while (page < MAX_PAGES) {
                val res = api.comments(g, n, page, COMMENT_PAGE_SIZE).requireData()
                all += res.content
                if (!res.hasNext) break
                page++
            }
            val hostId = if (all.any { it.authorHost == null }) api.detail(g, n).requireData().authorId else null
            all.map { it.toComment(hostId) }
        }
    }

    override suspend fun addComment(noticeId: String, body: String): Result<Comment> = withNotice(noticeId) { g, n ->
        apiCall {
            val created = api.addComment(g, n, CommentCreateDto(body.trim())).requireData()
            val hostId = if (created.authorHost == null) api.detail(g, n).requireData().authorId else null
            created.toComment(hostId)
        }
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        val noticeId = NoticeIndex.noticeOf(commentId) ?: return Result.failure(NoSuchElementException(commentId))
        val c = commentId.toLongOrNull() ?: return Result.failure(NoSuchElementException(commentId))
        return withNotice(noticeId) { g, n -> apiCall { api.deleteComment(g, n, c) }.map { } }
    }

    // ---- 내부 ----

    private suspend fun countComments(g: Long, n: Long): Int = try {
        api.comments(g, n, 0, COMMENT_PAGE_SIZE).requireData().content.size
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        0
    }

    private fun NoticeDto.toNotice(commentCount: Int) = Notice(
        id = noticeId.toString(),
        meetingId = gatheringId.toString(),
        title = title,
        body = content,
        createdAtMillis = IsoTime.parse(createdAt),
        isEdited = edited,
        commentCount = commentCount,
    )

    private fun CommentDto.toComment(hostId: Long?): Comment {
        NoticeIndex.putComment(commentId.toString(), noticeId.toString())
        return Comment(
            id = commentId.toString(),
            noticeId = noticeId.toString(),
            authorId = authorId.toString(),
            authorNickname = authorNickname,
            body = content,
            createdAtMillis = IsoTime.parse(createdAt),
            isAuthorHost = authorHost ?: (authorId == hostId),
        )
    }

    private inline fun <T> withLong(id: String, block: (Long) -> Result<T>): Result<T> {
        val v = id.toLongOrNull() ?: return Result.failure(NoSuchElementException(id))
        return block(v)
    }

    private inline fun <T> withNotice(noticeId: String, block: (Long, Long) -> Result<T>): Result<T> {
        val n = noticeId.toLongOrNull() ?: return Result.failure(NoSuchElementException(noticeId))
        val g = NoticeIndex.meetingOf(noticeId)?.toLongOrNull()
            ?: return Result.failure(NoSuchElementException("모임을 알 수 없는 공지예요: $noticeId"))
        return block(g, n)
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val COMMENT_PAGE_SIZE = 100
        const val MAX_PAGES = 4
        const val PARALLELISM = 6
    }
}
