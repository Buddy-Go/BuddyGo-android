package com.example.myapplication.ui.record

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.SignUpDraftStore
import com.example.myapplication.data.repository.UserRepository
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

// 프로필 기록 수정: 모임 신청·상세의 '기록 추가'·'기준 줄'에서 들어와 내 기록을 바꾼다.
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRecordTest {
    private var vm: RecordViewModel? = null

    @Before fun 준비() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        SignUpDraftStore.clear()
    }

    @After fun 정리() {
        vm?.viewModelScope?.cancel()
        SignUpDraftStore.clear()
        Dispatchers.resetMain()
    }

    private fun profile(users: UserRepository, sport: Sport = Sport.HEALTH) = RecordViewModel(sport, users).also { vm = it }
    private fun record(users: UserRepository) = runBlocking { users.getCurrentUser().getOrThrow().record }

    @Test
    fun 열면_저장된_내_기록으로_채워진다() {
        val v = profile(FakeUserRepository())
        assertEquals(MockUsers.me.record.benchPressKg, v.state.value.benchPressKg)
        assertEquals(MockUsers.me.record.armLevel, v.state.value.armLevel)
    }

    @Test
    fun 저장하면_프로필_기록이_바뀌고_다른_종목은_그대로다() {
        val users = FakeUserRepository()
        val v = profile(users)
        v.onStepBench(+1)
        v.onSelectArm(SkillLevel.ADVANCED)
        v.onSave()
        assertTrue(v.state.value.saved)
        val saved = record(users)
        assertEquals(SkillLevel.ADVANCED, saved.armLevel)
        assertTrue((saved.benchPressKg ?: 0) > (MockUsers.me.record.benchPressKg ?: 0))
        assertEquals("러닝·등산 기록은 그대로", MockUsers.me.record.runningPaceSec, saved.runningPaceSec)
        assertEquals(MockUsers.me.record.hikingStyle, saved.hikingStyle)
    }

    @Test
    fun 저장한_기록은_다시_열면_보이고_사용자_조회에도_반영된다() {
        val users = FakeUserRepository()
        profile(users).also { it.onSelectArm(SkillLevel.BEGINNER); it.onSave() }
        assertEquals(SkillLevel.BEGINNER, profile(users).state.value.armLevel)
        assertEquals(SkillLevel.BEGINNER, runBlocking { users.getUser(MockUsers.me.id).getOrThrow() }.record.armLevel)
    }

    @Test
    fun 가입_중_입력과는_섞이지_않는다() {
        val users = FakeUserRepository()
        SignUpDraftStore.update { it.copy(record = ExerciseRecord(benchPressKg = 15)) }
        val v = profile(users)
        assertEquals("프로필 수정은 가입 입력값이 아니라 내 기록에서 시작", MockUsers.me.record.benchPressKg, v.state.value.benchPressKg)
        v.onSave()
        assertEquals("가입 입력값은 건드리지 않는다", 15, SignUpDraftStore.draft.value.record.benchPressKg)
    }

    @Test
    fun 가입_중_모드는_그대로_가입_입력값에_저장한다() {
        val v = RecordViewModel(Sport.HEALTH).also { vm = it }
        v.onTogglePart(com.example.myapplication.data.model.BodyPart.CHEST) // 고른 부위만 저장된다
        v.onStepBench(+1)
        v.onSave()
        assertTrue(v.state.value.saved)
        assertEquals(RecordState.DEFAULT_BENCH_KG + RecordState.WEIGHT_STEP, SignUpDraftStore.draft.value.record.benchPressKg)
    }

    @Test
    fun 저장에_실패하면_화면에_남고_다시_시도할_수_있다() {
        val failing = object : UserRepository by FakeUserRepository() {
            override suspend fun updateRecord(record: ExerciseRecord) = Result.failure<Unit>(java.io.IOException())
        }
        val v = profile(failing)
        v.onSave()
        assertFalse(v.state.value.saved)
        assertTrue(v.state.value.saveFailed)
        assertFalse(v.state.value.saving)
        v.onSaveFailureShown()
        assertFalse(v.state.value.saveFailed)
        v.onSave()
        assertTrue("실패 뒤에도 다시 저장을 시도한다", v.state.value.saveFailed)
    }

    @Test
    fun 기록이_늦게_도착해도_이미_손댄_입력은_덮어쓰지_않는다() {
        // 불러오기가 끝나기 전에 입력하는 상황을 만들려고, 사용자 조회를 직접 풀어 줄 때까지 멈춘다.
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val slow = object : UserRepository by FakeUserRepository() {
            override suspend fun getCurrentUser(): Result<com.example.myapplication.data.model.User> {
                gate.await()
                return Result.success(MockUsers.me)
            }
        }
        val v = profile(slow)
        v.onSelectArm(SkillLevel.BEGINNER) // 아직 기록이 도착하기 전
        gate.complete(Unit)
        assertEquals("손댄 값이 남는다", SkillLevel.BEGINNER, v.state.value.armLevel)
    }
}
