package com.example.myapplication.ui.meeting.notice

import com.example.myapplication.data.model.Comment
import com.example.myapplication.data.model.NOTICE_BODY_MAX_LENGTH
import com.example.myapplication.data.model.Notice
import java.util.Calendar
import java.util.TimeZone

// 공지 화면 세 개(목록 8d·8e, 작성·수정 8a·8b·8c, 상세 8f~8i)의 상태.
// 권한 규칙 (service-rules 9장)
// - 공지 작성·수정·삭제: 호스트만
// - 읽기·댓글: 호스트와 확정 참가자
// - 종료·취소된 모임: 읽기 전용(쓰기 버튼·댓글 입력 없음)

// 8d 호스트 / 8e 참가자
data class NoticeListState(
    val notices: List<Notice> = emptyList(),
    val isHost: Boolean = false,
    val isReadOnly: Boolean = false,
    val isLoading: Boolean = true,
) {
    // 8d: 각 공지의 ⋯(수정·삭제)와 아래 '공지 작성' 버튼은 호스트에게만
    val canManage: Boolean get() = isHost && !isReadOnly
}

// 8a 빈 상태 / 8b 입력 중 / 8c 수정
data class NoticeEditState(
    val isEditMode: Boolean = false, // 8c
    val title: String = "",
    val body: String = "",
    val isLoading: Boolean = false, // 수정할 공지를 불러오는 중
    val isSubmitting: Boolean = false,
    val isDone: Boolean = false, // 등록·저장이 끝나면 화면을 닫는다
) {
    val screenTitle: String get() = if (isEditMode) "공지 수정" else "공지 작성"
    val submitLabel: String get() = if (isEditMode) "저장" else "등록"

    // 8a는 '등록'이 회색, 8b는 초록. 제목과 본문이 둘 다 있어야 켜진다 (추정: 디자인 8a·8b 기준)
    val canSubmit: Boolean get() = title.isNotBlank() && body.isNotBlank() && !isSubmitting && !isLoading

    val counterText: String get() = "${body.length} / $NOTICE_BODY_MAX_LENGTH"
}

// 8f 호스트 / 8g 참가자 / 8h 댓글 입력 중 / 8i 댓글 없음
data class NoticeDetailState(
    val notice: Notice? = null,
    val comments: List<Comment> = emptyList(),
    val myUserId: String = "",
    val isHost: Boolean = false,
    val isReadOnly: Boolean = false,
    val commentInput: String = "",
    val isSending: Boolean = false,
    val isDeleted: Boolean = false, // 공지를 지우면 화면을 닫는다
) {
    // 8f: 오른쪽 위 ⋯(수정·삭제)는 호스트에게만
    val canManageNotice: Boolean get() = isHost && !isReadOnly

    val canWriteComment: Boolean get() = !isReadOnly

    // 8h: 입력이 있으면 전송 버튼이 초록으로 켜진다
    val canSend: Boolean get() = commentInput.isNotBlank() && !isSending

    // 8f "댓글 3" / 8i "댓글"
    val commentHeader: String get() = if (comments.isEmpty()) "댓글" else "댓글 ${comments.size}"

    // 8f·8g: 내가 쓴 댓글에만 ⋯ (삭제)
    fun isMine(comment: Comment): Boolean = comment.authorId == myUserId && !isReadOnly
}

// "9/7 21:30" (8d, 8f). 8f는 수정된 공지에 " · 수정됨"을 붙인다.
fun noticeTimeText(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val hour = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$month/$day $hour:$minute"
}

fun Notice.metaText(timeZone: TimeZone = TimeZone.getDefault()): String =
    noticeTimeText(createdAtMillis, timeZone) + if (isEdited) " · 수정됨" else ""
