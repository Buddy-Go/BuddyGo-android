package com.example.myapplication.data.model

// 메이트 모델 (service-rules 10장). 메이트 화면(11, 15, 문진곤)에서 추가했다.
// 시각은 epoch 밀리초(Long), 매너온도는 User와 같이 Float.

// 나와 그 사람의 관계
enum class MateRelation {
    NONE, // 아무 관계 없음 → '메이트 신청'
    REQUESTED, // 내가 신청을 보냄 → '신청함'
    MATE, // 메이트
}

// 메이트에게 보이는 활동: 개설한 모임, 신청(대기)·확정한 모임, 지금 진행 중인 운동 (service-rules 10장)
enum class MateActivityKind { EXERCISING_NOW, HOSTING, JOINED }

data class MateActivity(
    val meetingId: String,
    val kind: MateActivityKind,
    val title: String,
    val placeName: String,
    val startAtMillis: Long,
    val confirmedCount: Int = 0,
    val capacity: Int = 0,
)

data class MateSummary(
    val userId: String,
    val nickname: String,
    val nextActivity: MateActivity?, // 가장 가까운 활동 하나. 없으면 '예정된 모임 없어요'
)

data class WorkedOutPerson(
    val userId: String,
    val nickname: String,
    val lastWorkedOutAtMillis: Long,
    val relation: MateRelation,
    val sports: Set<Sport> = setOf(Sport.HEALTH), // 함께 운동한 종목 (11b-2 종목 칩)
)

data class MateRequest(
    val requestId: String,
    val userId: String,
    val nickname: String,
    val mannerTemperature: Float,
    val joinedCount: Int, // 참여 횟수
)

data class TagCount(val label: String, val count: Int)

data class ProfileReview(
    val authorId: String,
    val authorNickname: String,
    val createdAtMillis: Long,
    val tag: String?,
    val comment: String,
)

data class MateProfile(
    val userId: String,
    val nickname: String,
    val mannerTemperature: Float,
    val topTags: List<TagCount>,
    val relation: MateRelation,
    val workedOutTogether: Boolean, // 아니면 '메이트 신청' 버튼을 숨긴다
    val activities: List<MateActivity>, // 메이트일 때만 서버가 채운다
    val reviewCount: Int,
    val recentReviews: List<ProfileReview>,
    val notificationEnabled: Boolean = true, // 메이트 알림. 기본 ON
)
