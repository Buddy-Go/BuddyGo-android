package com.example.myapplication.data.repository

import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.Meeting

// 화면과 ViewModel은 이 interface만 호출한다. 규칙 판정(수락·마감 등)은 서버가 하고 앱은 결과만 받는다.
// 개설·수정·취소, 공지, 평가처럼 아직 없는 호출은 그 화면을 만드는 사람이 추가한다.
interface MeetingRepository {
    suspend fun getMeetings(): Result<List<Meeting>>
    suspend fun getMeeting(meetingId: String): Result<Meeting>

    // 참가자
    suspend fun apply(meetingId: String, message: String?): Result<Unit>
    suspend fun cancelApplication(meetingId: String): Result<Unit>
    suspend fun leave(meetingId: String): Result<Unit>

    // 호스트
    suspend fun getJoinRequests(meetingId: String): Result<List<JoinRequest>>
    suspend fun accept(meetingId: String, requestId: String): Result<Unit>
    suspend fun reject(meetingId: String, requestId: String): Result<Unit>

    // 호스트: 개설·취소·삭제 (service-rules 4장). 개설은 새 모임 id를 돌려준다.
    // 시작 시각·정원 검사 같은 규칙 판정은 서버가 하고, 어기면 failure로 돌아온다.
    suspend fun createMeeting(meeting: Meeting): Result<String>
    suspend fun cancelMeeting(meetingId: String, reason: String): Result<Unit>
    suspend fun deleteMeeting(meetingId: String): Result<Unit>
}
