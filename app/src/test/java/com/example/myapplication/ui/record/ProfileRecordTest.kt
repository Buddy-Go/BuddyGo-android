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
            override suspend fun getMyRecord(): Result<ExerciseRecord> {
                gate.await()
                return Result.success(MockUsers.me.record)
            }
        }
        val v = profile(slow)
        v.onSelectArm(SkillLevel.BEGINNER) // 아직 기록이 도착하기 전
        gate.complete(Unit)
        assertEquals("손댄 값이 남는다", SkillLevel.BEGINNER, v.state.value.armLevel)
    }

    // ---- 저장된 기록을 불러오지 못했을 때: 빈 값으로 저장해 기존 기록이 지워지면 안 된다 ----

    @Test
    fun 기록을_불러오지_못하면_오류_상태가_되고_저장하지_못한다() {
        var updated = false
        val broken = object : UserRepository by FakeUserRepository() {
            override suspend fun getMyRecord(): Result<ExerciseRecord> = Result.failure(java.io.IOException())
            override suspend fun updateRecord(record: ExerciseRecord): Result<Unit> {
                updated = true
                return Result.success(Unit)
            }
        }
        val v = profile(broken)
        assertTrue(v.state.value.loadFailed)
        assertFalse(v.state.value.loading)
        assertFalse(v.state.value.canSave)

        v.onTogglePart(com.example.myapplication.data.model.BodyPart.CHEST) // 비어 보이는 화면에서 무언가 골라도
        v.onSave()
        assertFalse("불러오지 못한 채로는 서버에 저장 요청을 보내지 않는다", updated)
        assertFalse(v.state.value.saving)
        assertFalse(v.state.value.saved)
        assertFalse(v.state.value.saveFailed)
    }

    @Test
    fun 러닝_등산_화면도_기록을_불러오지_못하면_저장하지_못한다() {
        // 러닝·등산을 저장해도 헬스 기록은 함께 서버로 가므로, 헬스 기록을 모르는 채로 저장하면 지워진다.
        var updated = false
        val broken = object : UserRepository by FakeUserRepository() {
            override suspend fun getMyRecord(): Result<ExerciseRecord> = Result.failure(java.io.IOException())
            override suspend fun updateRecord(record: ExerciseRecord): Result<Unit> {
                updated = true
                return Result.success(Unit)
            }
        }
        profile(broken, Sport.RUNNING).also { it.onSelectPace(330); it.onSave() }
        assertFalse(updated)
        profile(broken, Sport.HIKING).also { it.onSave() }
        assertFalse(updated)
    }

    @Test
    fun 기록을_불러오는_동안에는_저장하지_못하고_끝나면_저장할_수_있다() {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val users = FakeUserRepository()
        val slow = object : UserRepository by users {
            override suspend fun getMyRecord(): Result<ExerciseRecord> {
                gate.await()
                return Result.success(MockUsers.me.record)
            }
        }
        val v = profile(slow)
        assertTrue(v.state.value.loading)
        v.onSave() // 아직 기존 기록을 모른다
        assertFalse(v.state.value.saved)
        assertFalse(v.state.value.saving)
        assertEquals("저장된 적 없음", MockUsers.me.record, record(users))

        gate.complete(Unit)
        assertFalse(v.state.value.loading)
        v.onSave()
        assertTrue(v.state.value.saved)
    }

    @Test
    fun 다시_불러오면_오류가_풀리고_기존_기록을_지키며_저장한다() {
        var failing = true
        val users = FakeUserRepository()
        val flaky = object : UserRepository by users {
            override suspend fun getMyRecord(): Result<ExerciseRecord> =
                if (failing) Result.failure(java.io.IOException()) else Result.success(MockUsers.me.record)
        }
        val v = profile(flaky)
        assertTrue(v.state.value.loadFailed)

        v.onRetryLoad() // 여전히 실패
        assertTrue(v.state.value.loadFailed)
        assertFalse(v.state.value.loading)

        failing = false
        v.onRetryLoad()
        assertFalse(v.state.value.loadFailed)
        assertFalse(v.state.value.loading)
        assertTrue(v.state.value.canSave)
        assertEquals("불러온 기존 기록으로 채워진다", MockUsers.me.record.benchPressKg, v.state.value.benchPressKg)

        v.onSelectArm(SkillLevel.ADVANCED)
        v.onSave()
        assertTrue(v.state.value.saved)
        val saved = record(users)
        assertEquals(SkillLevel.ADVANCED, saved.armLevel)
        assertEquals("기존 기록은 지워지지 않는다", MockUsers.me.record.benchPressKg, saved.benchPressKg)
        assertEquals(MockUsers.me.record.squatKg, saved.squatKg)
        assertEquals(MockUsers.me.record.pullUpCount, saved.pullUpCount)
    }

    @Test
    fun 불러온_뒤에는_다시_불러오기를_눌러도_입력을_덮어쓰지_않는다() {
        val v = profile(FakeUserRepository())
        v.onSelectArm(SkillLevel.BEGINNER)
        v.onRetryLoad() // 오류 상태가 아니므로 아무 일도 없다
        assertEquals(SkillLevel.BEGINNER, v.state.value.armLevel)
        assertFalse(v.state.value.loadFailed)
    }

    @Test
    fun 저장_직전에_기록을_다시_받지_못해도_처음_불러온_기록으로_저장한다() {
        var calls = 0
        val users = FakeUserRepository()
        val dropsLater = object : UserRepository by users {
            override suspend fun getMyRecord(): Result<ExerciseRecord> =
                if (calls++ == 0) Result.success(MockUsers.me.record) else Result.failure(java.io.IOException())
        }
        val v = profile(dropsLater)
        v.onSelectArm(SkillLevel.ADVANCED)
        v.onSave()
        assertTrue(v.state.value.saved)
        assertEquals("빈 기록으로 덮어쓰지 않는다", MockUsers.me.record.squatKg, record(users).squatKg)
    }

    @Test
    fun 가입_중_모드는_기록을_불러오지_않으므로_바로_저장할_수_있다() {
        val v = RecordViewModel(Sport.HEALTH).also { vm = it }
        assertFalse(v.state.value.loading)
        assertTrue(v.state.value.canSave)
    }
}
