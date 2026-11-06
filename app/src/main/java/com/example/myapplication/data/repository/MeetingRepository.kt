package com.example.myapplication.data.repository

import com.example.myapplication.data.model.CheckInResult
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.Meeting

// 화면과 ViewModel은 이 interface만 호출한다. 규칙 판정(수락·마감 등)은 서버가 하고 앱은 결과만 받는다.
// 개설·수정·취소, 공지, 평가처럼 아직 없는 호출은 그 화면을 만드는 사람이 추가한다.
interface MeetingRepository {
    suspend fun getMeetings(): Result<List<Meeting>>
    suspend fun getMeeting(meetingId: String): Result<Meeting>

    // 찜 (service-rules 2장). 켜고 끄는 값을 그대로 보낸다. 서버가 같은 값을 받아도 결과는 같다.
    suspend fun setFavorite(meetingId: String, favorite: Boolean): Result<Unit>

    // 참가자
    suspend fun apply(meetingId: String, message: String?): Result<Unit>
    suspend fun cancelApplication(meetingId: String): Result<Unit>
    suspend fun leave(meetingId: String): Result<Unit>

    // 체크인 (service-rules 7장): 기기 GPS 좌표를 보내면 서버가 모임 장소와의 거리를 재서 범위 안일 때만 성공시킨다.
    // 범위 밖이면 CheckInOutOfRangeException으로 실패한다. 체크인은 한 번만 할 수 있다.
    suspend fun checkIn(meetingId: String, latitude: Double, longitude: Double): Result<CheckInResult>

    // 호스트
    suspend fun getJoinRequests(meetingId: String): Result<List<JoinRequest>>
    suspend fun accept(meetingId: String, requestId: String): Result<Unit>
    suspend fun reject(meetingId: String, requestId: String): Result<Unit>

    // 호스트: 개설·취소·삭제 (service-rules 4장). 개설은 새 모임 id를 돌려준다.
    // 시작 시각·정원 검사 같은 규칙 판정은 서버가 하고, 어기면 failure로 돌아온다.
    suspend fun createMeeting(meeting: Meeting): Result<String>

    // 호스트: 모임 수정 (service-rules 4장). 바뀐 값을 담은 Meeting 전체를 보낸다.
    // 확정 참가자(호스트 제외)가 있으면 서버는 제목·설명·상세주소만 반영하고 나머지는 무시한다.
    // 진행중이 된 뒤이거나, 시작이 지금부터 1시간 30분 안이 되는 일시 변경은 failure로 돌아온다.
    suspend fun updateMeeting(meetingId: String, meeting: Meeting): Result<Unit>
    suspend fun cancelMeeting(meetingId: String, reason: String): Result<Unit>
    suspend fun deleteMeeting(meetingId: String): Result<Unit>
}
