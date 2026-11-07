package com.example.myapplication.ui.mypage

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.ProfileReview
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.repository.FakeMateRepository
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AllReviewsTest {
    private var vm: AllReviewsViewModel? = null

    @Before fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun 정리() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun 받은_후기는_한_줄_모델로_바뀌고_운동과_날짜가_붙는다() {
        val row = MockUsers.meReviews.first().toRow()
        assertEquals("서연", row.author)
        assertTrue(row.meta!!.startsWith("등산 · "))
        assertEquals(ReviewVerdict.GOOD, row.verdict)
    }

    @Test
    fun 메이트_프로필_후기는_판정이_없고_태그가_있으면_태그로_붙는다() {
        val row = ProfileReview("u2", "서연", 1_788_220_800_000L, "시간 잘 지킴", "좋았어요").toRow()
        assertNull(row.verdict)
        assertEquals(listOf("시간 잘 지킴"), row.tags)
        assertEquals(emptyList<String>(), ProfileReview("u2", "서연", 0L, null, "x").toRow().tags)
    }

    @Test
    fun 최신순으로_정렬한다() {
        val rows = listOf(ReviewRow("a", "가", null, null, null, createdAtMillis = 1), ReviewRow("b", "나", null, null, null, createdAtMillis = 3), ReviewRow("c", "다", null, null, null, createdAtMillis = 2))
        assertEquals(listOf("b", "c", "a"), rows.newestFirst().map { it.id })
    }

    @Test
    fun 내_후기를_불러온다() {
        val v = AllReviewsViewModel(users = FakeUserRepository()).also { vm = it }
        val data = (v.state.value as UiState.Success).data
        assertEquals(MockUsers.meReviews.size, data.size)
        assertEquals(data.sortedByDescending { it.createdAtMillis }, data)
    }

    @Test
    fun 메이트의_후기를_불러오고_없는_사람은_오류다() {
        val ok = AllReviewsViewModel(MockUsers.hostA.id, FakeUserRepository(), FakeMateRepository()).also { vm = it }
        assertTrue(ok.state.value is UiState.Success)
        ok.viewModelScope.cancel()
        val bad = AllReviewsViewModel("없는-사람", FakeUserRepository(), FakeMateRepository()).also { vm = it }
        assertTrue(bad.state.value is UiState.Error)
    }
}
