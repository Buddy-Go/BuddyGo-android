package com.example.myapplication.data.api

import com.example.myapplication.data.local.InMemoryKeyValueStore
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.SignUpDraft
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.RealMateRepository
import com.example.myapplication.data.repository.RealMeetingRepository
import com.example.myapplication.data.repository.RealNoticeRepository
import com.example.myapplication.data.repository.RealNotificationRepository
import com.example.myapplication.data.repository.RealReviewRepository
import com.example.myapplication.data.repository.RealUserRepository
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.ExploreViewModel
import com.example.myapplication.data.repository.FakeNotificationRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

// 실제 서버에 대고 앱의 Real 저장소들을 한 번에 돌려 보는 점검 테스트.
// 기본으로는 건너뛴다. 환경변수 LIVE_SERVER_URL(예: http://localhost:8080/)이 있을 때만 돈다.
//   LIVE_SERVER_URL=http://localhost:8080/ ./gradlew :app:testDebugUnitTest --tests '*LiveServerTest*'
// 가입·모임 개설 같은 데이터를 서버에 만들므로 내 PC의 로컬 서버에서 돌린다. 배포 서버에는 돌리지 않는다.
class LiveServerTest {
    private val url: String? = System.getenv("LIVE_SERVER_URL")

    private class Actor(url: String) {
        val tokens = TokenStore(InMemoryKeyValueStore())
        val client = ApiClient(url, tokens, onSessionExpired = {})
        val users = RealUserRepository(client.authApi, client.userApi, tokens, client.reviewApi, FakeUserRepository())
        val meetings = RealMeetingRepository(client.meetingApi, tokens, regions = { _, _ -> "1144012200" })
        val notices = RealNoticeRepository(client.noticeApi)
        val notifications = RealNotificationRepository(client.notificationApi)
        val mates = RealMateRepository(client.buddyApi, client.userApi, client.meetingApi, client.reviewApi)
        val reviews = RealReviewRepository(client.reviewApi, client.meetingApi, client.buddyApi)
        var id = ""
    }

    private fun <T> Result<T>.ok(what: String): T = getOrElse { throw AssertionError("$what 실패: $it", it) }

    private fun signUp(url: String, tag: String, name: String): Actor {
        val a = Actor(url)
        val user = runBlocking {
            a.users.signUp(
                SignUpDraft(
                    name = name, nickname = "${name.take(4)}$tag", email = "live$tag$name@example.com", password = "Passw0rd!x",
                    gender = Gender.MALE, record = ExerciseRecord(benchPressKg = 80, pullUpCount = 8, armLevel = SkillLevel.INTERMEDIATE),
                ),
            ).ok("가입($name)")
        }
        a.id = user.id
        return a
    }

