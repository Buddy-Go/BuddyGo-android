package com.example.myapplication.data.repository

import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.MateActivity
import com.example.myapplication.data.model.MateActivityKind
import com.example.myapplication.data.model.MateProfile
import com.example.myapplication.data.model.MateRelation
import com.example.myapplication.data.model.MateRequest
import com.example.myapplication.data.model.MateSummary
import com.example.myapplication.data.model.ProfileReview
import com.example.myapplication.data.model.TagCount
import com.example.myapplication.data.model.WorkedOutPerson

// 목데이터로 응답한다. 앱이 켜져 있는 동안만 메모리에 남는다.
// 여러 화면(11a·11b·11c·15)이 같은 데이터를 보도록 저장 공간은 companion object에 둔다.
// 사람은 MockUsers(서연 u2·지훈 u3·유나 u4)에 메이트 화면용 몇 명을 더했다.
class FakeMateRepository : MateRepository {

    override suspend fun getMates(): Result<List<MateSummary>> = Result.success(
        people.filter { it.relation == MateRelation.MATE }.map { p ->
            MateSummary(p.id, p.nickname, activities[p.id]?.minByOrNull { it.startAtMillis })
        },
    )

    override suspend fun getWorkedOut(): Result<List<WorkedOutPerson>> = Result.success(
        people.mapNotNull { p -> p.lastWorkedOutAt?.let { WorkedOutPerson(p.id, p.nickname, it, p.relation) } }
            .sortedByDescending { it.lastWorkedOutAtMillis },
    )

    override suspend fun getRequests(): Result<List<MateRequest>> = Result.success(requests.toList())

    override suspend fun acceptRequest(requestId: String): Result<Unit> {
        val request = requests.find { it.requestId == requestId } ?: return Result.failure(NoSuchElementException(requestId))
        requests.remove(request)
        update(request.userId) { it.copy(relation = MateRelation.MATE) }
        return Result.success(Unit)
    }

    override suspend fun rejectRequest(requestId: String): Result<Unit> {
        requests.removeAll { it.requestId == requestId }
        return Result.success(Unit)
    }

    override suspend fun sendRequest(userId: String): Result<Unit> {
        val person = people.find { it.id == userId } ?: return Result.failure(NoSuchElementException(userId))
        // 함께 운동한 사람에게만 신청할 수 있다 (service-rules 10장)
        if (person.lastWorkedOutAt == null) return Result.failure(IllegalStateException("함께 운동한 사람에게만 신청할 수 있어요"))
        if (person.relation == MateRelation.NONE) update(userId) { it.copy(relation = MateRelation.REQUESTED) }
        return Result.success(Unit)
    }

    override suspend fun getProfile(userId: String): Result<MateProfile> {
        val p = people.find { it.id == userId } ?: return Result.failure(NoSuchElementException(userId))
        val isMate = p.relation == MateRelation.MATE
        return Result.success(
            MateProfile(
                userId = p.id,
                nickname = p.nickname,
                mannerTemperature = p.manner,
                topTags = listOf(TagCount("시간 약속을 지켜요", 9), TagCount("서로 배려해요", 6)),
                relation = p.relation,
                workedOutTogether = p.lastWorkedOutAt != null,
                activities = if (isMate) activities[p.id].orEmpty() else emptyList(),
                reviewCount = 6,
                recentReviews = listOf(
                    ProfileReview(MockUsers.me.id, MockUsers.me.nickname, now - 3 * DAY, "서로 배려해요", "운동 루틴을 잘 짜주셔서 도움이 많이 됐어요."),
                ),
                notificationEnabled = p.id !in mutedMates,
            ),
        )
    }

    override suspend fun removeMate(userId: String): Result<Unit> {
        update(userId) { it.copy(relation = MateRelation.NONE) }
        return Result.success(Unit)
    }

    override suspend fun setMateNotification(userId: String, enabled: Boolean): Result<Unit> {
        if (enabled) mutedMates -= userId else mutedMates += userId
        return Result.success(Unit)
    }

    private fun update(userId: String, change: (Person) -> Person) {
        val index = people.indexOfFirst { it.id == userId }
        if (index >= 0) people[index] = change(people[index])
    }

    private data class Person(
        val id: String,
        val nickname: String,
        val manner: Float,
        val relation: MateRelation,
        val lastWorkedOutAt: Long?, // null이면 함께 운동한 적 없음
    )

    companion object {
        private const val HOUR = 3_600_000L
        private const val DAY = 24 * HOUR
        private val now = System.currentTimeMillis()

        private val people = mutableListOf(
            Person(MockUsers.hostA.id, MockUsers.hostA.nickname, MockUsers.hostA.mannerTemperature, MateRelation.MATE, now - 5 * DAY),
            Person(MockUsers.hostB.id, MockUsers.hostB.nickname, MockUsers.hostB.mannerTemperature, MateRelation.MATE, now - 14 * DAY),
            Person("mt1", "하늘", 38.0f, MateRelation.MATE, now - 30 * DAY),
            Person(MockUsers.noRecord.id, MockUsers.noRecord.nickname, MockUsers.noRecord.mannerTemperature, MateRelation.NONE, now - DAY),
            Person("mt2", "우진", 37.9f, MateRelation.REQUESTED, now - DAY),
            Person("mt3", "태윤", 40.2f, MateRelation.NONE, now - 16 * DAY),
            Person("mt4", "수아", 39.8f, MateRelation.NONE, now - 20 * DAY), // 나에게 신청함
            Person("mt5", "준호", 42.5f, MateRelation.NONE, now - 25 * DAY), // 나에게 신청함
            Person("mt6", "소영", 36.5f, MateRelation.NONE, null), // 함께 운동한 적 없음 → 15b에서 신청 버튼 숨김
        )

        private val activities = mapOf(
            MockUsers.hostA.id to listOf(
                MateActivity("a1", MateActivityKind.EXERCISING_NOW, "아침 가슴 운동", "에이블짐 홍대점", now - HOUR, 3, 3),
            ),
            MockUsers.hostB.id to listOf(
                MateActivity("a2", MateActivityKind.HOSTING, "퇴근 후 등·이두 같이 치실 분", "에이블짐 홍대점", now + 8 * HOUR, 1, 3),
            ),
        )

        private val requests = mutableListOf(
            MateRequest("q1", "mt5", "준호", 42.5f, 18),
            MateRequest("q2", "mt4", "수아", 39.8f, 9),
        )

        private val mutedMates = mutableSetOf<String>()
    }
}
