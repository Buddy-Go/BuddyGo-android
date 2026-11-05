package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.Notice

// 공지·댓글 창구 (8a~8i). 권한(호스트만 작성·수정·삭제 등)은 서버가 다시 확인한다 (project-rules 6장).
// 데이터 계약(docs/data-model.md)대로 모든 호출은 suspend이고 Result<T>를 돌려준다.
interface NoticeRepository {
    suspend fun getNotices(meetingId: String): Result<List<Notice>> // 최신순
    suspend fun getNotice(noticeId: String): Result<Notice>
    suspend fun createNotice(meetingId: String, title: String, body: String): Result<Notice>
    suspend fun updateNotice(noticeId: String, title: String, body: String): Result<Notice>
    suspend fun deleteNotice(noticeId: String): Result<Unit>

    suspend fun getComments(noticeId: String): Result<List<Comment>> // 오래된 순
    suspend fun addComment(noticeId: String, body: String): Result<Comment>
    suspend fun deleteComment(commentId: String): Result<Unit>
}
