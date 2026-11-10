package com.example.myapplication.data.repository

import com.example.myapplication.data.model.CheckInResult
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.MapBounds
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingMember

// 화면과 ViewModel은 이 interface만 호출한다. 규칙 판정(수락·마감 등)은 서버가 하고 앱은 결과만 받는다.
// 개설·수정·취소, 공지, 평가처럼 아직 없는 호출은 그 화면을 만드는 사람이 추가한다.
// 서버가 지번 단위로 묶어 준 지도 핀 하나
data class MeetingPin(val parcelKey: String, val meetingCount: Int, val latitude: Double, val longitude: Double)

interface MeetingRepository {
    // area가 있으면 지도에 보이는 영역 안의 모임만 준다('현 지도에서 검색'). 없으면 전체. 서버가 영역으로 거른다.
    suspend fun getMeetings(area: MapBounds? = null): Result<List<Meeting>>
    suspend fun getMeeting(meetingId: String): Result<Meeting>

    // 내가 개설했거나 신청·참여한 모임 전부(모집중이 아닌 것 포함). 활동 내역 화면용이다.
    // 서버 목록은 모집중인 모임만 주기 때문에 getMeetings와 따로 둔다. 목 저장소는 전부 들고 있어 같은 값이다.
    suspend fun getMyMeetings(): Result<List<Meeting>> = getMeetings()

    // 지도 핀(지번이 같은 모임 묶음). 앱은 지금 모임 목록으로 핀을 직접 묶어서 쓰고, 서버가 묶어 주는 값이 필요해지면 이걸 쓴다.
    suspend fun getPins(area: MapBounds? = null): Result<List<MeetingPin>> = Result.failure(UnsupportedOperationException())

    // 확정 명단. 마감된 모임의 호스트·확정 참가자만 받을 수 있다. 서버가 거절하면 failure다.
    suspend fun getParticipants(meetingId: String): Result<List<MeetingMember>> = Result.failure(UnsupportedOperationException())

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
