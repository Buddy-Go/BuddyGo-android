package com.example.myapplication.ui.mate.review

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.Review
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.repository.FakeReviewRepository
import com.example.myapplication.data.repository.ReviewRepository
import com.example.myapplication.data.repository.ReviewSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

// 평가를 제출한 뒤 명단 재조회가 실패해도 이미 평가한 사람에게 되돌아가지 않는지 (12a·12b·12c)
// FakeReviewRepository 명단: 서연(noRecord)·유나(hostB)는 아직, 지훈(hostA)은 이미 평가함
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewRefetchFailureTest {
    private var vm: ReviewViewModel? = null

    @Before fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun 정리() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private val seoyeon = MockUsers.noRecord.id
    private val jihoon = MockUsers.hostA.id
    private val yuna = MockUsers.hostB.id

    private fun viewModel(meetingId: String, repo: ReviewRepository) = ReviewViewModel(meetingId, repo).also { vm = it }

    // 12b에서 지금 사람에게 좋음 + 태그 하나를 골라 제출한다.
    private fun fillAndSubmit(v: ReviewViewModel) {
        v.onVerdictSelect(ReviewVerdict.GOOD)
        v.onTagToggle(ReviewTag.PUNCTUAL)
        v.onSubmit()
    }

    private fun ReviewViewModel.isReviewed(userId: String) = state.session!!.targets.first { it.userId == userId }.isReviewed

    @Test
    fun 재조회가_실패해도_제출한_사람은_평가_완료이고_다음_사람으로_간다() {
        val repo = FlakyReviewRepository()
        val v = viewModel("m-refetch-1", repo)
        v.onStartReview(seoyeon)
        repo.fetchError = IOException()
        fillAndSubmit(v)

        val s = v.state
        assertTrue("제출한 사람은 옛 명단이어도 평가 완료다", v.isReviewed(seoyeon))
        assertEquals("이미 평가한 지훈을 건너뛰고 유나로 간다", yuna, s.currentTargetId)
        assertFalse(s.isAllDone)
        assertFalse(s.isSubmitting)
        assertFalse("제출은 성공했다", s.submitFailed)
        assertNull("작성 중이던 평가는 비워진다", s.draft.verdict)
        assertTrue("재조회 실패는 안내 띠로 알린다", s.refreshFailed)
        assertFalse("명단이 있으니 전체 오류 화면은 아니다", s.loadFailed)
    }

    @Test
    fun 재조회가_계속_실패해도_마지막_제출_뒤에는_완료_화면으로_간다() {
        val repo = FlakyReviewRepository()
        val v = viewModel("m-refetch-2", repo)
        v.onStartReview(seoyeon)
        repo.fetchError = IOException()
        fillAndSubmit(v) // → 유나
        fillAndSubmit(v) // 마지막 사람. 옛 명단이면 서연이 '다음'으로 돌아왔다.

        val s = v.state
        assertTrue(s.isAllDone)
        assertNull(s.currentTargetId)
        assertEquals(3, s.reviewedCount)
        assertEquals("함께한 메이트 3명 평가 완료", s.doneSubtitle)
        assertEquals(listOf(true, true, true), s.targets.map { it.isReviewed })
    }

    @Test
    fun 다시_불러오기에_성공하면_안내가_사라지고_진행은_그대로다() {
        val repo = FlakyReviewRepository()
        val v = viewModel("m-refetch-3", repo)
        v.onStartReview(seoyeon)
        repo.fetchError = IOException()
        fillAndSubmit(v)

        repo.fetchError = null
        v.reload()

        val s = v.state
        assertFalse(s.refreshFailed)
        assertFalse(s.loadFailed)
        assertTrue(v.isReviewed(seoyeon))
        assertEquals(yuna, s.currentTargetId)
    }

    @Test
    fun 다시_불러오기도_실패하면_명단은_그대로_두고_안내만_남긴다() {
        val repo = FlakyReviewRepository()
        val v = viewModel("m-refetch-4", repo)
        v.onStartReview(seoyeon)
        repo.fetchError = IOException()
        fillAndSubmit(v)

        v.reload()

        val s = v.state
        assertFalse("보여 줄 명단이 있으면 전체 오류로 가리지 않는다", s.loadFailed)
        assertTrue(s.refreshFailed)
        assertEquals(3, s.targets.size)
        assertTrue(v.isReviewed(seoyeon))
        assertEquals(yuna, s.currentTargetId)
    }

    @Test
    fun 서버가_늦은_옛_명단을_돌려줘도_제출한_사람은_평가_완료다() {
        val repo = FlakyReviewRepository()
        val v = viewModel("m-refetch-5", repo)
        v.onStartReview(seoyeon)
        fillAndSubmit(v)

        repo.staleIds = setOf(seoyeon) // 서버가 아직 '평가 안 함'으로 돌려준다
        v.reload()

        assertFalse(v.state.refreshFailed)
        assertTrue(v.isReviewed(seoyeon))
    }

    @Test
    fun 처음부터_못_불러오면_전체_오류이고_다시_불러오면_풀린다() {
        val repo = FlakyReviewRepository().apply { fetchError = IOException() }
        val v = viewModel("m-refetch-6", repo)
        assertNull(v.state.session)
        assertTrue(v.state.loadFailed)
        assertFalse("보여 줄 명단이 없으면 안내 띠가 아니라 전체 오류다", v.state.refreshFailed)

        repo.fetchError = null
        v.reload()
        assertFalse(v.state.loadFailed)
        assertFalse(v.state.refreshFailed)
        assertEquals(3, v.state.targets.size)
    }

    @Test
    fun 제출이_거절되면_평가_완료로_바꾸지_않는다() {
        val repo = FlakyReviewRepository().apply { submitError = IllegalStateException("평가 기한이 지났어요") }
        val v = viewModel("m-refetch-7", repo)
        v.onStartReview(seoyeon)
        fillAndSubmit(v)

        val s = v.state
        assertTrue(s.submitFailed)
        assertFalse(s.refreshFailed)
        assertFalse(v.isReviewed(seoyeon))
        assertEquals(seoyeon, s.currentTargetId)
        assertFalse(s.isAllDone)
        assertTrue("원래 평가한 사람은 그대로", v.isReviewed(jihoon))
    }
}

// 제출은 FakeReviewRepository 그대로 받고, 명단 재조회·제출만 필요할 때 실패하거나 늦은 값을 돌려주게 바꾼다.
private class FlakyReviewRepository(
    private val delegate: ReviewRepository = FakeReviewRepository(),
) : ReviewRepository by delegate {
    var fetchError: Throwable? = null
    var submitError: Throwable? = null
    var staleIds: Set<String> = emptySet() // 서버가 아직 '평가 안 함'으로 돌려주는 사람들

    override suspend fun getReviewSession(meetingId: String): Result<ReviewSession> {
        fetchError?.let { return Result.failure(it) }
        return delegate.getReviewSession(meetingId).map { session ->
            session.copy(targets = session.targets.map { if (it.userId in staleIds) it.copy(isReviewed = false) else it })
        }
    }

    override suspend fun submitReview(review: Review): Result<Unit> =
        submitError?.let { Result.failure(it) } ?: delegate.submitReview(review)
}
