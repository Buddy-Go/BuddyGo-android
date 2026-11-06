package com.example.myapplication.data.repository

import com.example.myapplication.data.model.CheckInResult
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.Meeting

// TODO(임시) 서버가 준비되면 MeetingApi로 연결한다.
class RealMeetingRepository : MeetingRepository {
    override suspend fun getMeetings(): Result<List<Meeting>> = TODO("서버 연결 후 구현")
    override suspend fun getMeeting(meetingId: String): Result<Meeting> = TODO("서버 연결 후 구현")
    override suspend fun setFavorite(meetingId: String, favorite: Boolean): Result<Unit> = TODO("서버 연결 후 구현")
    override suspend fun apply(meetingId: String, message: String?): Result<Unit> = TODO("서버 연결 후 구현")
    override suspend fun cancelApplication(meetingId: String): Result<Unit> = TODO("서버 연결 후 구현")
    override suspend fun leave(meetingId: String): Result<Unit> = TODO("서버 연결 후 구현")
    override suspend fun checkIn(meetingId: String, latitude: Double, longitude: Double): Result<CheckInResult> = TODO("서버 연결 후 구현")
    override suspend fun getJoinRequests(meetingId: String): Result<List<JoinRequest>> = TODO("서버 연결 후 구현")
    override suspend fun accept(meetingId: String, requestId: String): Result<Unit> = TODO("서버 연결 후 구현")
    override suspend fun reject(meetingId: String, requestId: String): Result<Unit> = TODO("서버 연결 후 구현")
    override suspend fun createMeeting(meeting: Meeting): Result<String> = TODO("서버 연결 후 구현")
    override suspend fun cancelMeeting(meetingId: String, reason: String): Result<Unit> = TODO("서버 연결 후 구현")
    override suspend fun deleteMeeting(meetingId: String): Result<Unit> = TODO("서버 연결 후 구현")
}
