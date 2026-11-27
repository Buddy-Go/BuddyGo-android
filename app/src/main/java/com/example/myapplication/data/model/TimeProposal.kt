package com.example.myapplication.data.model

// 시작 시각 변경 제안 (서버 reschedule 도메인).
// 확정 참가자(호스트 제외)가 있는 모임은 호스트가 혼자 일시를 바꿀 수 없고, 구성원 누구나 제안해서 제안 시점 구성원 모두가 수락해야 바뀐다.
//  - PENDING: 진행 중. 한 모임에 하나만 있다
//  - APPROVED: 모두 수락해 시각이 바뀌었다
//  - REJECTED: 한 명이 거절했다 · WITHDRAWN: 제안자가 철회했다 · EXPIRED: 응답 기한까지 모두 수락하지 않았다
//  - CANCELED: 진행 중에 확정 명단이 바뀌었거나 모임이 취소됐다
enum class TimeProposalStatus { PENDING, APPROVED, REJECTED, WITHDRAWN, EXPIRED, CANCELED }

enum class VoteDecision { PENDING, ACCEPTED, REJECTED }

data class TimeProposalVote(
    val userId: String,
    val nickname: String,
    val isHost: Boolean,
    val decision: VoteDecision,
)

data class TimeProposal(
    val id: String,
    val status: TimeProposalStatus,
    val proposerId: String,
    val proposerNickname: String,
    val fromStartAtMillis: Long,   // 제안할 때의 (기존) 시작 시각
    val toStartAtMillis: Long,     // 제안한 시작 시각
    val expiresAtMillis: Long,     // 응답 기한. 이때까지 모두 수락하지 않으면 무산된다
    val votes: List<TimeProposalVote>, // 제안 시점 구성원. 호스트가 맨 앞
) {
    val acceptedCount: Int get() = votes.count { it.decision == VoteDecision.ACCEPTED }
    fun voteOf(userId: String): TimeProposalVote? = votes.find { it.userId == userId }
}
