package com.example.myapplication.ui.mate.review

import com.example.myapplication.data.model.Review
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.repository.FakeReviewRepository
import com.example.myapplication.data.repository.ReviewSession
import com.example.myapplication.data.repository.ReviewTargetInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

// 메이트 평가(12a~12c) 흐름과 규칙 (service-rules 11장)
class ReviewStateTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    private val state = ReviewState(
        session = ReviewSession(
            meetingId = "m1",
            meetingTitle = "등·이두 같이 치실 분",
            meetingStartAtMillis = 1789380000000L, // 9/14 19:00
            placeName = "스포애니 합정점",
            reviewDueAtMillis = 1789984800000L, // 9/21 19:00
            targets = listOf(
                ReviewTargetInfo("a", "서연", isReviewed = false, isMate = false, isMateRequested = false),
                ReviewTargetInfo("b", "진현", isReviewed = true, isMate = true, isMateRequested = false),
                ReviewTargetInfo("c", "우진", isReviewed = false, isMate = false, isMateRequested = true),
            ),
        ),
    )

    @Test
    fun 목록_머리글_12a() {
        assertEquals("9/14 · 스포애니 합정점 · 9/21까지 평가 가능", state.meetingSubtitle(seoul))
        assertEquals("함께한 메이트 3", state.targetHeader)
    }

    @Test
    fun 진행_표시와_다음_사람_12b() {
        val s = state.copy(currentTargetId = "a")
        assertEquals("1 / 3", s.progressText)
        assertEquals("서연님과 함께한 시간은 어땠나요?", s.question)
        assertEquals("c", s.nextTargetId) // 진현은 이미 평가함
        assertEquals("제출하고 다음 메이트", s.submitLabel)

        val last = s.copy(currentTargetId = "c", session = s.session!!.copy(targets = s.targets.map { if (it.userId == "a") it.copy(isReviewed = true) else it }))
        assertNull(last.nextTargetId)
        assertEquals("평가 제출", last.submitLabel)
        assertEquals("3 / 3", last.progressText)
    }

    @Test
    fun 판정을_골라야_제출_가능_12b1() {
        val s = state.copy(currentTargetId = "a")
        assertFalse(s.canSubmit)
        assertFalse(s.showDetails)
        val picked = s.copy(draft = s.draft.withVerdict(ReviewVerdict.GOOD))
        assertTrue(picked.canSubmit)
        assertTrue(picked.showDetails)
    }

    @Test
    fun 태그_묶음과_질문_12b2_12b4() {
        val good = state.copy(draft = ReviewDraft().withVerdict(ReviewVerdict.GOOD))
        assertEquals("어떤 점이 좋았나요?", good.tagQuestion)
        assertTrue(good.availableTags.all { it.isPositive })
        val normal = state.copy(draft = ReviewDraft().withVerdict(ReviewVerdict.NORMAL))
        assertTrue(normal.availableTags.all { it.isPositive })
        val bad = state.copy(draft = ReviewDraft().withVerdict(ReviewVerdict.BAD))
        assertEquals("어떤 점이 아쉬웠나요?", bad.tagQuestion)
        assertTrue(bad.availableTags.none { it.isPositive })
    }

    @Test
    fun 판정을_바꾸면_다른_묶음_태그는_지움() {
        val draft = ReviewDraft().withVerdict(ReviewVerdict.GOOD).toggled(ReviewTag.ON_TIME)
        assertEquals(setOf(ReviewTag.ON_TIME), draft.withVerdict(ReviewVerdict.NORMAL).tags)
        assertTrue(draft.withVerdict(ReviewVerdict.BAD).tags.isEmpty())
        assertTrue(draft.toggled(ReviewTag.ON_TIME).tags.isEmpty())
    }

    @Test
    fun 완료_화면_메이트_제안_12c() {
        assertEquals(listOf("a", "c"), state.mateSuggestions.map { it.userId })
        assertEquals("함께한 메이트 1명 평가 완료", state.doneSubtitle)
    }

    @Test
    fun 제출한_평가는_다시_못함() = runBlocking {
        val repo = FakeReviewRepository()
        val meetingId = "test-${System.nanoTime()}"
        val target = repo.getReviewSession(meetingId).getOrThrow().targets.first { !it.isReviewed }.userId
        val review = Review(meetingId = meetingId, targetUserId = target, verdict = ReviewVerdict.GOOD, tags = listOf(ReviewTag.ON_TIME.label))
        assertTrue(repo.submitReview(review).isSuccess)
        assertTrue(repo.getReviewSession(meetingId).getOrThrow().targets.first { it.userId == target }.isReviewed)
        assertTrue(repo.submitReview(review.copy(verdict = ReviewVerdict.BAD)).isFailure)
    }

    @Test
    fun 판정_글자() {
        assertEquals(listOf("좋음", "보통", "아쉬움"), ReviewVerdict.entries.map { it.label })
    }
}
