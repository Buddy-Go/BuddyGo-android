package com.example.myapplication.ui.record

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.repository.SignUpDraftStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordStateTest {
    @Before
    fun 입력값_저장소_비우기() = SignUpDraftStore.clear()

    @Test
    fun 선택한_부위는_가슴_등_하체_어깨_팔_순서() {
        val state = RecordState(parts = setOf(BodyPart.ARM, BodyPart.CHEST, BodyPart.LEGS))
        assertEquals(listOf(BodyPart.CHEST, BodyPart.LEGS, BodyPart.ARM), state.selectedParts)
    }

    @Test
    fun 일알엠_제목은_첫_번째_일알엠_부위에만_붙는다() {
        assertEquals(BodyPart.LEGS, RecordState(parts = setOf(BodyPart.LEGS, BodyPart.SHOULDER)).firstOneRmPart)
        assertEquals(BodyPart.CHEST, RecordState(parts = setOf(BodyPart.CHEST, BodyPart.LEGS)).firstOneRmPart)
        assertNull("등·팔은 1RM이 아님", RecordState(parts = setOf(BodyPart.BACK, BodyPart.ARM)).firstOneRmPart)
    }

    @Test
    fun 선택하지_않은_부위는_기록에서_비운다() {
        val state = RecordState(parts = setOf(BodyPart.CHEST, BodyPart.ARM), benchPressKg = 80, squatKg = 100, armLevel = SkillLevel.ADVANCED)
        val record = state.toRecord(ExerciseRecord(runningPaceSec = 330))
        assertEquals(80, record.benchPressKg)
        assertNull(record.squatKg)
        assertEquals(SkillLevel.ADVANCED, record.armLevel)
        assertEquals("다른 종목 기록은 그대로", 330, record.runningPaceSec)
    }

    @Test
    fun 저장해_둔_기록이_있으면_그_부위가_선택된_상태로_복원된다() {
        val state = RecordState.from(ExerciseRecord(benchPressKg = 85, pullUpCount = 0))
        assertEquals(setOf(BodyPart.CHEST, BodyPart.BACK), state.parts)
        assertEquals(85, state.benchPressKg)
        assertEquals("턱걸이 0회도 입력한 값", 0, state.pullUpCount)
        assertEquals("기록이 없는 부위는 기본값", RecordState.DEFAULT_SQUAT_KG, state.squatKg)
    }

    @Test
    fun 무게는_5kg씩_0에서_400까지() {
        val vm = RecordViewModel()
        vm.onStepBench(1)
        assertEquals(RecordState.DEFAULT_BENCH_KG + 5, vm.state.value.benchPressKg)
        repeat(200) { vm.onStepBench(1) }
        assertEquals(RecordState.MAX_WEIGHT, vm.state.value.benchPressKg)
        repeat(200) { vm.onStepBench(-1) }
        assertEquals(0, vm.state.value.benchPressKg)
    }

    @Test
    fun 턱걸이는_1회씩_0에서_100까지() {
        val vm = RecordViewModel()
        vm.onStepPullUp(-1)
        assertEquals("0회 아래로 내려가지 않음", 0, vm.state.value.pullUpCount)
        vm.onStepPullUp(1)
        assertEquals(1, vm.state.value.pullUpCount)
        repeat(200) { vm.onStepPullUp(1) }
        assertEquals(RecordState.MAX_PULL_UP, vm.state.value.pullUpCount)
    }

    @Test
    fun 저장하면_입력값_저장소에_기록이_들어간다() {
        val vm = RecordViewModel()
        vm.onTogglePart(BodyPart.CHEST)
        vm.onStepBench(1) // 40 → 45
        vm.onTogglePart(BodyPart.BACK) // 턱걸이 기본 0회
        assertFalse(vm.state.value.saved)
        vm.onSave()
        val record = SignUpDraftStore.draft.value.record
        assertEquals(45, record.benchPressKg)
        assertEquals(0, record.pullUpCount)
        assertNull(record.squatKg)
        assertTrue(vm.state.value.saved)
    }
}
