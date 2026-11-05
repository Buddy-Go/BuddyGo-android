package com.example.myapplication.data.model

enum class JoinRequestStatus { PENDING, ACCEPTED, REJECTED, CANCELED }

// 참가 신청. 호스트에게 가는 정보: 닉네임, 매너온도, 이 종목의 내 기록, 한마디 (5장)
data class JoinRequest(
    val id: String = "",
    val meetingId: String = "",
    val applicantId: String = "",
    val applicantNickname: String = "",
    val applicantMannerTemperature: Float = 0f,
    val applicantRecord: ExerciseRecord = ExerciseRecord(), // 비어 있으면 '미설정'
    val message: String? = null,
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val requestedAtMillis: Long = 0L,
)
