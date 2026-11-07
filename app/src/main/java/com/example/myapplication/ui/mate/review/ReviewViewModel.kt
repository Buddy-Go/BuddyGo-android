package com.example.myapplication.ui.mate.review

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Review
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.repository.MateRepository
import com.example.myapplication.data.repository.ReviewRepository
import kotlinx.coroutines.launch

// 12a·12b·12c가 같이 쓰는 ViewModel. 세 화면이 같은 인스턴스를 받도록 화면을 띄우는 쪽에서 하나만 만든다.
class ReviewViewModel(
    private val meetingId: String,
    private val repository: ReviewRepository,
    private val mateRepository: MateRepository? = null, // 12c '메이트 신청'을 실제로 보낸다. 없으면 화면 상태만 바꾼다
) : ViewModel() {

    var state by mutableStateOf(ReviewState())
        private set

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            // TODO(임시): 실패하면 16f·16g 오류 화면 (공용 부품이 정해지면)
            state = state.copy(session = repository.getReviewSession(meetingId).getOrNull(), now = System.currentTimeMillis())
        }
    }

    // 12a '평가하기' → 12b
    fun onStartReview(targetUserId: String) {
        state = state.copy(currentTargetId = targetUserId, draft = ReviewDraft(), isAllDone = false)
    }

    fun onVerdictSelect(verdict: ReviewVerdict) {
        state = state.copy(draft = state.draft.withVerdict(verdict))
    }

    fun onTagToggle(tag: ReviewTag) {
        state = state.copy(draft = state.draft.toggled(tag))
    }

    fun onCommentChange(comment: String) {
        state = state.copy(draft = state.draft.copy(comment = comment.take(REVIEW_COMMENT_MAX_LENGTH)))
    }

    // 제출 → 다음 사람이 있으면 그 사람, 없으면 12c
    fun onSubmit() {
        val target = state.currentTargetId ?: return
        val verdict = state.draft.verdict ?: return
        if (state.isSubmitting) return
        state = state.copy(isSubmitting = true)
        viewModelScope.launch {
            val result = repository.submitReview(
                Review(
                    meetingId = meetingId,
                    targetUserId = target,
                    verdict = verdict,
                    tags = state.draft.tags.map { it.label },
                    comment = state.draft.comment.trim().ifEmpty { null },
                ),
            )
            if (result.isFailure) {
                // TODO(임시): 16i 인라인 오류 (공용 부품이 정해지면). 기한이 지났거나 이미 평가했으면 서버가 거절한다.
                state = state.copy(isSubmitting = false)
                return@launch
            }
            val next = state.nextTargetId
            val session = repository.getReviewSession(meetingId).getOrNull() ?: state.session
            state = state.copy(
                session = session,
                isSubmitting = false,
                draft = ReviewDraft(),
                currentTargetId = next,
                isAllDone = next == null,
            )
        }
    }

    // 12c '메이트 신청'
    // 먼저 '신청함'으로 바꾸고 보낸다. 서버가 거절하면(함께 운동한 사람이 아님 등) 되돌린다.
    fun onMateRequest(userId: String) {
        val session = state.session ?: return
        if (session.targets.firstOrNull { it.userId == userId }?.isMateRequested == true) return
        setRequested(userId, true)
        val repo = mateRepository ?: return
        viewModelScope.launch {
            if (repo.sendRequest(userId).isFailure) setRequested(userId, false)
        }
    }

    private fun setRequested(userId: String, requested: Boolean) {
        val session = state.session ?: return
        state = state.copy(
            session = session.copy(
                targets = session.targets.map { if (it.userId == userId) it.copy(isMateRequested = requested) else it },
            ),
        )
    }
}
