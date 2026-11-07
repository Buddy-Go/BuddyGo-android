package com.example.myapplication.ui.mate.review

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.data.repository.FakeMateRepository
import com.example.myapplication.data.repository.FakeReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 12c '메이트 신청'이 실제로 신청을 보내는지
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewMateRequestTest {
    private var vm: ReviewViewModel? = null

    @Before fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun 정리() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun target(v: ReviewViewModel, id: String) = v.state.session!!.targets.first { it.userId == id }

    @Test
    fun 신청하면_신청함으로_바뀌고_메이트_저장소에도_신청이_간다() {
        val mates = FakeMateRepository()
        val v = ReviewViewModel("m-mate-1", FakeReviewRepository(), mates).also { vm = it }
        val id = MockUsers.noRecord.id
        v.onMateRequest(id)
        assertTrue(target(v, id).isMateRequested)
        val person = runBlocking { mates.getWorkedOut().getOrThrow() }.first { it.userId == id }
        assertEquals(MateRelation.REQUESTED, person.relation)
    }

    @Test
    fun 저장소가_거절하면_신청함을_되돌린다() {
        val repo = object : com.example.myapplication.data.repository.MateRepository by FakeMateRepository() {
            override suspend fun sendRequest(userId: String) = Result.failure<Unit>(IllegalStateException("함께 운동한 사람에게만 신청할 수 있어요"))
        }
        val v = ReviewViewModel("m-mate-3", FakeReviewRepository(), repo).also { vm = it }
        val id = v.state.session!!.targets.first { !it.isMateRequested && !it.isMate }.userId
        v.onMateRequest(id)
        assertFalse(target(v, id).isMateRequested)
    }

    @Test
    fun 이미_신청한_사람에게는_다시_보내지_않는다() {
        var calls = 0
        val repo = object : com.example.myapplication.data.repository.MateRepository by FakeMateRepository() {
            override suspend fun sendRequest(userId: String): Result<Unit> { calls++; return Result.success(Unit) }
        }
        val v = ReviewViewModel("m-mate-4", FakeReviewRepository(), repo).also { vm = it }
        val id = v.state.session!!.targets.first { !it.isMateRequested && !it.isMate }.userId
        v.onMateRequest(id)
        v.onMateRequest(id)
        assertEquals(1, calls)
    }

    @Test
    fun 저장소가_없으면_화면_상태만_바꾼다() {
        val v = ReviewViewModel("m-mate-5", FakeReviewRepository()).also { vm = it }
        val id = v.state.session!!.targets.first { !it.isMateRequested && !it.isMate }.userId
        v.onMateRequest(id)
        assertTrue(target(v, id).isMateRequested)
    }
}
