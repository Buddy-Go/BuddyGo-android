package com.example.myapplication.ui.mate

import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.repository.FakeMateRepository
import com.example.myapplication.data.model.MateActivity
import com.example.myapplication.data.model.MateActivityKind
import com.example.myapplication.data.model.MateProfile
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.data.model.MateRequest
import com.example.myapplication.data.model.MateSummary
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.WorkedOutPerson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

// 메이트(11a~11c, 15a~15c) 표시 글자와 규칙 (service-rules 10장)
class MateStateTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 1788566400000L // 2026-09-05(토) 09:00 (서울)

    @Test
    fun 언제_표시() {
        assertEquals("오늘 오후 8:00", whenText(now, now + 11 * hour, seoul))
        assertEquals("내일 오전 6:30", whenText(now, now + day - 2 * hour - 30 * 60_000, seoul))
        assertEquals("화요일 오후 2:00", whenText(now, now + 3 * day + 5 * hour, seoul))
        assertEquals("9/20 오전 9:00", whenText(now, now + 15 * day, seoul))
    }

    @Test
    fun 메이트_줄_11a() {
        val live = MateSummary("a", "민준", MateActivity("x", MateActivityKind.EXERCISING_NOW, "아침 가슴 운동", "p", now))
        assertEquals(MateLine("지금 운동 중 · 아침 가슴 운동", isLive = true), live.line(now, seoul))
        val hosting = MateSummary("b", "지훈", MateActivity("y", MateActivityKind.HOSTING, "퇴근 후 등 운동", "p", now + 11 * hour))
        assertEquals(MateLine("퇴근 후 등 운동 · 오늘 오후 8:00", isLive = false), hosting.line(now, seoul))
        assertEquals("예정된 모임 없어요", MateSummary("c", "하늘", null).line(now, seoul).text)
    }

    @Test
    fun 배너와_이름_찾기_11a_11b() {
        val s = MateState(
            pendingRequestCount = 2,
            workedOut = listOf(
                WorkedOutPerson("a", "서연", now, MateRelation.NONE),
                WorkedOutPerson("b", "우진", now, MateRelation.REQUESTED),
            ),
        )
        assertEquals("메이트 신청 2명이 기다리고 있어요", s.bannerText)
        assertNull(s.copy(pendingRequestCount = 0).bannerText)
        assertEquals(listOf("b"), s.copy(searchQuery = "우").filteredWorkedOut.map { it.userId })
        assertEquals("'민' 이름의 사람이 없어요", s.copy(searchQuery = "민").workedOutEmptyText)
        assertEquals("아직 함께 운동한 사람이 없어요", MateState().workedOutEmptyText)
        assertEquals("마지막 운동 9/5", s.workedOut[0].lastText(seoul))
    }

    @Test
    fun 종목_칩_11b_2_11b_3() {
        val s = MateState(
            workedOut = listOf(
                WorkedOutPerson("a", "서연", now, MateRelation.NONE, setOf(Sport.HEALTH, Sport.RUNNING)),
                WorkedOutPerson("b", "우진", now, MateRelation.NONE, setOf(Sport.HEALTH)),
                WorkedOutPerson("c", "태윤", now, MateRelation.NONE, setOf(Sport.HIKING)),
            ),
        )
        assertEquals(3, s.sportCount(null))
        assertEquals(2, s.sportCount(Sport.HEALTH))
        assertEquals(1, s.sportCount(Sport.RUNNING))
        assertEquals(listOf("a", "b"), s.copy(sportFilter = Sport.HEALTH).filteredWorkedOut.map { it.userId })
        // 종목과 이름을 같이 걸면 둘 다 맞는 사람만
        assertEquals(listOf("b"), s.copy(sportFilter = Sport.HEALTH, searchQuery = "우").filteredWorkedOut.map { it.userId })
        // 11b-3: 그 종목을 함께한 사람이 없으면
        val none = s.copy(workedOut = s.workedOut.filter { Sport.RUNNING !in it.sports }, sportFilter = Sport.RUNNING)
        assertEquals("러닝을 함께한 사람이 아직 없어요", none.workedOutEmptyText)
        assertEquals(0, none.sportCount(Sport.RUNNING))
    }

    @Test
    fun 받은_신청_11c() {
        val r = MateRequest("q", "u", "지훈", 42.5f, 18)
        assertEquals("매너온도 42.5°C · 참여 18회", r.infoText())
        assertEquals("메이트 신청 1", MateRequestState(requests = listOf(r)).title)
        assertEquals("메이트 신청", MateRequestState().title)
    }

    private fun profile(relation: MateRelation, workedOut: Boolean) = MateProfile(
        userId = "u", nickname = "태윤", mannerTemperature = 40.2f, topTags = emptyList(), relation = relation,
        workedOutTogether = workedOut, activities = emptyList(), reviewCount = 6, recentReviews = emptyList(),
    )

    @Test
    fun 프로필_아래_버튼_15a_15b_15c() {
        assertEquals(ProfileButton.NONE, MateProfileState(profile(MateRelation.MATE, true)).bottomButton)
        assertEquals(ProfileButton.REQUEST, MateProfileState(profile(MateRelation.NONE, true)).bottomButton)
        assertEquals(ProfileButton.REQUESTED, MateProfileState(profile(MateRelation.REQUESTED, true)).bottomButton)
        // 함께 운동한 적 없으면 신청 버튼 숨김 (정의서 13장 '15b 수정 필요')
        assertEquals(ProfileButton.NONE, MateProfileState(profile(MateRelation.NONE, false)).bottomButton)
        assertTrue(MateProfileState(profile(MateRelation.MATE, true)).showMenu)
        assertFalse(MateProfileState(profile(MateRelation.NONE, true)).showMenu)
        assertEquals("후기 6", MateProfileState(profile(MateRelation.NONE, true)).reviewHeader)
    }

    @Test
    fun 가짜_저장소_신청_수락_끊기() = runBlocking {
        val repo = FakeMateRepository()
        // 함께 운동한 적 없는 사람에게는 신청 불가
        assertTrue(repo.sendRequest("mt6").isFailure)
        repo.sendRequest("mt3")
        assertEquals(MateRelation.REQUESTED, repo.getProfile("mt3").getOrThrow().relation)
        // 받은 신청 수락 → 메이트
        val request = repo.getRequests().getOrThrow().first { it.userId == "mt4" }
        repo.acceptRequest(request.requestId)
        assertEquals(MateRelation.MATE, repo.getProfile("mt4").getOrThrow().relation)
        assertTrue(repo.getMates().getOrThrow().any { it.userId == "mt4" })
        // 메이트 끊기
        repo.removeMate("mt4")
        assertFalse(repo.getMates().getOrThrow().any { it.userId == "mt4" })
        // 메이트 알림 끄기
        repo.setMateNotification(MockUsers.hostA.id, false)
        assertFalse(repo.getProfile(MockUsers.hostA.id).getOrThrow().notificationEnabled)
    }
}
