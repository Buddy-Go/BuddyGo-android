package com.example.myapplication.data.repository

import com.example.myapplication.data.api.IsoTime
import com.example.myapplication.data.api.TimeProposalApi
import com.example.myapplication.data.api.TimeProposalCreateRequest
import com.example.myapplication.data.api.TimeProposalDto
import com.example.myapplication.data.api.apiCall
import com.example.myapplication.data.api.requireData
import com.example.myapplication.data.mock.MockUsers
import com.example.myapplication.data.model.TimeProposal
import com.example.myapplication.data.model.TimeProposalStatus
import com.example.myapplication.data.model.TimeProposalVote
import com.example.myapplication.data.model.VoteDecision

// 시작 시각 변경 제안 창구. 제안할 수 있는 시각·기한 같은 규칙 판정은 서버가 다시 하고, 어기면 ApiException(TP00x)으로 실패한다.
interface TimeProposalRepository {
    // 진행 중인 제안. 없으면 success(null)
    suspend fun getCurrent(meetingId: String): Result<TimeProposal?>
    suspend fun propose(meetingId: String, startAtMillis: Long): Result<TimeProposal>
    // 수락 결과. 마지막 수락이면 status=APPROVED, 그 사이 명단이 바뀌었으면 200인데 CANCELED일 수 있다
    suspend fun accept(meetingId: String, proposalId: String): Result<TimeProposal>
    suspend fun reject(meetingId: String, proposalId: String): Result<TimeProposal>
    suspend fun withdraw(meetingId: String, proposalId: String): Result<Unit>
}

class RealTimeProposalRepository(private val api: TimeProposalApi) : TimeProposalRepository {

    override suspend fun getCurrent(meetingId: String): Result<TimeProposal?> = withIds(meetingId) { g ->
        apiCall { api.current(g).data?.toModel() }
    }

    override suspend fun propose(meetingId: String, startAtMillis: Long): Result<TimeProposal> = withIds(meetingId) { g ->
        apiCall { api.create(g, TimeProposalCreateRequest(IsoTime.format(startAtMillis))).requireData().toModel() }
    }

    override suspend fun accept(meetingId: String, proposalId: String): Result<TimeProposal> = withIds(meetingId, proposalId) { g, p ->
        apiCall { api.accept(g, p).requireData().toModel() }
    }

    override suspend fun reject(meetingId: String, proposalId: String): Result<TimeProposal> = withIds(meetingId, proposalId) { g, p ->
        apiCall { api.reject(g, p).requireData().toModel() }
    }

    override suspend fun withdraw(meetingId: String, proposalId: String): Result<Unit> = withIds(meetingId, proposalId) { g, p ->
        apiCall { api.withdraw(g, p) }.map { }
    }

    private inline fun <T> withIds(meetingId: String, block: (Long) -> Result<T>): Result<T> =
        meetingId.toLongOrNull()?.let(block) ?: Result.failure(NoSuchElementException(meetingId))

    private inline fun <T> withIds(meetingId: String, proposalId: String, block: (Long, Long) -> Result<T>): Result<T> {
        val g = meetingId.toLongOrNull() ?: return Result.failure(NoSuchElementException(meetingId))
        val p = proposalId.toLongOrNull() ?: return Result.failure(NoSuchElementException(proposalId))
        return block(g, p)
    }
}

internal fun TimeProposalDto.toModel() = TimeProposal(
    id = proposalId.toString(),
    status = runCatching { TimeProposalStatus.valueOf(status) }.getOrDefault(TimeProposalStatus.CANCELED),
    proposerId = proposerId.toString(),
    proposerNickname = proposerNickname,
    fromStartAtMillis = IsoTime.parse(fromStartAt),
    toStartAtMillis = IsoTime.parse(toStartAt),
    expiresAtMillis = IsoTime.parse(expiresAt),
    votes = votes.map {
        TimeProposalVote(
            userId = it.userId.toString(),
            nickname = it.nickname,
            isHost = it.host,
            decision = runCatching { VoteDecision.valueOf(it.decision) }.getOrDefault(VoteDecision.PENDING),
        )
    },
)

// 목데이터로 응답한다. 서버 규칙은 흉내 내지 않고, 모두 수락하면 APPROVED가 되는 정도만 한다.
// initial로 모임 id별 진행 중 제안을 넣어 둘 수 있다(미리보기·테스트용).
class FakeTimeProposalRepository(
    initial: Map<String, TimeProposal> = emptyMap(),
    private val meId: String = MockUsers.me.id,
) : TimeProposalRepository {
    private val current = initial.toMutableMap()
    private var nextId = 100

    override suspend fun getCurrent(meetingId: String): Result<TimeProposal?> = Result.success(current[meetingId])

    override suspend fun propose(meetingId: String, startAtMillis: Long): Result<TimeProposal> {
        val proposal = TimeProposal(
            id = "tp${nextId++}", status = TimeProposalStatus.PENDING, proposerId = meId, proposerNickname = MockUsers.me.nickname,
            fromStartAtMillis = startAtMillis, toStartAtMillis = startAtMillis, expiresAtMillis = startAtMillis - 30 * 60_000L,
            votes = listOf(TimeProposalVote(meId, MockUsers.me.nickname, isHost = true, decision = VoteDecision.ACCEPTED)),
        )
        current[meetingId] = proposal
        return Result.success(proposal)
    }

    override suspend fun accept(meetingId: String, proposalId: String): Result<TimeProposal> = respond(meetingId, VoteDecision.ACCEPTED)
    override suspend fun reject(meetingId: String, proposalId: String): Result<TimeProposal> = respond(meetingId, VoteDecision.REJECTED)

    override suspend fun withdraw(meetingId: String, proposalId: String): Result<Unit> {
        current.remove(meetingId) ?: return Result.failure(NoSuchElementException(proposalId))
        return Result.success(Unit)
    }

    private fun respond(meetingId: String, decision: VoteDecision): Result<TimeProposal> {
        val p = current[meetingId] ?: return Result.failure(NoSuchElementException(meetingId))
        val votes = p.votes.map { if (it.userId == meId) it.copy(decision = decision) else it }
        val status = when {
            decision == VoteDecision.REJECTED -> TimeProposalStatus.REJECTED
            votes.all { it.decision == VoteDecision.ACCEPTED } -> TimeProposalStatus.APPROVED
            else -> TimeProposalStatus.PENDING
        }
        val next = p.copy(status = status, votes = votes)
        if (status == TimeProposalStatus.PENDING) current[meetingId] = next else current.remove(meetingId)
        return Result.success(next)
    }
}
