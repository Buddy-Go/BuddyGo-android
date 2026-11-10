package com.example.myapplication.data.api

import com.example.myapplication.data.local.InMemoryKeyValueStore
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.data.model.MateActivityKind
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.data.model.NotificationType
import com.example.myapplication.data.model.Review
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.data.repository.NoticeIndex
import com.example.myapplication.data.repository.RealMateRepository
import com.example.myapplication.data.repository.RealNoticeRepository
import com.example.myapplication.data.repository.RealNotificationRepository
import com.example.myapplication.data.repository.RealReviewRepository
import com.example.myapplication.data.repository.RealUserRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 공지·알림·메이트·후기·프로필을 가짜 서버로 확인한다. 응답 JSON은 실제 서버(BuddyGo-server)가 내려준 모양 그대로다.
class ServiceIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: TokenStore
    private lateinit var client: ApiClient
    private val routes = mutableMapOf<String, (RecordedRequest) -> MockResponse>()
    private val seen = mutableListOf<RecordedRequest>()

    @Before
    fun setUp() {
        NoticeIndex.clear()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                seen += request
                val key = "${request.method} ${request.path!!.substringBefore('?')}"
                return routes[key]?.invoke(request) ?: json("""{"code":"G006","message":"없음"}""", 404)
            }
        }
        server.start()
        tokens = TokenStore(InMemoryKeyValueStore()).also { it.save(TokenDto("a", "r"), keepLogin = true); it.setUserId("5") }
        client = ApiClient(server.url("/").toString(), tokens, onSessionExpired = {})
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(body: String, status: Int = 200) = MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)
    private fun ok(data: String) = json("""{"code":200,"message":"ok","data":$data}""")
    private val noData = json("""{"code":200,"message":"ok"}""")

    private val notice = """{"noticeId":1,"gatheringId":2,"authorId":2,"authorNickname":"호스트","title":"주차 안내","content":"지하 1층","createdAt":"2026-11-10T12:28:45.978897710Z","editedAt":null,"edited":false,"commentCount":2}"""
    private fun comment(id: Int, author: Int) = """{"commentId":$id,"noticeId":1,"authorId":$author,"authorNickname":"작성자$author","content":"댓글$id","createdAt":"2026-11-10T12:30:00.123456Z"}"""

    // ---- 공지·댓글 ----

    @Test
    fun 공지_목록은_서버가_준_댓글_수를_쓰고_공지_id로_모임을_기억한다() = runTest {
        routes["GET /api/gatherings/2/notices"] = { ok("""{"content":[$notice],"page":0,"size":50,"hasNext":false}""") }
        val repo = RealNoticeRepository(client.noticeApi)

        val n = repo.getNotices("2").getOrThrow().single()

        assertEquals("1", n.id)
        assertEquals("2", n.meetingId)
        assertEquals(2, n.commentCount)
        assertEquals("2", NoticeIndex.meetingOf("1"))
        assertEquals(1_794_313_725_978L, n.createdAtMillis)
        assertTrue(seen.none { it.path!!.contains("/comments") }) // 댓글 수를 서버가 주면 댓글을 따로 받지 않는다
    }

    @Test
    fun 댓글_수가_비어_있는_서버면_댓글을_받아_센다() = runTest {
        val old = notice.replace(""","commentCount":2""", "")
        routes["GET /api/gatherings/2/notices"] = { ok("""{"content":[$old],"page":0,"size":50,"hasNext":false}""") }
        routes["GET /api/gatherings/2/notices/1/comments"] = { ok("""{"content":[${comment(1, 5)},${comment(2, 2)},${comment(3, 5)}],"page":0,"size":100,"hasNext":false}""") }

        assertEquals(3, RealNoticeRepository(client.noticeApi).getNotices("2").getOrThrow().single().commentCount)
    }

    @Test
    fun 댓글의_호스트_여부는_서버가_준_값을_쓴다() = runTest {
        NoticeIndex.putNotice("1", "2")
        routes["GET /api/gatherings/2/notices/1/comments"] = { ok("""{"content":[{"commentId":1,"noticeId":1,"authorId":5,"authorNickname":"a","content":"x","createdAt":"2026-11-10T12:30:00Z","authorHost":false},{"commentId":2,"noticeId":1,"authorId":2,"authorNickname":"b","content":"y","createdAt":"2026-11-10T12:31:00Z","authorHost":true}],"page":0,"size":100,"hasNext":false}""") }

        val list = RealNoticeRepository(client.noticeApi).getComments("1").getOrThrow()

        assertEquals(listOf(false, true), list.map { it.isAuthorHost })
        assertTrue(seen.none { it.path == "/api/gatherings/2/notices/1" }) // 공지를 따로 받아 비교하지 않는다
    }

    @Test
    fun 모임을_모르는_공지는_실패한다() = runTest {
        assertTrue(RealNoticeRepository(client.noticeApi).getNotice("99").exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun 댓글은_호스트_표시를_붙이고_댓글_삭제는_공지를_따라간다() = runTest {
        NoticeIndex.putNotice("1", "2")
        routes["GET /api/gatherings/2/notices/1"] = { ok(notice) }
        routes["GET /api/gatherings/2/notices/1/comments"] = { ok("""{"content":[${comment(1, 5)},${comment(2, 2)}],"page":0,"size":100,"hasNext":false}""") }
        routes["DELETE /api/gatherings/2/notices/1/comments/1"] = { noData }
        val repo = RealNoticeRepository(client.noticeApi)

        val list = repo.getComments("1").getOrThrow()

        assertFalse(list[0].isAuthorHost)
        assertTrue(list[1].isAuthorHost)
        assertTrue(repo.deleteComment("1").isSuccess)
    }

    @Test
    fun 공지_작성과_수정과_삭제는_맞는_주소로_보낸다() = runTest {
        routes["POST /api/gatherings/2/notices"] = { json("""{"code":201,"message":"ok","data":$notice}""", 201) }
        routes["PATCH /api/gatherings/2/notices/1"] = { ok(notice) }
        routes["GET /api/gatherings/2/notices/1/comments"] = { ok("""{"content":[],"page":0,"size":100,"hasNext":false}""") }
        routes["DELETE /api/gatherings/2/notices/1"] = { noData }
        val repo = RealNoticeRepository(client.noticeApi)

        val created = repo.createNotice("2", " 주차 안내 ", " 지하 1층 ").getOrThrow()
        assertTrue(repo.updateNotice(created.id, "새 제목", "새 본문").isSuccess)
        assertTrue(repo.deleteNotice(created.id).isSuccess)
        assertTrue(seen.first { it.method == "POST" }.body.readUtf8().contains(""""title":"주차 안내""""))
    }

    // ---- 알림 ----

    @Test
    fun 알림은_앱_종류로_바꾸고_모르는_종류는_뺀다() = runTest {
        routes["GET /api/notifications"] = {
            ok("""{"content":[
              {"notificationId":6,"type":"NOTICE_CREATED","message":"새 공지","createdAt":"2026-11-10T12:28:46.161881Z","readAt":null,"read":false,"actorId":2,"gatheringId":2,"noticeId":1,"buddyRequestId":null},
              {"notificationId":5,"type":"APPLICATION_ACCEPTED","message":"확정","createdAt":"2026-11-10T12:28:45.748030Z","readAt":"2026-11-10T12:30:00Z","read":true,"actorId":2,"gatheringId":2,"noticeId":null,"buddyRequestId":null},
              {"notificationId":4,"type":"SOMETHING_NEW","message":"?","createdAt":"2026-11-10T12:00:00Z","read":false}],
              "page":0,"size":50,"hasNext":false,"unreadCount":2}""")
        }

        val list = RealNotificationRepository(client.notificationApi).getNotifications().getOrThrow()

        assertEquals(listOf("6", "5"), list.map { it.id })
        assertEquals(NotificationType.NOTICE_WRITTEN, list[0].type)
        assertEquals(NotificationType.JOIN_ACCEPTED, list[1].type)
        assertEquals("1", list[0].noticeId)
        assertEquals("2", list[0].meetingId)
        assertNull(list[0].subject)
        assertTrue(list[1].isRead)
        assertEquals("2", NoticeIndex.meetingOf("1")) // 알림에서 공지로 갈 수 있게 기억한다
    }

    @Test
    fun 알림_읽음과_모두_읽음() = runTest {
        routes["PATCH /api/notifications/5/read"] = { noData }
        routes["PATCH /api/notifications/read-all"] = { noData }
        val repo = RealNotificationRepository(client.notificationApi)

        assertTrue(repo.markAsRead("5").isSuccess)
        assertTrue(repo.markAllAsRead().isSuccess)
    }

    // ---- 메이트 ----

    private fun mateRepo() = RealMateRepository(client.buddyApi, client.userApi, client.meetingApi, client.reviewApi)

    @Test
    fun 메이트_목록은_다음_활동의_제목과_인원을_상세에서_채운다() = runTest {
        routes["GET /api/buddies"] = { ok("""[{"userId":2,"nickname":"호스트","mannerTemperature":36.5,"activity":{"gatheringId":2,"placeName":"스포애니 망원점","startAt":"2026-11-10T18:20:00Z","inProgress":false},"notifyGatheringCreated":true,"notifyGatheringApplied":true},{"userId":3,"nickname":"무활동","mannerTemperature":36.5,"activity":null,"notifyGatheringCreated":true,"notifyGatheringApplied":true}]""") }
        routes["GET /api/gatherings/2"] = { ok("""{"gatheringId":2,"title":"망원 하체 운동","status":"RECRUITING","startAt":"2026-11-10T18:20:00Z","endAt":"2026-11-10T19:50:00Z","capacity":3,"confirmedCount":2,"place":{"name":"스포애니 망원점"},"host":{"userId":2,"nickname":"호스트"}}""") }

        val mates = mateRepo().getMates().getOrThrow()

        val a = mates[0].nextActivity!!
        assertEquals("망원 하체 운동", a.title)
        assertEquals(2, a.confirmedCount)
        assertEquals(3, a.capacity)
        assertEquals(MateActivityKind.HOSTING, a.kind)
        assertNull(mates[1].nextActivity)
    }

    @Test
    fun 함께_운동한_사람의_관계를_앱_관계로_바꾼다() = runTest {
        routes["GET /api/buddies/workout-partners"] = { ok("""[{"userId":2,"nickname":"가","workoutCount":3,"lastWorkoutAt":"2026-11-09T10:00:00Z","relation":"NONE","requestId":null},{"userId":3,"nickname":"나","workoutCount":1,"lastWorkoutAt":"2026-11-08T10:00:00Z","relation":"BUDDY","requestId":null},{"userId":4,"nickname":"다","workoutCount":1,"lastWorkoutAt":"2026-11-07T10:00:00Z","relation":"REQUEST_SENT","requestId":9}]""") }

        val list = mateRepo().getWorkedOut().getOrThrow()

        assertEquals(listOf(MateRelation.NONE, MateRelation.MATE, MateRelation.REQUESTED), list.map { it.relation })
    }

    @Test
    fun 메이트_신청_수락_거절_끊기와_알림_설정() = runTest {
        routes["GET /api/buddy-requests/received"] = { ok("""[{"requestId":9,"userId":4,"nickname":"다","mannerTemperature":36.5,"participationCount":18,"requestedAt":"2026-11-10T10:00:00Z"}]""") }
        routes["POST /api/buddy-requests"] = { json("""{"code":201,"message":"ok","data":{"requestId":10,"status":"PENDING"}}""", 201) }
        routes["POST /api/buddy-requests/9/accept"] = { ok("""{"requestId":9,"status":"ACCEPTED"}""") }
        routes["POST /api/buddy-requests/9/reject"] = { ok("""{"requestId":9,"status":"REJECTED"}""") }
        routes["DELETE /api/buddies/3"] = { noData }
        routes["PATCH /api/buddies/3/notification"] = { ok("""{"userId":3,"gatheringCreated":false,"gatheringApplied":false}""") }
        val repo = mateRepo()

        assertEquals(18, repo.getRequests().getOrThrow().single().joinedCount)
        assertTrue(repo.sendRequest("4").isSuccess)
        assertTrue(repo.acceptRequest("9").isSuccess)
        assertTrue(repo.rejectRequest("9").isSuccess)
        assertTrue(repo.removeMate("3").isSuccess)
        assertTrue(repo.setMateNotification("3", false).isSuccess)
        assertTrue(seen.first { it.method == "POST" && it.path == "/api/buddy-requests" }.body.readUtf8().contains(""""receiverId":4"""))
        val patch = seen.first { it.method == "PATCH" }.body.readUtf8()
        assertTrue(patch.contains(""""gatheringCreated":false""") && patch.contains(""""gatheringApplied":false"""))
    }

    @Test
    fun 메이트가_아닌_사람의_프로필은_활동을_받지_않는다() = runTest {
        routes["GET /api/users/2"] = { ok("""{"userId":2,"nickname":"호스트","mannerTemperature":36.5,"me":false,"stats":{"reviewCount":3,"hostedCount":1,"participatedCount":2,"topTags":[{"code":"PUNCTUAL","label":"시간 약속을 잘 지켜요","polarity":"POSITIVE","count":2}]},"buddy":{"relation":"NONE","requestId":null,"canRequest":true}}""") }
        routes["GET /api/users/2/reviews"] = { ok("""{"content":[{"reviewId":1,"rating":"GOOD","comment":"좋았어요","tags":[{"code":"PUNCTUAL","label":"시간 약속을 잘 지켜요"}],"createdAt":"2026-11-09T10:00:00Z","evaluator":{"userId":5,"nickname":"게스트일"},"gathering":null},{"reviewId":2,"rating":"BAD","comment":null,"tags":[],"createdAt":"2026-11-09T11:00:00Z","evaluator":null,"gathering":null}],"nextCursor":null}""") }

        val p = mateRepo().getProfile("2").getOrThrow()

        assertEquals(MateRelation.NONE, p.relation)
        assertTrue(p.workedOutTogether)
        assertEquals(3, p.reviewCount)
        assertEquals("시간 약속을 잘 지켜요", p.topTags.single().label)
        assertTrue(p.activities.isEmpty())
        assertEquals(1, p.recentReviews.size) // 평가자가 숨겨진 아쉬움 평가는 뺀다
        assertEquals("좋았어요", p.recentReviews.single().comment)
        assertTrue(seen.none { it.path!!.contains("/activities") })
    }

    // ---- 후기 ----

    @Test
    fun 평가_화면은_대상과_메이트_관계와_모임_정보를_합친다() = runTest {
        routes["GET /api/gatherings/2/review-targets"] = { ok("""{"gatheringId":2,"deadlineAt":"2026-11-13T19:50:00Z","reviewable":true,"targets":[{"userId":2,"nickname":"호스트","host":true,"reviewed":false},{"userId":3,"nickname":"참가자","host":false,"reviewed":true}]}""") }
        routes["GET /api/gatherings/2"] = { ok("""{"gatheringId":2,"title":"망원 하체 운동","status":"FINISHED","startAt":"2026-11-10T18:20:00Z","endAt":"2026-11-10T19:50:00Z","capacity":3,"confirmedCount":2,"place":{"name":"스포애니 망원점"},"host":{"userId":2,"nickname":"호스트"}}""") }
        routes["GET /api/buddies/workout-partners"] = { ok("""[{"userId":2,"nickname":"호스트","workoutCount":1,"lastWorkoutAt":"2026-11-10T19:00:00Z","relation":"REQUEST_SENT","requestId":1}]""") }

        val s = RealReviewRepository(client.reviewApi, client.meetingApi, client.buddyApi).getReviewSession("2").getOrThrow()

        assertEquals("망원 하체 운동", s.meetingTitle)
        assertEquals("스포애니 망원점", s.placeName)
        assertEquals(1_794_599_400_000L, s.reviewDueAtMillis)
        assertTrue(s.targets[0].isMateRequested)
        assertFalse(s.targets[0].isReviewed)
        assertTrue(s.targets[1].isReviewed)
    }

    @Test
    fun 평가를_제출하면_태그_문구를_서버_코드로_바꿔_보낸다() = runTest {
        routes["POST /api/gatherings/2/reviews"] = { json("""{"code":201,"message":"ok","data":{"reviewId":1}}""", 201) }
        routes["GET /api/review-tags"] = { ok("""{"positive":[],"negative":[]}""") } // 모르는 문구는 서버 태그 목록에서 찾아보고 없으면 뺀다

        val r = RealReviewRepository(client.reviewApi, client.meetingApi, client.buddyApi).submitReview(
            Review(meetingId = "2", targetUserId = "3", verdict = ReviewVerdict.NORMAL, tags = listOf("시간 약속을 지켜요", "없는 태그"), comment = " 좋았어요 "),
        )

        assertTrue(r.isSuccess)
        val body = seen.first { it.method == "POST" }.body.readUtf8()
        assertTrue(body.contains(""""rating":"NORMAL"""") && body.contains(""""targetId":3""") && body.contains("PUNCTUAL"))
        assertFalse(body.contains("없는 태그"))
        assertTrue(body.contains(""""comment":"좋았어요""""))
    }

    @Test
    fun 이미_평가했으면_서버_에러_코드로_실패한다() = runTest {
        routes["POST /api/gatherings/2/reviews"] = { json("""{"code":"R002","message":"이미 평가를 제출했습니다."}""", 409) }

        val e = RealReviewRepository(client.reviewApi, client.meetingApi, client.buddyApi).submitReview(Review(meetingId = "2", targetUserId = "3")).exceptionOrNull() as ApiException

        assertEquals("R002", e.code)
    }

    // ---- 프로필 ----

    private fun userRepo() = RealUserRepository(client.authApi, client.userApi, tokens, client.reviewApi, FakeUserRepository())

    @Test
    fun 내_프로필_숫자와_알림_설정을_내_정보에서_읽는다() = runTest {
        routes["GET /api/users/me"] = { ok("""{"userId":5,"email":"g@ex.com","name":"게스트","nickname":"게스트일","gender":"MALE","provider":"LOCAL","mannerTemperature":36.5,"pushEnabled":false,"stats":{"reviewCount":4,"hostedCount":2,"participatedCount":1,"topTags":[{"code":"PUNCTUAL","label":"시간 약속을 잘 지켜요","polarity":"POSITIVE","count":3}]}}""") }
        val repo = userRepo()

        val stats = repo.getProfileStats("5").getOrThrow()

        assertEquals(4, stats.receivedReviewCount)
        assertEquals(2, stats.hostedMeetingCount)
        assertEquals(3, stats.topTags.single().count)
        assertEquals(false, repo.isNotificationEnabled().getOrThrow())
    }

    @Test
    fun 알림_설정을_바꾼다() = runTest {
        routes["PATCH /api/users/me/notification-settings"] = { ok("""{"pushEnabled":false}""") }

        assertTrue(userRepo().setNotificationEnabled(false).isSuccess)

        assertTrue(seen.single().body.readUtf8().contains(""""pushEnabled":false"""))
    }

    @Test
    fun 받은_후기는_최신순이고_아쉬움_평가는_익명이다() = runTest {
        routes["GET /api/users/me/reviews"] = { ok("""{"content":[{"reviewId":1,"rating":"GOOD","comment":"좋아요","tags":[{"code":"PUNCTUAL","label":"시간 약속을 잘 지켜요"}],"createdAt":"2026-11-08T10:00:00Z","evaluator":{"userId":2,"nickname":"호스트"}},{"reviewId":2,"rating":"BAD","comment":null,"tags":[],"createdAt":"2026-11-09T10:00:00Z","evaluator":null}],"nextCursor":null}""") }

        val list = userRepo().getReceivedReviews("5").getOrThrow()

        assertEquals(listOf("2", "1"), list.map { it.id })
        assertEquals("익명", list[0].reviewerNickname)
        assertEquals(ReviewVerdict.BAD, list[0].verdict)
        assertEquals("호스트", list[1].reviewerNickname)
        assertEquals(listOf("시간 약속을 잘 지켜요"), list[1].tags)
        assertEquals(1, seen.size) // 다음 쪽이 없으면 더 받지 않는다
    }

    @Test
    fun 다른_사용자_조회는_공개_정보만_담는다() = runTest {
        routes["GET /api/users/2"] = { ok("""{"userId":2,"nickname":"호스트","mannerTemperature":37.5,"me":false,"stats":{},"buddy":{"relation":"NONE","canRequest":false}}""") }

        val u = userRepo().getUser("2").getOrThrow()

        assertEquals("호스트", u.nickname)
        assertEquals(37.5f, u.mannerTemperature, 0.001f)
        assertEquals("", u.email)
    }

    // ---- 남은 API: 평가 대기, 평가 태그, 신청 취소, 푸시 토큰, 핀, 명단 ----

    @Test
    fun 평가_대기는_서버가_판정한_모임과_기한을_그대로_쓴다() = runTest {
        routes["GET /api/reviews/pending"] = { ok("""{"gatherings":[{"gatheringId":2,"title":"망원 하체 운동","startAt":"2026-11-10T18:20:00Z","endAt":"2026-11-10T19:50:00Z","deadlineAt":"2026-11-13T19:50:00Z","remainingCount":2}]}""") }

        val p = RealReviewRepository(client.reviewApi, client.meetingApi, client.buddyApi).getPendingReviews().getOrThrow().single()

        assertEquals("2", p.meetingId)
        assertEquals(2, p.remainingCount)
        assertEquals(1_794_599_400_000L, p.reviewDueAtMillis)
    }

    @Test
    fun 평가_태그를_받아오고_서버_문구로_온_태그도_코드로_바꾼다() = runTest {
        routes["GET /api/review-tags"] = { ok("""{"positive":[{"code":"WANT_AGAIN","label":"또 같이 운동하고 싶어요"}],"negative":[{"code":"ON_PHONE","label":"운동 중 휴대폰만 봐요"}]}""") }
        routes["POST /api/gatherings/2/reviews"] = { json("""{"code":201,"message":"ok","data":{"reviewId":1}}""", 201) }
        val repo = RealReviewRepository(client.reviewApi, client.meetingApi, client.buddyApi)

        val tags = repo.getReviewTags().getOrThrow()
        assertEquals(listOf(true, false), tags.map { it.isPositive })

        repo.submitReview(Review(meetingId = "2", targetUserId = "3", tags = listOf("또 같이 운동하고 싶어요", "시간 약속을 지켜요")))
        val body = seen.last { it.method == "POST" }.body.readUtf8()
        assertTrue(body.contains("WANT_AGAIN") && body.contains("PUNCTUAL"))
    }

    @Test
    fun 보낸_메이트_신청을_취소한다() = runTest {
        routes["DELETE /api/buddy-requests/9"] = { ok("""{"requestId":9,"status":"CANCELED"}""") }

        assertTrue(mateRepo().cancelRequest("9").isSuccess)
    }

    @Test
    fun 푸시_토큰은_등록하고_로그아웃하면_서버에서_지운다() = runTest {
        routes["PUT /api/users/me/fcm-token"] = { noData }
        routes["DELETE /api/users/me/fcm-token"] = { noData }
        routes["POST /api/auth/logout"] = { noData }
        val repo = userRepo()

        assertTrue(repo.registerPushToken("tok-1").isSuccess)
        assertEquals("tok-1", tokens.pushToken)
        assertTrue(repo.logout().isSuccess)

        val delete = seen.first { it.method == "DELETE" }
        assertTrue(delete.body.readUtf8().contains(""""fcmToken":"tok-1""""))
        assertEquals("Bearer a", delete.getHeader("Authorization")) // 로그인 상태에서 지운다
        assertNull(tokens.pushToken)
        assertFalse(tokens.hasSession)
    }

    @Test
    fun 지도_핀과_확정_명단을_받는다() = runTest {
        routes["GET /api/gatherings/pins"] = { ok("""{"pins":[{"parcelKey":"서울 마포구 망원동 411-3","gatheringCount":2,"latitude":37.5557,"longitude":126.9014}]}""") }
        routes["GET /api/gatherings/2/participants"] = { ok("""[{"userId":2,"nickname":"호스트","mannerTemperature":36.5,"host":true},{"userId":5,"nickname":"게스트일","mannerTemperature":36.5,"host":false}]""") }
        val repo = com.example.myapplication.data.repository.RealMeetingRepository(client.meetingApi, tokens, regions = { _, _ -> null })

        val pin = repo.getPins().getOrThrow().single()
        assertEquals(2, pin.meetingCount)
        assertTrue(seen.first().path!!.contains("swLat=33.0")) // 영역이 없으면 전국 범위
        assertEquals(listOf("2", "5"), repo.getParticipants("2").getOrThrow().map { it.userId })
    }
}
