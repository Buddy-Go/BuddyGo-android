package com.example.myapplication.data.api

import com.example.myapplication.data.local.InMemoryKeyValueStore
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.CheckInOutOfRangeException
import com.example.myapplication.data.model.CriteriaMatchResult
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.MapBounds
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.MeetingRole
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.repository.RealMeetingRepository
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 가짜 서버로 모임 연동을 확인한다. 응답 JSON은 실제 서버(BuddyGo-server)가 내려준 모양 그대로다.
class MeetingIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: TokenStore
    private lateinit var repo: RealMeetingRepository
    private val routes = mutableMapOf<String, (RecordedRequest) -> MockResponse>()
    private val seen = mutableListOf<RecordedRequest>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                seen += request
                val key = "${request.method} ${request.path!!.substringBefore('?')}"
                return routes[key]?.invoke(request) ?: json("""{"code":"G006","message":"없음"}""", 404)
            }
        }
        server.start()
        tokens = TokenStore(InMemoryKeyValueStore()).also {
            it.save(TokenDto("a", "r"), keepLogin = true)
            it.setUserId("7")
        }
        val client = ApiClient(server.url("/").toString(), tokens, onSessionExpired = {})
        repo = RealMeetingRepository(client.meetingApi, tokens, regions = { _, _ -> "1144012200" })
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(body: String, status: Int = 200) =
        MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)

    private fun ok(data: String) = json("""{"code":200,"message":"ok","data":$data}""")

    private fun detail(role: String = "NONE", status: String = "RECRUITING", members: String = "null", match: String = """{"result":null,"mismatchedParts":[],"partsWithoutMyRecord":["CHEST"]}""") = """
        {"gatheringId":1,"sport":"GYM","title":"합정 가슴 운동","description":"같이해요","status":"$status",
        "startAt":"2026-11-10T12:30:00Z","endAt":"2026-11-10T13:30:00Z","durationMinutes":60,"deadlineAt":"2026-11-10T11:00:00Z",
        "capacity":3,"confirmedCount":1,"pendingCount":${if (role == "HOST") 2 else "null"},"genderRule":"FEMALE_ONLY",
        "place":{"placeId":1,"kakaoPlaceId":null,"name":"스포애니 합정점","roadAddress":"서울 마포구 양화로 45","jibunAddress":"서울 마포구 서교동 464-1","detailAddress":"3층","regionCode":"1144012200","latitude":37.5496,"longitude":126.9139},
        "host":{"userId":2,"nickname":"호스트","mannerTemperature":36.5,"reviewCount":4},
        "gym":{"parts":[
          {"bodyPart":"CHEST","recordShown":true,"minValue":70,"maxValue":90,"armLevel":null,"myRecordValue":null,"myArmLevel":null,"comparison":"NO_MY_RECORD"},
          {"bodyPart":"BACK","recordShown":true,"minValue":6,"maxValue":10,"armLevel":null,"myRecordValue":null,"myArmLevel":null,"comparison":"NO_MY_RECORD"},
          {"bodyPart":"ARM","recordShown":true,"minValue":null,"maxValue":null,"armLevel":"INTERMEDIATE","myRecordValue":null,"myArmLevel":null,"comparison":"NO_MY_RECORD"}],
          "match":$match},
        "viewer":{"role":"$role","applicationId":null,"liked":true},"members":$members,"cancellation":null}
    """.trimIndent()

    private val card = """{"gatheringId":1,"title":"합정 가슴 운동","startAt":"2026-11-10T12:30:00Z","endAt":"2026-11-10T13:30:00Z","distanceMeters":null,
        "place":{"name":"스포애니 합정점","roadAddress":"서울 마포구 양화로 45","jibunAddress":"서울 마포구 서교동 464-1","detailAddress":"3층","latitude":37.5496,"longitude":126.9139},
        "host":{"userId":2,"nickname":"호스트","mannerTemperature":36.5},"confirmedCount":1,"capacity":3,"liked":true}"""

    @Test
    fun 영역이_없으면_전국_범위로_검색하고_상세로_기준을_채운다() = runTest {
        routes["GET /api/gatherings"] = { ok("""{"content":[$card],"nextCursor":null}""") }
        routes["GET /api/gatherings/1"] = { ok(detail()) }

        val list = repo.getMeetings(null).getOrThrow()

        val search = seen.first { it.path!!.startsWith("/api/gatherings?") }
        assertTrue(search.path!!.contains("swLat=33.0") && search.path!!.contains("neLng=132.0"))
        val m = list.single()
        assertEquals("1", m.id)
        assertEquals(setOf(BodyPart.CHEST, BodyPart.BACK, BodyPart.ARM), (m.criteria as MeetingCriteria.Health).parts)
        assertEquals(GenderLimit.FEMALE_ONLY, m.genderLimit)
        assertEquals(1_794_313_800_000L, m.startAtMillis)
        assertTrue(m.isFavorite)
    }

    @Test
    fun 상세를_못_받으면_카드_정보로_보여_준다() = runTest {
        routes["GET /api/gatherings"] = { ok("""{"content":[$card],"nextCursor":null}""") }

        val m = repo.getMeetings(MapBounds(37.0, 126.0, 38.0, 127.0)).getOrThrow().single()

        assertEquals("합정 가슴 운동", m.title)
        assertEquals(MeetingStatus.RECRUITING, m.status)
        assertEquals(60, m.durationMinutes)
        assertEquals(1, m.confirmedCount)
    }

    @Test
    fun 다음_커서가_있으면_이어서_받는다() = runTest {
        var calls = 0
        routes["GET /api/gatherings"] = { ok(if (calls++ == 0) """{"content":[$card],"nextCursor":"abc"}""" else """{"content":[],"nextCursor":null}""") }

        repo.getMeetings(null)

        assertEquals(2, seen.count { it.path!!.startsWith("/api/gatherings?") })
        assertTrue(seen.last { it.path!!.startsWith("/api/gatherings?") }.path!!.contains("cursor=abc"))
    }

    @Test
    fun 호스트_기록은_범위에서_값으로_되돌린다() = runTest {
        routes["GET /api/gatherings/1"] = { ok(detail()) }

        val m = repo.getMeeting("1").getOrThrow()

        val record = (m.criteria as MeetingCriteria.Health).hostRecord
        assertEquals(80, record?.benchPressKg) // 70~90 → 80
        assertEquals(8, record?.pullUpCount) // 6~10 → 8
        assertEquals(SkillLevel.INTERMEDIATE, record?.armLevel)
        assertEquals("호스트", m.hostNickname)
        assertEquals(4, m.hostReviewCount)
        assertEquals("3층", m.place.detailAddress)
        assertEquals("서울 마포구 양화로 45", m.place.address)
    }

    @Test
    fun 판정이_없으면_null이고_있으면_일치_결과와_범위_밖_부위를_담는다() = runTest {
        routes["GET /api/gatherings/1"] = { ok(detail()) }
        assertNull(repo.getMeeting("1").getOrThrow().criteriaJudgement)

        routes["GET /api/gatherings/1"] = { ok(detail(match = """{"result":"PARTIAL","mismatchedParts":["BACK"],"partsWithoutMyRecord":[]}""")) }
        val j = repo.getMeeting("1").getOrThrow().criteriaJudgement
        assertEquals(CriteriaMatchResult.PARTIAL, j?.result)
        assertEquals(listOf(BodyPart.BACK), j?.outOfRangeParts)
    }

    @Test
    fun 내_역할은_viewer로_정한다() = runTest {
        routes["GET /api/gatherings/1"] = { ok(detail(role = "APPLICANT")) }
        assertEquals(MeetingRole.PENDING, repo.getMeeting("1").getOrThrow().roleOf("7"))

        routes["GET /api/gatherings/1"] = { ok(detail(role = "PARTICIPANT")) }
        assertEquals(MeetingRole.CONFIRMED, repo.getMeeting("1").getOrThrow().roleOf("7"))

        routes["GET /api/gatherings/1"] = { ok(detail(role = "NONE")) }
        assertEquals(MeetingRole.NONE, repo.getMeeting("1").getOrThrow().roleOf("7"))
    }

    @Test
    fun 호스트_상세는_신청자_id와_체크인_현황까지_채운다() = runTest {
        val members = """[{"userId":2,"nickname":"호스트","mannerTemperature":36.5,"host":true},{"userId":9,"nickname":"참가자","mannerTemperature":36.5,"host":false}]"""
        routes["GET /api/gatherings/1"] = { ok(detail(role = "HOST", status = "IN_PROGRESS", members = members)) }
        routes["GET /api/gatherings/1/applications"] = { ok("""{"confirmed":[],"pending":[{"applicationId":5,"userId":11,"nickname":"신청자","mannerTemperature":36.5,"message":"안녕","appliedAt":"2026-11-10T08:00:00Z","gymRecords":[]},{"applicationId":6,"userId":12,"nickname":"신청자2","mannerTemperature":36.5,"message":null,"appliedAt":"2026-11-10T08:01:00Z","gymRecords":[]}]}""") }
        routes["GET /api/gatherings/1/check-ins"] = { ok("""{"openAt":"2026-11-10T12:20:00Z","radiusMeters":50,"me":{"attendance":"ON_TIME","checkedInAt":"2026-11-10T12:25:00Z","distanceMeters":10},"members":[{"userId":2,"nickname":"호스트","host":true,"attendance":"ON_TIME","checkedInAt":"2026-11-10T12:25:00Z"},{"userId":9,"nickname":"참가자","host":false,"attendance":null,"checkedInAt":null}]}""") }

        val m = repo.getMeeting("1").getOrThrow()

        assertEquals(listOf("11", "12"), m.pendingIds)
        assertEquals(listOf("2", "9"), m.confirmedIds)
        assertEquals(1, m.checkedInCount)
        assertEquals(1_794_313_500_000L, m.members.first { it.userId == "2" }.checkedInAtMillis)
        assertFalse(m.members.first { it.userId == "9" }.checkedIn)
        assertEquals(MeetingStatus.IN_PROGRESS, m.status)
    }

    @Test
    fun 찜과_신청과_나가기는_맞는_주소로_보낸다() = runTest {
        routes["PUT /api/gatherings/1/like"] = { ok("null").setBody("""{"code":200,"message":"ok"}""") }
        routes["DELETE /api/gatherings/1/like"] = { json("""{"code":200,"message":"ok"}""") }
        routes["POST /api/gatherings/1/applications"] = { json("""{"code":201,"message":"ok","data":{"applicationId":2,"status":"PENDING","gatheringStatus":"RECRUITING"}}""", 201) }
        routes["DELETE /api/gatherings/1/applications/me"] = { json("""{"code":200,"message":"ok"}""") }
        routes["POST /api/gatherings/1/leave"] = { json("""{"code":200,"message":"ok","data":{"applicationId":2,"status":"LEFT","gatheringStatus":"RECRUITING"}}""") }

        assertTrue(repo.setFavorite("1", true).isSuccess)
        assertTrue(repo.setFavorite("1", false).isSuccess)
        assertTrue(repo.apply("1", "  참여할게요  ").isSuccess)
        assertTrue(repo.cancelApplication("1").isSuccess)
        assertTrue(repo.leave("1").isSuccess)
        assertTrue(seen.first { it.method == "POST" && it.path!!.endsWith("/applications") }.body.readUtf8().contains(""""message":"참여할게요""""))
    }

    @Test
    fun 한마디가_비어_있으면_보내지_않는다() = runTest {
        routes["POST /api/gatherings/1/applications"] = { json("""{"code":201,"message":"ok","data":{"applicationId":2,"status":"PENDING","gatheringStatus":"RECRUITING"}}""", 201) }

        repo.apply("1", "   ")

        assertFalse(seen.single().body.readUtf8().contains("message"))
    }

    @Test
    fun 정원이_차서_신청이_거절되면_서버_에러_코드로_실패한다() = runTest {
        routes["POST /api/gatherings/1/applications"] = { json("""{"code":"AP003","message":"정원이 가득 찼습니다."}""", 409) }

        val e = repo.apply("1", null).exceptionOrNull() as ApiException

        assertEquals("AP003", e.code)
    }

    @Test
    fun 체크인_범위_밖이면_서버가_잰_거리를_담은_예외로_실패한다() = runTest {
        routes["POST /api/gatherings/1/check-in"] = { json("""{"code":"C003","message":"체크인 가능 범위를 벗어났습니다.","distanceMeters":132,"radiusMeters":50}""", 422) }

        val e = repo.checkIn("1", 37.0, 127.0).exceptionOrNull()

        assertEquals(132, (e as CheckInOutOfRangeException).distanceMeters)
    }

    @Test
    fun 체크인에_성공하면_체크인_시각을_돌려준다() = runTest {
        routes["POST /api/gatherings/1/check-in"] = { ok("""{"attendance":"ON_TIME","checkedInAt":"2026-11-10T12:25:00Z","distanceMeters":12}""") }

        assertEquals(1_794_313_500_000L, repo.checkIn("1", 37.5496, 126.9139).getOrThrow().checkedInAtMillis)
    }

    @Test
    fun 신청자_목록을_앱_모델로_바꾼다() = runTest {
        routes["GET /api/gatherings/1/applications"] = { ok("""{"confirmed":[],"pending":[{"applicationId":5,"userId":11,"nickname":"신청자","mannerTemperature":36.5,"message":"안녕","appliedAt":"2026-11-10T08:00:00Z","gymRecords":[{"bodyPart":"CHEST","recorded":true,"recordValue":85,"armLevel":null},{"bodyPart":"BACK","recorded":false,"recordValue":null,"armLevel":null},{"bodyPart":"ARM","recorded":true,"recordValue":null,"armLevel":"BEGINNER"}]}]}""") }

        val r = repo.getJoinRequests("1").getOrThrow().single()

        assertEquals("5", r.id)
        assertEquals("11", r.applicantId)
        assertEquals("안녕", r.message)
        assertEquals(85, r.applicantRecord.benchPressKg)
        assertNull(r.applicantRecord.pullUpCount)
        assertEquals(SkillLevel.BEGINNER, r.applicantRecord.armLevel)
    }

    @Test
    fun 헬스_모임을_개설하면_법정동_코드를_붙여_보내고_새_id를_돌려준다() = runTest {
        routes["POST /api/gatherings"] = { json("""{"code":201,"message":"ok","data":${detail()}}""", 201) }
        val meeting = Meeting(
            title = " 합정 가슴 운동 ", description = "", criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST), ExerciseRecord(benchPressKg = 80)),
            startAtMillis = 1_794_313_800_000L, durationMinutes = 60, capacity = 3, genderLimit = GenderLimit.FEMALE_ONLY,
            place = MeetingPlace("스포애니 합정점", "서울 마포구 양화로 45", "서울 마포구 서교동 464-1", "3층", 37.5496, 126.9139),
        )

        assertEquals("1", repo.createMeeting(meeting).getOrThrow())

        val body = seen.single().body.readUtf8()
        assertTrue(body.contains(""""regionCode":"1144012200""""))
        assertTrue(body.contains(""""startAt":"2026-11-10T12:30:00Z""""))
        assertTrue(body.contains(""""genderRule":"FEMALE_ONLY""""))
        assertTrue(body.contains(""""showMyRecord":true"""))
        assertTrue(body.contains(""""title":"합정 가슴 운동""""))
        assertFalse(body.contains("description")) // 빈 설명은 보내지 않는다
    }

    @Test
    fun 카카오_장소_id가_있으면_개설_요청에_실어_보내고_없으면_보내지_않는다() = runTest {
        routes["POST /api/gatherings"] = { json("""{"code":201,"message":"ok","data":${detail()}}""", 201) }
        fun meeting(kakaoId: String?) = Meeting(
            title = "합정 가슴 운동", description = "", criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST), ExerciseRecord(benchPressKg = 80)),
            startAtMillis = 1_794_313_800_000L, durationMinutes = 60, capacity = 3, genderLimit = GenderLimit.ANYONE,
            place = MeetingPlace("스포애니 합정점", "서울 마포구 양화로 45", "서울 마포구 서교동 464-1", "3층", 37.5496, 126.9139, kakaoId),
        )

        repo.createMeeting(meeting("26338954")).getOrThrow()
        repo.createMeeting(meeting(null)).getOrThrow()
        repo.createMeeting(meeting("  ")).getOrThrow()

        val bodies = seen.map { it.body.readUtf8() }
        assertTrue(bodies[0].contains(""""kakaoPlaceId":"26338954""""))
        assertFalse(bodies[1].contains("kakaoPlaceId"))
        assertFalse("빈 id는 보내지 않는다", bodies[2].contains("kakaoPlaceId"))
    }

    @Test
    fun 서버가_준_카카오_장소_id를_모임_장소에_담는다() = runTest {
        routes["GET /api/gatherings/1"] = { ok(detail().replace(""""kakaoPlaceId":null""", """"kakaoPlaceId":"26338954"""")) }

        assertEquals("26338954", repo.getMeeting("1").getOrThrow().place.kakaoPlaceId)
    }

    @Test
    fun 러닝과_등산_모임은_서버가_지원하지_않아_실패한다() = runTest {
        val result = repo.createMeeting(Meeting(criteria = MeetingCriteria.Running(5f, 360)))

        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
        assertTrue(seen.isEmpty())
    }

    @Test
    fun 지역_코드를_못_찾으면_개설하지_않는다() = runTest {
        val client = ApiClient(server.url("/").toString(), tokens, onSessionExpired = {})
        val noRegion = RealMeetingRepository(client.meetingApi, tokens, regions = { _, _ -> null })

        val result = noRegion.createMeeting(Meeting(criteria = MeetingCriteria.Health(setOf(BodyPart.CHEST)), place = MeetingPlace(name = "a", lotAddress = "b")))

        assertTrue(result.isFailure)
        assertTrue(seen.isEmpty())
    }

    @Test
    fun 수정은_바뀐_항목만_보낸다() = runTest {
        routes["GET /api/gatherings/1"] = { ok(detail()) }
        routes["PATCH /api/gatherings/1"] = { ok(detail()) }
        val current = repo.getMeeting("1").getOrThrow()
        seen.clear()

        assertTrue(repo.updateMeeting("1", current.copy(title = "새 제목")).isSuccess)

        val patch = seen.first { it.method == "PATCH" }.body.readUtf8()
        assertEquals("""{"title":"새 제목"}""", patch)
    }

    @Test
    fun 바뀐_게_없으면_수정_요청을_보내지_않는다() = runTest {
        routes["GET /api/gatherings/1"] = { ok(detail()) }
        val current = repo.getMeeting("1").getOrThrow()
        seen.clear()

        assertTrue(repo.updateMeeting("1", current).isSuccess)

        assertTrue(seen.none { it.method == "PATCH" })
    }

    @Test
    fun 취소와_삭제와_수락_거절은_맞는_주소로_보낸다() = runTest {
        routes["POST /api/gatherings/1/cancel"] = { json("""{"code":200,"message":"ok"}""") }
        routes["DELETE /api/gatherings/1"] = { json("""{"code":200,"message":"ok"}""") }
        routes["POST /api/gatherings/1/applications/5/accept"] = { json("""{"code":200,"message":"ok","data":{"applicationId":5,"status":"CONFIRMED","gatheringStatus":"RECRUITING"}}""") }
        routes["POST /api/gatherings/1/applications/6/reject"] = { json("""{"code":200,"message":"ok","data":{"applicationId":6,"status":"REJECTED","gatheringStatus":"RECRUITING"}}""") }

        assertTrue(repo.cancelMeeting("1", " 몸이 안 좋아요 ").isSuccess)
        assertTrue(repo.deleteMeeting("1").isSuccess)
        assertTrue(repo.accept("1", "5").isSuccess)
        assertTrue(repo.reject("1", "6").isSuccess)
        assertTrue(seen.first { it.path!!.endsWith("/cancel") }.body.readUtf8().contains(""""reason":"몸이 안 좋아요""""))
    }

    @Test
    fun 숫자가_아닌_모임_id는_없는_모임으로_실패한다() = runTest {
        assertTrue(repo.getMeeting("m-1").exceptionOrNull() is NoSuchElementException)
        assertTrue(seen.isEmpty())
    }

    @Test
    fun 내_활동은_개설과_참여의_지난_것과_예정인_것을_모두_합친다() = runTest {
        fun activity(id: Int, status: String, part: String?) = """{"gatheringId":$id,"participationId":1,"title":"모임$id","sport":"GYM","placeName":"장소","address":"주소","detailAddress":null,"startAt":"2026-11-10T12:30:00Z","endAt":"2026-11-10T13:30:00Z","hostId":2,"hostNickname":"호스트","confirmedCount":1,"capacity":3,"gatheringStatus":"$status","participationStatus":${if (part == null) "null" else "\"$part\""},"attendance":null,"result":null,"inProgress":false}"""
        routes["GET /api/users/me/gatherings"] = { req ->
            val q = req.path!!
            when {
                "role=host" in q && "period=upcoming" in q -> ok("""{"content":[${activity(1, "RECRUITING", null)}],"page":0,"size":50,"totalElements":1,"totalPages":1}""")
                "role=participant" in q && "period=past" in q -> ok("""{"content":[${activity(3, "FINISHED", "CONFIRMED")}],"page":0,"size":50,"totalElements":1,"totalPages":1}""")
                else -> ok("""{"content":[],"page":0,"size":50,"totalElements":0,"totalPages":0}""")
            }
        }

        val list = repo.getMyMeetings().getOrThrow()

        assertEquals(setOf("1", "3"), list.map { it.id }.toSet())
        assertEquals(MeetingStatus.ENDED, list.first { it.id == "3" }.status)
        assertEquals(MeetingRole.CONFIRMED, list.first { it.id == "3" }.roleOf("7"))
        assertNotNull(list.first { it.id == "1" })
    }

    @Test
    fun 시각_변환은_소수점과_오프셋을_다룬다() {
        assertEquals(1_794_313_800_000L, IsoTime.parse("2026-11-10T12:30:00Z"))
        assertEquals(1_794_313_800_123L, IsoTime.parse("2026-11-10T12:30:00.123456Z"))
        assertEquals(1_794_313_800_000L, IsoTime.parse("2026-11-10T21:30:00+09:00"))
        assertEquals("2026-11-10T12:30:00Z", IsoTime.format(1_794_313_800_000L))
    }
}
