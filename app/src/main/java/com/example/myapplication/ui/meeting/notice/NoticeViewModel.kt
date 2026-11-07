package com.example.myapplication.ui.meeting.notice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.NOTICE_BODY_MAX_LENGTH
import com.example.myapplication.data.repository.MeetingRepository
import com.example.myapplication.data.repository.NoticeRepository
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.launch

// 공지 화면 세 개의 ViewModel. 화면 사이에는 id만 넘긴다 (project-rules 7장).
// 호스트 여부는 Meeting.roleOf(내 id)로만 판단한다 (docs/data-model.md).

// 종료·취소된 모임의 공지는 읽기 전용 (service-rules 9장)
internal fun Meeting.isNoticeReadOnly(): Boolean = status == MeetingStatus.ENDED || status == MeetingStatus.CANCELED

// 8d·8e 공지 목록
class NoticeListViewModel(
    private val meetingId: String,
    private val noticeRepository: NoticeRepository,
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    var state by mutableStateOf(NoticeListState())
        private set

    init {
        viewModelScope.launch {
            val meeting = meetingRepository.getMeeting(meetingId).getOrNull()
            val me = userRepository.getCurrentUser().getOrNull()
            if (meeting != null && me != null) {
                state = state.copy(isHost = meeting.roleOf(me.id) == MeetingRole.HOST, isReadOnly = meeting.isNoticeReadOnly())
            }
            refresh()
        }
    }

    // 작성·수정 화면에서 돌아왔을 때도 부른다.
    fun refresh() {
        viewModelScope.launch {
            val result = noticeRepository.getNotices(meetingId)
            state = state.copy(notices = result.getOrDefault(state.notices), isLoading = false, loadFailed = result.isFailure)
        }
    }

    fun onDelete(noticeId: String) {
        if (!state.canManage) return
        viewModelScope.launch {
            noticeRepository.deleteNotice(noticeId)
            refresh()
        }
    }
}

// 8a·8b 작성 / 8c 수정 (noticeId가 있으면 수정)
class NoticeEditViewModel(
    private val meetingId: String,
    private val noticeId: String?,
    private val noticeRepository: NoticeRepository,
) : ViewModel() {

    var state by mutableStateOf(NoticeEditState(isEditMode = noticeId != null, isLoading = noticeId != null))
        private set

    init {
        if (noticeId != null) {
            viewModelScope.launch {
                val notice = noticeRepository.getNotice(noticeId).getOrNull()
                state = state.copy(title = notice?.title.orEmpty(), body = notice?.body.orEmpty(), isLoading = false)
            }
        }
    }

    fun onTitleChange(title: String) {
        state = state.copy(title = title)
    }

    fun onBodyChange(body: String) {
        state = state.copy(body = body.take(NOTICE_BODY_MAX_LENGTH))
    }

    fun onSubmit() {
        if (!state.canSubmit) return
        state = state.copy(isSubmitting = true, submitFailed = false)
        viewModelScope.launch {
            val title = state.title.trim()
            val body = state.body.trim()
            val result = if (noticeId == null) {
                noticeRepository.createNotice(meetingId, title, body)
            } else {
                noticeRepository.updateNotice(noticeId, title, body)
            }
            state = state.copy(isSubmitting = false, isDone = result.isSuccess, submitFailed = result.isFailure)
        }
    }
}

// 8f~8i 공지 상세 + 댓글
class NoticeDetailViewModel(
    private val meetingId: String,
    private val noticeId: String,
    private val noticeRepository: NoticeRepository,
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    var state by mutableStateOf(NoticeDetailState())
        private set

    init {
        viewModelScope.launch {
            val meeting = meetingRepository.getMeeting(meetingId).getOrNull()
            val me = userRepository.getCurrentUser().getOrNull()
            if (meeting != null && me != null) {
                state = state.copy(
                    myUserId = me.id,
                    isHost = meeting.roleOf(me.id) == MeetingRole.HOST,
                    isReadOnly = meeting.isNoticeReadOnly(),
                )
            }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val notice = noticeRepository.getNotice(noticeId).getOrNull()
            state = state.copy(
                notice = notice,
                loadFailed = notice == null,
                comments = noticeRepository.getComments(noticeId).getOrDefault(emptyList()),
            )
        }
    }

    fun onCommentInputChange(text: String) {
        state = state.copy(commentInput = text)
    }

    fun onSend() {
        if (!state.canSend || !state.canWriteComment) return
        val body = state.commentInput.trim()
        state = state.copy(isSending = true)
        viewModelScope.launch {
            val ok = noticeRepository.addComment(noticeId, body).isSuccess
            state = state.copy(
                commentInput = if (ok) "" else state.commentInput,
                isSending = false,
                comments = noticeRepository.getComments(noticeId).getOrDefault(state.comments),
            )
        }
    }

    fun onDeleteComment(commentId: String) {
        val comment = state.comments.find { it.id == commentId } ?: return
        if (!state.isMine(comment)) return
        viewModelScope.launch {
            noticeRepository.deleteComment(commentId)
            state = state.copy(comments = noticeRepository.getComments(noticeId).getOrDefault(emptyList()))
        }
    }

    fun onDeleteNotice() {
        if (!state.canManageNotice) return
        viewModelScope.launch {
            if (noticeRepository.deleteNotice(noticeId).isSuccess) state = state.copy(isDeleted = true)
        }
    }
}
