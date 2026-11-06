package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.CriteriaJudgement
import com.example.myapplication.data.model.CriteriaMatchResult
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingMember
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.ui.common.CriteriaMatch
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingDetailRulesTest {
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val minute = 60_000L

    private fun millis(month: Int, day: Int, hour: Int, min: Int = 0) =
        Calendar.getInstance(seoul).apply { clear(); set(2026, month - 1, day, hour, min) }.timeInMillis

    private fun meeting(
        status: MeetingStatus = MeetingStatus.RECRUITING,
        start: Long = millis(9, 8, 7),
        confirmed: Int = 3,
        capacity: Int = 6,
        members: List<MeetingMember> = emptyList(),
    ) = Meeting(
        id = "m", status = status, startAtMillis = start, durationMinutes = 240,
        confirmedCount = confirmed, capacity = capacity, members = members,
    )

    // ---- 시각 ----

    @Test
    fun 일시는_날짜_요일_시각_진행시간_순이다() {
        assertEquals("9월 8일 (화) 오전 7:00 · 4시간 진행", formatDetailDateTime(millis(9, 8, 7), 240, seoul))
        assertEquals("9월 8일 (화) 오후 7:30 · 1시간 30분 진행", formatDetailDateTime(millis(9, 8, 19, 30), 90, seoul))
        assertEquals("9월 8일 (화) 오전 12:05", formatDetailDateTime(millis(9, 8, 0, 5), 0, seoul))
    }

    @Test
    fun 진행시간은_시간과_분으로_쓴다() {
        assertEquals("1시간 30분", formatDuration(90))
        assertEquals("2시간", formatDuration(120))
        assertEquals("45분", formatDuration(45))
        assertEquals("", formatDuration(0))
    }

    @Test
    fun 데드라인은_시작_한시간_반_전이다() {
        val m = meeting(start = millis(9, 8, 7))
        assertEquals(millis(9, 8, 5, 30), deadlineMillis(m))
    }

    @Test
    fun 마감까지_남은_시간은_일_시간_분_단위로_쓴다() {
        val m = meeting(start = millis(9, 8, 7)) // 데드라인 5:30
        assertEquals("마감까지 5시간", formatUntilDeadline(m, millis(9, 8, 0, 0)))
        assertEquals("마감까지 1일", formatUntilDeadline(m, millis(9, 7, 0, 0)))
        assertEquals("마감까지 40분", formatUntilDeadline(m, millis(9, 8, 4, 50)))
        assertEquals("마감까지 1분", formatUntilDeadline(m, deadlineMillis(m) - 5_000))
        assertNull("지났으면 null", formatUntilDeadline(m, deadlineMillis(m)))
    }

    // ---- 칩 ----

    @Test
    fun 상태_칩은_상태마다_문구와_색이_다르다() {
        assertEquals(StatusChip("모집중 3/6", ChipTone.Lime), statusChipsOf(meeting(), MeetingRole.NONE).single())
        assertEquals(StatusChip("마감 6/6", ChipTone.Dark), statusChipsOf(meeting(MeetingStatus.CLOSED, confirmed = 6), MeetingRole.NONE).single())
        assertEquals(StatusChip("진행중 · 6명", ChipTone.Lime), statusChipsOf(meeting(MeetingStatus.IN_PROGRESS, confirmed = 6), MeetingRole.CONFIRMED).single())
        assertEquals(StatusChip("종료", ChipTone.Gray), statusChipsOf(meeting(MeetingStatus.ENDED), MeetingRole.NONE).single())
        assertEquals(StatusChip("취소됨", ChipTone.Gray), statusChipsOf(meeting(MeetingStatus.CANCELED), MeetingRole.NONE).single())
    }

    @Test
    fun 신청_대기중이면_모집중_옆에_검토중_칩이_붙는다() {
        val chips = statusChipsOf(meeting(), MeetingRole.PENDING)
        assertEquals(listOf("모집중 3/6", "신청 검토중"), chips.map { it.text })
        assertEquals("모집중이 아니면 붙지 않는다", 1, statusChipsOf(meeting(MeetingStatus.CLOSED), MeetingRole.PENDING).size)
    }

    // ---- 참가자 영역 ----

    @Test
    fun 참가자_제목은_상태마다_다르다() {
        assertEquals("참여자 3 / 6", participantsTitle(meeting()))
        val members = listOf(MeetingMember("a", "민", true), MeetingMember("b", "서", false))
        assertEquals("참여자 6 / 6 · 체크인 1", participantsTitle(meeting(MeetingStatus.IN_PROGRESS, confirmed = 6, members = members)))
        assertEquals("명단이 없으면 체크인 수를 붙이지 않는다", "참여자 6 / 6", participantsTitle(meeting(MeetingStatus.IN_PROGRESS, confirmed = 6)))
        assertEquals("참여한 메이트 1 / 6", participantsTitle(meeting(MeetingStatus.ENDED, confirmed = 6, members = members)))
    }

    @Test
    fun 아바타는_앞의_세_명과_나머지_수를_보여_준다() {
        val members = listOf("가", "나", "다", "라", "마", "바").map { MeetingMember(it, "${it}길동") }
        val row = avatarRowOf(meeting(members = members))!!
        assertEquals(listOf("가", "나", "다"), row.initials)
        assertEquals(3, row.extra)
        assertEquals(0, avatarRowOf(meeting(members = members.take(2)))!!.extra)
        assertNull("명단이 없으면 아바타 줄도 없다", avatarRowOf(meeting()))
    }

    @Test
    fun 진행중_안내는_시작_시각을_알려_준다() {
        val m = meeting(MeetingStatus.IN_PROGRESS, start = millis(9, 8, 7))
        assertEquals("지금 진행 중이에요" to "오전 7:00 시작", statusNoteOf(m, millis(9, 8, 8), seoul))
    }

    @Test
    fun 종료_안내는_종료_시각과_평가_안내를_준다() {
        val m = meeting(MeetingStatus.ENDED, start = millis(9, 8, 5)) // 4시간 → 9:00 종료
        assertEquals("오늘 오전 9:00에 종료됐어요" to "함께한 메이트를 평가하면 매너온도가 올라가요", statusNoteOf(m, millis(9, 8, 10), seoul))
        assertNull(statusNoteOf(meeting(), millis(9, 8, 10), seoul))
    }

    // ---- 정보 카드 ----

    @Test
    fun 장소_줄은_이름과_구를_보여_준다() {
        val m = meeting().copy(place = MeetingPlace(name = "합정 스포애니", address = "서울 마포구 양화로 100"))
        assertEquals("합정 스포애니 · 마포구", placeLine(m))
        assertEquals("한강공원", placeLine(meeting().copy(place = MeetingPlace(name = "한강공원", address = "주소 없음"))))
    }

    @Test
    fun 헬스_기준_요약은_호스트_기록에서_범위를_계산한다() {
        val host = ExerciseRecord(benchPressKg = 80, pullUpCount = 8, armLevel = SkillLevel.INTERMEDIATE)
        val m = meeting().copy(criteria = MeetingCriteria.Health(setOf(BodyPart.ARM, BodyPart.CHEST, BodyPart.BACK), host))
        assertEquals("가슴 70–90kg · 등 6–10회 · 팔 중급", criteriaSummaryOf(m))
    }

    @Test
    fun 호스트가_기록을_숨기면_부위_이름만_보인다() {
        val m = meeting().copy(criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST, BodyPart.ARM), null))
        assertEquals("가슴 · 팔", criteriaSummaryOf(m))
    }

    @Test
    fun 헬스_범위는_0_아래로_내려가지_않는다() {
        val m = meeting().copy(criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST, BodyPart.BACK), ExerciseRecord(benchPressKg = 5, pullUpCount = 0)))
        assertEquals("가슴 0–15kg · 등 0–2회", criteriaSummaryOf(m))
    }

    @Test
    fun 러닝과_등산_기준_요약() {
        assertEquals("5km · 5'15\"–6'15\"", criteriaSummaryOf(meeting().copy(criteria = MeetingCriteria.Running(5f, 345))))
        assertEquals("5.5km · 5'30\"–6'30\"", criteriaSummaryOf(meeting().copy(criteria = MeetingCriteria.Running(5.5f, 360))))
        assertEquals("취미", criteriaSummaryOf(meeting().copy(criteria = MeetingCriteria.Hiking(HikingStyle.HOBBY))))
        assertEquals("전문", criteriaSummaryOf(meeting().copy(criteria = MeetingCriteria.Hiking(HikingStyle.PRO))))
    }

    @Test
    fun 판정이_없으면_판정_줄을_숨긴다() {
        assertNull(judgementViewOf(meeting()))
    }

    @Test
    fun 헬스_판정_문구() {
        fun view(j: CriteriaJudgement) = judgementViewOf(MockMeetings.recruiting.copy(criteriaJudgement = j))!!
        assertEquals(JudgementView(CriteriaMatch.Match, "내 기록 범위 안"), view(CriteriaJudgement(CriteriaMatchResult.MATCH)))
        assertEquals(JudgementView(CriteriaMatch.Mismatch, "내 기록과 맞지 않아요"), view(CriteriaJudgement(CriteriaMatchResult.MISMATCH)))
        assertEquals(
            JudgementView(CriteriaMatch.Partial, "가슴은 범위 밖이에요"),
            view(CriteriaJudgement(CriteriaMatchResult.PARTIAL, listOf(BodyPart.CHEST))),
        )
    }

    @Test
    fun 범위_밖_부위가_여럿이면_디자인_순서로_이어_쓰고_조사를_맞춘다() {
        val j = CriteriaJudgement(CriteriaMatchResult.PARTIAL, listOf(BodyPart.SHOULDER, BodyPart.CHEST))
        assertEquals("가슴, 어깨는 범위 밖이에요", judgementViewOf(MockMeetings.recruiting.copy(criteriaJudgement = j))!!.statusText)
        val legs = CriteriaJudgement(CriteriaMatchResult.PARTIAL, listOf(BodyPart.LEGS))
        assertEquals("하체는 범위 밖이에요", judgementViewOf(MockMeetings.recruiting.copy(criteriaJudgement = legs))!!.statusText)
        val none = CriteriaJudgement(CriteriaMatchResult.PARTIAL)
        assertEquals("일부만 맞아요", judgementViewOf(MockMeetings.recruiting.copy(criteriaJudgement = none))!!.statusText)
    }

    @Test
    fun 러닝과_등산_판정_문구() {
        val hiking = MockMeetings.inProgress // 등산
        assertEquals("내 성향과 맞음", judgementViewOf(hiking.copy(criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.MATCH)))!!.statusText)
        assertEquals("내 성향과 맞지 않아요", judgementViewOf(hiking.copy(criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.MISMATCH)))!!.statusText)
        val running = MockMeetings.pending
        assertEquals("내 페이스 범위 안", judgementViewOf(running.copy(criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.MATCH)))!!.statusText)
        assertEquals("내 페이스와 맞지 않아요", judgementViewOf(running.copy(criteriaJudgement = CriteriaJudgement(CriteriaMatchResult.MISMATCH)))!!.statusText)
    }

    @Test
    fun 받침에_따라_은_는을_고른다() {
        assertEquals("은", topicParticle("가슴"))
        assertEquals("은", topicParticle("등"))
        assertEquals("은", topicParticle("팔"))
        assertEquals("는", topicParticle("하체"))
        assertEquals("는", topicParticle("어깨"))
        assertEquals("은", topicParticle("abc"))
        assertEquals("은", topicParticle(""))
    }

    // ---- 하단 버튼 ----

    private val me = "u1"
    private fun action(m: Meeting, role: MeetingRole, now: Long) = bottomActionOf(m, role, me, now)

    @Test
    fun 호스트는_하단_버튼이_없다() {
        assertNull(action(meeting(), MeetingRole.HOST, millis(9, 8, 0)))
    }

    @Test
    fun 모집중에_신청하지_않았으면_신청하기와_마감까지_남은_시간_남은_자리를_보여_준다() {
        val a = action(meeting(), MeetingRole.NONE, millis(9, 8, 0))!!
        assertEquals(DetailAction.APPLY, a.action)
        assertEquals("참여 신청하기", a.title)
        assertEquals("마감까지 5시간 · 남은 자리 3", a.subtitle)
        assertTrue(a.enabled)
    }

    @Test
    fun 데드라인이_지난_모집중이면_남은_시간_없이_남은_자리만_보여_준다() {
        val a = action(meeting(), MeetingRole.NONE, millis(9, 8, 6))!!
        assertEquals("남은 자리 3", a.subtitle)
    }

    @Test
    fun 대기중이면_신청_취소하기_보조_버튼이다() {
        val a = action(meeting(), MeetingRole.PENDING, millis(9, 8, 0))!!
        assertEquals(DetailAction.CANCEL_APPLICATION, a.action)
        assertTrue(a.outlined)
        assertEquals("호스트가 확인하면 알림을 보내드릴게요", a.subtitle)
    }

    @Test
    fun 모집중에_확정된_사람은_참여_확정_안내만_보인다() {
        val a = action(meeting(), MeetingRole.CONFIRMED, millis(9, 8, 0))!!
        assertEquals(DetailAction.NONE, a.action)
        assertFalse(a.enabled)
        assertEquals("참여 확정", a.title)
    }

    @Test
    fun 마감이면_모임_마감_회색_버튼이다() {
        val a = action(meeting(MeetingStatus.CLOSED, confirmed = 6), MeetingRole.NONE, millis(9, 8, 0))!!
        assertEquals("모임 마감", a.title)
        assertFalse(a.enabled)
    }

    @Test
    fun 마감된_모임에서_확정된_사람은_시작_십분_전부터_체크인할_수_있다() {
        val m = meeting(MeetingStatus.CLOSED, start = millis(9, 8, 7), confirmed = 6)
        assertEquals("아직은 마감 안내", "모임 마감", action(m, MeetingRole.CONFIRMED, millis(9, 8, 6, 40))!!.title)
        val open = action(m, MeetingRole.CONFIRMED, millis(9, 8, 6, 50))!!
        assertEquals(DetailAction.CHECK_IN, open.action)
        assertTrue(open.enabled)
    }

    @Test
    fun 체크인은_시작_십분_전부터_시작_후_십분까지만_열린다() {
        val start = millis(9, 8, 7)
        val m = meeting(MeetingStatus.IN_PROGRESS, start = start, confirmed = 6)
        assertTrue(action(m, MeetingRole.CONFIRMED, start)!!.enabled)
        assertTrue("경계 포함", action(m, MeetingRole.CONFIRMED, start + 10 * minute)!!.enabled)
        val late = action(m, MeetingRole.CONFIRMED, start + 10 * minute + 1)!!
        assertFalse(late.enabled)
        assertEquals("체크인 시간이 지났어요", late.subtitle)
    }

    @Test
    fun 시작_십분_전_이전에는_체크인이_닫혀_있다() {
        val start = millis(9, 8, 7)
        val m = meeting(MeetingStatus.IN_PROGRESS, start = start, confirmed = 6)
        val early = action(m, MeetingRole.CONFIRMED, start - 10 * minute - 1)!!
        assertFalse(early.enabled)
        assertEquals("시작 10분 전부터 할 수 있어요", early.subtitle)
    }

    @Test
    fun 이미_체크인했으면_체크인_완료_회색_버튼이다() {
        val start = millis(9, 8, 7)
        val m = meeting(MeetingStatus.IN_PROGRESS, start = start, confirmed = 2, members = listOf(MeetingMember(me, "민준", true)))
        val a = action(m, MeetingRole.CONFIRMED, start + minute)!!
        assertEquals("체크인 완료", a.title)
        assertFalse(a.enabled)
    }

    @Test
    fun 진행중_모임에_확정되지_않은_사람은_체크인할_수_없다() {
        val a = action(meeting(MeetingStatus.IN_PROGRESS), MeetingRole.NONE, millis(9, 8, 7, 5))!!
        assertFalse(a.enabled)
        assertEquals(DetailAction.NONE, a.action)
    }

    @Test
    fun 종료된_모임에서_체크인한_사람은_메이트_평가하기를_누를_수_있다() {
        val members = listOf(MeetingMember(me, "민준", true), MeetingMember("u2", "서연", true), MeetingMember("u3", "태", false))
        val m = meeting(MeetingStatus.ENDED, confirmed = 3, capacity = 6, members = members)
        val a = action(m, MeetingRole.CONFIRMED, millis(9, 9, 0))!!
        assertEquals(DetailAction.REVIEW, a.action)
        assertEquals("3명 중 2명이 참여 완료", a.subtitle)
    }

    @Test
    fun 종료된_모임에서_체크인하지_않았거나_확정되지_않은_사람은_평가할_수_없다() {
        val members = listOf(MeetingMember(me, "민준", false))
        val notCheckedIn = action(meeting(MeetingStatus.ENDED, members = members), MeetingRole.CONFIRMED, millis(9, 9, 0))!!
        assertFalse(notCheckedIn.enabled)
        assertFalse(action(meeting(MeetingStatus.ENDED), MeetingRole.NONE, millis(9, 9, 0))!!.enabled)
    }

    @Test
    fun 취소된_모임은_취소된_모임_회색_버튼이다() {
        val a = action(meeting(MeetingStatus.CANCELED), MeetingRole.NONE, millis(9, 8, 0))!!
        assertEquals("취소된 모임", a.title)
        assertFalse(a.enabled)
    }

    // ---- 장소 시트 ----

    @Test
    fun 전체_주소는_도로명_주소_뒤에_상세주소를_붙인다() {
        val m = meeting().copy(place = MeetingPlace(address = "서울 마포구 양화로 100", detailAddress = "3층"))
        assertEquals("서울 마포구 양화로 100 3층", fullAddress(m))
        assertEquals("서울 마포구 양화로 100", fullAddress(meeting().copy(place = MeetingPlace(address = "서울 마포구 양화로 100"))))
        assertEquals("상세주소가 비어 있으면 붙이지 않는다", "서울 마포구 양화로 100", fullAddress(meeting().copy(place = MeetingPlace(address = "서울 마포구 양화로 100", detailAddress = " "))))
    }

    @Test
    fun 길찾기_링크는_이름과_좌표를_담고_공백을_인코딩한다() {
        val m = meeting().copy(place = MeetingPlace(name = "한강공원 망원지구", latitude = 37.5551, longitude = 126.8978))
        val url = routeUrl(m)
        assertTrue(url.startsWith("https://map.kakao.com/link/to/"))
        assertTrue(url.endsWith(",37.5551,126.8978"))
        assertFalse("공백이 그대로 남지 않는다", url.contains(' '))
        assertTrue(url.contains("%20"))
    }

    @Test
    fun 성별_조건이_안_맞으면_신청하기_대신_안내를_보여_준다() {
        val femaleOnly = meeting().copy(genderLimit = com.example.myapplication.data.model.GenderLimit.FEMALE_ONLY)
        val a = bottomActionOf(femaleOnly, MeetingRole.NONE, me, millis(9, 8, 0), com.example.myapplication.data.model.Gender.MALE)!!
        assertEquals("신청할 수 없어요", a.title)
        assertEquals("성별 조건이 맞지 않아요", a.subtitle)
        assertFalse(a.enabled)
        val ok = bottomActionOf(femaleOnly, MeetingRole.NONE, me, millis(9, 8, 0), com.example.myapplication.data.model.Gender.FEMALE)!!
        assertEquals(DetailAction.APPLY, ok.action)
        val unknown = bottomActionOf(femaleOnly, MeetingRole.NONE, me, millis(9, 8, 0), null)!!
        assertEquals("내 성별을 모르면 막지 않는다", DetailAction.APPLY, unknown.action)
    }
}
