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
import com.example.myapplication.data.repository.ReviewSession
import kotlinx.coroutines.launch

// 12a·12b·12c가 같이 쓰는 ViewModel. 세 화면이 같은 인스턴스를 받도록 화면을 띄우는 쪽에서 하나만 만든다.
class ReviewViewModel(
    private val meetingId: String,
    private val repository: ReviewRepository,
    private val mateRepository: MateRepository? = null, // 12c '메이트 신청'을 실제로 보낸다. 없으면 화면 상태만 바꾼다
) : ViewModel() {

    var state by mutableStateOf(ReviewState())
        private set

    // 이 화면에서 제출에 성공한 사람. 서버 명단을 못 받거나 늦은 값을 받아도 평가 완료로 둔다.
    // init의 reload()가 이 값을 읽으므로 init보다 위에 둔다.
    private val submittedTargetIds = mutableSetOf<String>()

    init {
        reload()
    }

    // 12a 처음 불러오기, 그리고 '다시 불러오기'
    fun reload() {
        viewModelScope.launch {
            applyFetched(repository.getReviewSession(meetingId))
        }
    }

    // 받은 명단을 반영한다. 못 받았을 때 보여 줄 명단이 없으면 16g 전체 오류, 있으면 화면은 그대로 두고 안내 띠만 띄운다.
    private fun applyFetched(result: Result<ReviewSession>) {
        val fetched = result.getOrNull()
        state = state.copy(
            session = (fetched ?: state.session)?.withReviewed(submittedTargetIds),
            loadFailed = fetched == null && state.session == null,
            refreshFailed = fetched == null && state.session != null,
            now = System.currentTimeMillis(),
        )
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
        state = state.copy(isSubmitting = true, submitFailed = false)
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
                // 기한이 지났거나 이미 평가했으면 서버가 거절한다. 16i 안내 띠로 알린다.
                state = state.copy(isSubmitting = false, submitFailed = true)
                return@launch
            }
            // 제출은 서버에 반영됐다. 이어지는 재조회가 실패해도 이 사람은 평가 완료로 두고 다음으로 넘어간다.
            // (옛 명단 그대로면 이미 평가한 사람이 '다음 메이트'로 돌아오고, 마지막 제출 뒤에도 12c로 못 간다)
            submittedTargetIds += target
            val fetched = repository.getReviewSession(meetingId).getOrNull()
            val refreshed = state.copy(session = (fetched ?: state.session)?.withReviewed(submittedTargetIds))
            val next = refreshed.nextTargetId
            state = refreshed.copy(
                isSubmitting = false,
                draft = ReviewDraft(),
                currentTargetId = next,
                isAllDone = next == null,
                loadFailed = false,
                refreshFailed = fetched == null, // 막지 않고 안내 띠 + '다시 불러오기'로 알린다
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
