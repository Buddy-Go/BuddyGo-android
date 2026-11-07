package com.example.myapplication.ui.meeting.create

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeUserRepository
import java.util.TimeZone
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 모임 수정 (service-rules 4장): 기존 모임을 개설 화면과 같은 화면 상태로 바꾸고, 바뀐 값만 담아 보낸다.
@OptIn(ExperimentalCoroutinesApi::class)
class MeetingEditTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val hosted = MockMeetings.hosted
    private val myRecord = ExerciseRecord(benchPressKg = 80, armLevel = com.example.myapplication.data.model.SkillLevel.INTERMEDIATE)
    private val created = mutableListOf<MeetingCreateViewModel>()

    @Before fun 준비() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun 정리() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun vm(meeting: com.example.myapplication.data.model.Meeting = hosted, repo: FakeMeetingRepository = FakeMeetingRepository(listOf(meeting))) =
        MeetingCreateViewModel(FakeUserRepository(), repo, editMeetingId = meeting.id, nowMillis = { meeting.startAtMillis - 24 * 3_600_000L })
            .also { created += it }

    // ---- 상태 변환 ----

    @Test
    fun 고칠_모임의_값이_화면_상태로_채워진다() {
        val s = editStateOf(hosted, myRecord, hosted.startAtMillis - 3_600_000L, seoul)
        assertEquals(hosted.title, s.title)
        assertEquals(hosted.durationMinutes, s.durationMinutes)
        assertEquals(hosted.capacity, s.capacity)
        assertEquals(hosted.place.name, s.place?.name)
        assertEquals(hosted.id, s.editMeetingId)
        assertTrue(s.isEditMode)
        assertTrue(s.canSubmit)
        assertFalse("확정 참가자가 호스트뿐이면 잠기지 않는다", s.lockedCore)
    }

    @Test
    fun 시작_시각이_분_단위로_들어간다() {
        val s = editStateOf(hosted, myRecord, hosted.startAtMillis - 3_600_000L, seoul)
        val c = java.util.Calendar.getInstance(seoul).apply { timeInMillis = hosted.startAtMillis }
        assertEquals(c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE), s.startMinutes)
    }

    @Test
    fun 확정_참가자가_있으면_잠긴다() {
        val locked = hosted.copy(confirmedCount = 2, confirmedIds = listOf("u1", "u2"))
        assertTrue(editStateOf(locked, myRecord, 0, seoul).lockedCore)
    }

    @Test
    fun 날짜가_다섯_날_밖이어도_그_날짜_칩이_선택된다() {
        val far = hosted.copy(startAtMillis = hosted.startAtMillis + 30L * 24 * 3_600_000L)
        val s = editStateOf(far, myRecord, hosted.startAtMillis - 3_600_000L, seoul)
        assertEquals(6, s.dateOptions.size)
        assertEquals(5, s.selectedDateIndex)
    }

    @Test
    fun 날짜가_다섯_날_안이면_칩을_늘리지_않는다() {
        val s = editStateOf(hosted, myRecord, hosted.startAtMillis - 24 * 3_600_000L, seoul)
        assertEquals(5, s.dateOptions.size)
        assertNotNull(s.selectedDateIndex)
        assertEquals("오늘", buildDateOptions(0L, null, seoul).first().topLabel)
    }

    @Test
    fun 내_기록_표시는_모임에_저장된_기록이_있으면_켜져_있다() {
        val shown = hosted.copy(criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST), ExerciseRecord(benchPressKg = 70)))
        val hidden = hosted.copy(criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST), null))
        assertTrue(editStateOf(shown, myRecord, 0, seoul).healthCriteria.showMyRecord)
        assertEquals("저장된 기록을 그대로", 70, editStateOf(shown, myRecord, 0, seoul).healthCriteria.myRecord.benchPressKg)
        val h = editStateOf(hidden, myRecord, 0, seoul).healthCriteria
        assertFalse(h.showMyRecord)
        assertEquals("숨겼다면 켤 때 보여 줄 현재 프로필 기록", 80, h.myRecord.benchPressKg)
    }

    @Test
    fun 러닝_모임도_수정_화면_상태로_바뀌고_제출할_수_있다() {
        val running = MockMeetings.pending.copy(hostId = "u1")
        val s = editStateOf(running, myRecord, 0, seoul)
        assertEquals(com.example.myapplication.data.model.Sport.RUNNING, s.sport)
        assertEquals(5f, s.runningCriteria.distanceKm)
        assertEquals(345, s.runningCriteria.paceSec)
        assertTrue(s.canSubmit)
        assertNull(s.errorMessage)
        assertTrue(s.toUpdatedMeeting(seoul)!!.criteria is MeetingCriteria.Running)
    }

    @Test
    fun 등산_모임도_수정_화면_상태로_바뀌고_제출할_수_있다() {
        val hiking = MockMeetings.inProgress.copy(hostId = "u1", status = com.example.myapplication.data.model.MeetingStatus.RECRUITING)
        val s = editStateOf(hiking, myRecord, 0, seoul)
        assertEquals(com.example.myapplication.data.model.Sport.HIKING, s.sport)
        assertEquals(com.example.myapplication.data.model.HikingStyle.HOBBY, s.hikingStyle)
        assertTrue(s.canSubmit)
        assertEquals("취미", s.criteriaSummary)
        assertEquals("등산 기준", s.criteriaTitle)
    }

    @Test
    fun 수정_요청은_원본의_장소_좌표를_그대로_두고_상세주소만_바꾼다() {
        val s = editStateOf(hosted, myRecord, hosted.startAtMillis - 3_600_000L, seoul)
            .copy(title = " 새 제목 ", place = PlaceSelection(hosted.place.name, "후문 3층"))
        val m = s.toUpdatedMeeting(seoul)!!
        assertEquals("새 제목", m.title)
        assertEquals(hosted.id, m.id)
        assertEquals(hosted.place.latitude, m.place.latitude, 0.0)
        assertEquals("후문 3층", m.place.detailAddress)
        assertEquals(hosted.startAtMillis, m.startAtMillis)
        assertEquals(hosted.hostId, m.hostId)
    }

    @Test
    fun 상세주소를_비우면_없는_것으로_보낸다() {
        val s = editStateOf(hosted, myRecord, hosted.startAtMillis - 3_600_000L, seoul).copy(place = PlaceSelection(hosted.place.name, "  "))
        assertNull(s.toUpdatedMeeting(seoul)!!.place.detailAddress)
    }

    @Test
    fun 수정_모드가_아니면_수정_요청을_만들지_않는다() {
        assertNull(MeetingCreateState().toUpdatedMeeting(seoul))
    }

    // ---- ViewModel ----

    @Test
    fun 열면_고칠_모임을_불러와_채운다() {
        val v = vm()
        assertEquals(hosted.title, v.state.title)
        assertTrue(v.state.isEditMode)
        assertTrue(v.state.canSubmit)
    }

    @Test
    fun 불러오기_전에도_수정_모드이므로_새_모임이_만들어지지_않는다() {
        val repo = FakeMeetingRepository(listOf(hosted))
        val v = MeetingCreateViewModel(FakeUserRepository(), repo, editMeetingId = "없는-모임")
        created += v
        v.onSubmit()
        assertNull(v.state.createdMeetingId)
        assertEquals(1, runBlocking { repo.getMeetings().getOrThrow() }.size)
    }

    @Test
    fun 없는_모임을_고치려_하면_안내하고_닫는다() {
        val v = MeetingCreateViewModel(FakeUserRepository(), FakeMeetingRepository(listOf(hosted)), editMeetingId = "없는-모임")
        created += v
        assertNotNull(v.state.errorMessage)
        assertTrue(v.state.updated)
    }

    @Test
    fun 제목과_설명과_상세주소를_고치고_완료하면_저장된다() {
        val repo = FakeMeetingRepository(listOf(hosted))
        val v = vm(repo = repo)
        v.onTitleChange("새 제목")
        v.onDescriptionApply("새 설명")
        v.onDetailAddressApply("후문")
        v.onSubmit()
        assertTrue(v.state.updated)
        val saved = runBlocking { repo.getMeeting(hosted.id) }.getOrThrow()
        assertEquals("새 제목", saved.title)
        assertEquals("새 설명", saved.description)
        assertEquals("후문", saved.place.detailAddress)
    }

    @Test
    fun 확정_참가자가_있으면_일시_기준_정원은_눌러도_안_바뀐다() {
        val locked = hosted.copy(confirmedCount = 2, confirmedIds = listOf("u1", "u2"))
        val v = vm(locked)
        assertTrue(v.state.lockedCore)
        val before = v.state
        v.onTimeClick()
        v.onCriteriaClick()
        v.onDateSelect(3)
        assertNull(v.state.openSheet)
        assertEquals(before.selectedDateIndex, v.state.selectedDateIndex)
        v.onDescriptionClick()
        assertEquals(CreateSheet.DESCRIPTION, v.state.openSheet)
    }

    @Test
    fun 잠기지_않았을_때_시작을_90분_안으로_바꾸면_실패_안내가_나온다() {
        val repo = FakeMeetingRepository(listOf(hosted), nowMillis = { hosted.startAtMillis - 3_600_000L })
        val v = vm(repo = repo)
        v.onTimeApply(startMinutes = ((hosted.startAtMillis - 3_600_000L + 30 * 60_000L) / 60_000L % 1440).toInt(), durationMinutes = 60)
        v.onSubmit()
        assertFalse(v.state.updated)
        assertNotNull(v.state.errorMessage)
        v.onErrorShown()
        assertNull(v.state.errorMessage)
    }

    @Test
    fun 상세주소를_비우고_적용하면_상세주소가_없어진다() {
        val v = vm()
        v.onDetailAddressApply("  ")
        assertNull(v.state.place?.detail)
        assertEquals(hosted.place.name, v.state.place?.name)
        v.onDetailAddressApply("가".repeat(DETAIL_ADDRESS_MAX_LENGTH + 10))
        assertEquals(DETAIL_ADDRESS_MAX_LENGTH, v.state.place?.detail?.length)
    }
}