    @Test
    fun 실제_서버에서_모든_흐름이_이어진다() = runBlocking {
        assumeTrue("LIVE_SERVER_URL이 없어 건너뜀", url != null)
        val tag = (System.currentTimeMillis() % 100000).toString()
        val host = signUp(url!!, tag, "host")
        val p1 = signUp(url, tag, "pone")
        val p2 = signUp(url, tag, "ptwo")

        // 인증·프로필
        assertEquals(80, host.users.getCurrentUser().ok("내 정보").record.benchPressKg) // 가입 때 보낸 기록이 서버에 저장됨
        assertTrue(host.users.isEmailAvailable("nobody$tag@example.com").ok("이메일 확인"))
        assertFalse(host.users.isNicknameAvailable("host$tag").ok("닉네임 확인"))
        assertTrue(host.users.updateRecord(ExerciseRecord(benchPressKg = 90, armLevel = SkillLevel.ADVANCED)).isSuccess)
        assertEquals(90, host.users.getCurrentUser().ok("기록 수정 확인").record.benchPressKg)
        assertTrue(host.users.setNotificationEnabled(false).isSuccess)
        assertEquals(false, host.users.isNotificationEnabled().ok("알림 설정"))
        assertTrue(host.users.setNotificationEnabled(true).isSuccess)
        assertTrue(host.users.registerPushToken("live-token-$tag").isSuccess)
        assertEquals("host$tag", p1.users.getUser(host.id).ok("다른 사용자").nickname)

        // 모임 개설·조회·수정
        val start = (System.currentTimeMillis() / 600_000) * 600_000 + 3 * 3_600_000L // 지금부터 3시간 뒤 정각 10분 단위
        val draft = Meeting(
            title = "라이브 점검 $tag", description = "설명", criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST, BodyPart.BACK), ExerciseRecord(benchPressKg = 90)),
            startAtMillis = start, durationMinutes = 60, capacity = 3, genderLimit = GenderLimit.ANYONE,
            place = MeetingPlace("점검 헬스장", "서울 마포구 양화로 45", "서울 마포구 서교동 464-1", "3층", 37.5496, 126.9139),
        )
        val mid = host.meetings.createMeeting(draft).ok("개설")
        val listed = p1.meetings.getMeetings(null).ok("목록").first { it.id == mid }
        assertEquals("라이브 점검 $tag", listed.title)
        assertEquals(setOf(BodyPart.CHEST, BodyPart.BACK), (listed.criteria as MeetingCriteria.Health).parts)
        assertEquals(90, (listed.criteria as MeetingCriteria.Health).hostRecord?.benchPressKg)
        assertTrue(host.meetings.updateMeeting(mid, listed.copy(title = "수정된 제목 $tag", description = "새 설명")).isSuccess)
        assertEquals("수정된 제목 $tag", p1.meetings.getMeeting(mid).ok("수정 확인").title)
        assertTrue(p1.meetings.setFavorite(mid, true).isSuccess)
        assertTrue(p1.meetings.getMeeting(mid).ok("찜 확인").isFavorite)
        assertTrue(p1.meetings.setFavorite(mid, false).isSuccess)
        assertTrue(p1.meetings.getPins(null).ok("핀").isNotEmpty())

        // 신청·취소·수락·거절·나가기
        assertTrue(p1.meetings.apply(mid, "참여할게요").isSuccess)
        assertEquals(MeetingRole.PENDING, p1.meetings.getMeeting(mid).ok("신청 후").roleOf(p1.id))
        assertTrue(p1.meetings.cancelApplication(mid).isSuccess)
        assertTrue(p1.meetings.apply(mid, null).isSuccess)
        assertTrue(p2.meetings.apply(mid, "나도").isSuccess)
        val requests = host.meetings.getJoinRequests(mid).ok("신청자 목록")
        assertEquals(2, requests.size)
        assertTrue(host.meetings.getMeeting(mid).ok("호스트 상세").pendingIds.size == 2)
        assertTrue(host.meetings.reject(mid, requests.first { it.applicantId == p2.id }.id).isSuccess)
        assertTrue(p2.meetings.apply(mid, "다시").isSuccess) // 거절돼도 모집중이면 다시 신청 가능
        assertTrue(host.meetings.accept(mid, host.meetings.getJoinRequests(mid).ok("재조회").first { it.applicantId == p1.id }.id).isSuccess)
        assertEquals(MeetingRole.CONFIRMED, p1.meetings.getMeeting(mid).ok("수락 후").roleOf(p1.id))
        assertTrue(p1.meetings.leave(mid).isSuccess)
        assertEquals(MeetingRole.NONE, p1.meetings.getMeeting(mid).ok("나간 후").roleOf(p1.id))
        assertTrue(p1.meetings.apply(mid, null).isSuccess)
        host.meetings.getJoinRequests(mid).ok("마지막 신청자").forEach { assertTrue(host.meetings.accept(mid, it.id).isSuccess) } // 정원이 차서 마감
        val closed = p1.meetings.getMeeting(mid).ok("마감 상세")
        assertEquals(MeetingStatus.CLOSED, closed.status)
        assertEquals(3, p1.meetings.getParticipants(mid).ok("확정 명단").size)

        // 공지·댓글
        val notice = host.notices.createNotice(mid, "주차 안내", "지하 1층").ok("공지 작성")
        assertEquals(1, p1.notices.getNotices(mid).ok("공지 목록").size)
        val c1 = p1.notices.addComment(notice.id, "감사합니다").ok("댓글")
        assertTrue(host.notices.addComment(notice.id, "네").ok("호스트 댓글").isAuthorHost)
        val comments = p1.notices.getComments(notice.id).ok("댓글 목록")
        assertEquals(listOf(false, true), comments.map { it.isAuthorHost })
        assertEquals(2, host.notices.getNotice(notice.id).ok("댓글 수").commentCount)
        assertTrue(host.notices.updateNotice(notice.id, "주차 안내(수정)", "지하 2층").ok("공지 수정").isEdited)
        assertTrue(p1.notices.deleteComment(c1.id).isSuccess)

        // 알림
        val notis = p1.notifications.getNotifications().ok("알림")
        assertTrue(notis.any { it.noticeId == notice.id })
        assertTrue(p1.notifications.markAsRead(notis.first().id).isSuccess)
        assertTrue(p1.notifications.markAllAsRead().isSuccess)
        assertTrue(p1.notifications.getNotifications().ok("읽음 후").all { it.isRead })

        // 체크인 (서버가 거리를 재서 판정). 시작 10분 전부터 열려서 지금은 열리지 않는다.
        val early = p1.meetings.checkIn(mid, 37.5496, 126.9139)
        assertTrue("체크인 시간이 아니면 거절돼야 해요", early.isFailure)

        // 모임 취소(호스트)와 삭제
        assertTrue(host.meetings.cancelMeeting(mid, "점검 끝").isSuccess)
        assertEquals(MeetingStatus.CANCELED, host.meetings.getMeeting(mid).ok("취소 후").status)
        val mid2 = host.meetings.createMeeting(draft.copy(title = "삭제용 $tag")).ok("두 번째 개설")
        assertTrue(host.meetings.deleteMeeting(mid2).isSuccess)
        assertTrue(host.meetings.getMeeting(mid2).isFailure)

        // 내 활동 (취소된 모임 포함)
        assertTrue(host.meetings.getMyMeetings().ok("내 활동").any { it.id == mid })

        // 메이트: 함께 운동한 적이 없어 신청은 서버가 거절한다. 프로필과 후기 조회는 된다.
        assertTrue(p1.mates.sendRequest(host.id).isFailure)
        assertTrue(p1.mates.getMates().ok("메이트 목록").isEmpty())
        assertTrue(p1.mates.getWorkedOut().ok("함께 운동한 사람").isEmpty())
        assertTrue(p1.mates.getRequests().ok("받은 신청").isEmpty())
        assertNotNull(p1.mates.getProfile(host.id).ok("프로필"))
        assertEquals(0, p1.users.getProfileStats(host.id).ok("통계").receivedReviewCount)
        assertTrue(p1.users.getReceivedReviews(host.id).ok("후기").isEmpty())
        assertTrue(p1.reviews.getPendingReviews().ok("평가 대기").isEmpty())
        assertTrue(p1.reviews.getReviewTags().ok("태그").size >= 10)

        // 로그아웃 (푸시 토큰 삭제 포함) 뒤에는 보호된 API가 거절된다
        assertTrue(host.users.logout().isSuccess)
        assertTrue(host.users.getCurrentUser().exceptionOrNull() is ApiException)
    }

    // 같은 카카오 장소 id로 두 번 개설하면 서버가 장소 행을 하나로 합치는지(id가 null이면 합치지 못한다는 것까지) 확인한다.
    @Test
    fun 같은_카카오_장소_id로_개설하면_장소_행이_하나로_합쳐진다() = runBlocking {
        assumeTrue("LIVE_SERVER_URL이 없어 건너뜀", url != null)
        val tag = (System.currentTimeMillis() % 100000).toString()
        val host = signUp(url!!, tag, "dedu")
        val start = (System.currentTimeMillis() / 600_000) * 600_000 + 3 * 3_600_000L
        fun draft(title: String, kakaoId: String?) = Meeting(
            title = title, description = "", criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST), null),
            startAtMillis = start, durationMinutes = 60, capacity = 3, genderLimit = GenderLimit.ANYONE,
            place = MeetingPlace("중복 점검 헬스장 $tag", "서울 마포구 양화로 45", "서울 마포구 서교동 464-1", "3층", 37.5496, 126.9139, kakaoId),
        )
        suspend fun placeIdOf(id: String) = host.client.meetingApi.detail(id.toLong()).requireData().place

        val kakaoId = "live$tag"
        val a = placeIdOf(host.meetings.createMeeting(draft("중복 점검 A $tag", kakaoId)).ok("개설 A"))
        val b = placeIdOf(host.meetings.createMeeting(draft("중복 점검 B $tag", kakaoId)).ok("개설 B"))
        println("LIVE kakaoPlaceId 보냄: placeId A=${a.placeId} B=${b.placeId} kakao=${a.kakaoPlaceId}")
        assertEquals(kakaoId, a.kakaoPlaceId)
        assertEquals("같은 kakaoPlaceId면 장소 행이 하나여야 한다", a.placeId, b.placeId)

        val c = placeIdOf(host.meetings.createMeeting(draft("중복 점검 C $tag", null)).ok("개설 C"))
        val d = placeIdOf(host.meetings.createMeeting(draft("중복 점검 D $tag", null)).ok("개설 D"))
        println("LIVE kakaoPlaceId 안 보냄: placeId C=${c.placeId} D=${d.placeId}")
    }

    // 지도 화면 ViewModel을 실제 저장소·서버에 붙여, 지도를 보던 중 모임이 개설된 뒤 새로고침하면 목록에 들어오는지 본다.
    @Test
    fun 지도를_보던_중_개설된_모임이_새로고침하면_목록에_들어온다() {
        assumeTrue("LIVE_SERVER_URL이 없어 건너뜀", url != null)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val tag = (System.currentTimeMillis() % 100000).toString()
        val host = signUp(url!!, tag, "pins")
        val vm = ExploreViewModel(host.meetings, FakeNotificationRepository(), FakeUserRepository(), { emptyList() })
        try {
            fun ExploreViewModel.titles() = ((state.value.meetings as? UiState.Success)?.data ?: emptyList()).map { it.title }
            fun waitUntil(what: String, cond: () -> Boolean) {
                val end = System.currentTimeMillis() + 15_000
                while (!cond()) { check(System.currentTimeMillis() < end) { "$what (15초 안에 안 됨)" }; Thread.sleep(50) }
            }
            waitUntil("첫 목록") { state(vm) is UiState.Success }
            val title = "핀 점검 $tag"
            val start = (System.currentTimeMillis() / 600_000) * 600_000 + 3 * 3_600_000L
            runBlocking {
                host.meetings.createMeeting(
                    Meeting(
                        title = title, description = "", criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST), null),
                        startAtMillis = start, durationMinutes = 60, capacity = 3, genderLimit = GenderLimit.ANYONE,
                        place = MeetingPlace("핀 점검 헬스장 $tag", "서울 마포구 양화로 45", "서울 마포구 서교동 464-1", "3층", 37.5496, 126.9139, "pin$tag"),
                    ),
                ).ok("개설")
            }
            println("LIVE 개설 직후, 새로고침 전 목록에 있음=${title in vm.titles()}")
            vm.refreshMeetings()
            waitUntil("새로고침 후 새 모임") { title in vm.titles() }
            println("LIVE 새로고침 후 목록에 있음=${title in vm.titles()} / 전체 ${vm.titles().size}건, 핀 ${vm.state.value.pins.size}개")
        } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private fun state(vm: ExploreViewModel) = vm.state.value.meetings
}
