package com.example.myapplication.data.repository

import com.example.myapplication.data.model.MateProfile
import com.example.myapplication.data.model.MateRequest
import com.example.myapplication.data.model.MateSummary
import com.example.myapplication.data.model.WorkedOutPerson

// 메이트 창구 (11a~11c, 15a~15c). 신청 대상 확인(함께 운동한 사람만)은 서버가 다시 한다 (service-rules 10장).
// 데이터 계약(docs/data-model.md)대로 모든 호출은 suspend이고 Result<T>를 돌려준다.
interface MateRepository {
    suspend fun getMates(): Result<List<MateSummary>> // 11a
    suspend fun getWorkedOut(): Result<List<WorkedOutPerson>> // 11b. 같은 모임에서 둘 다 체크인한 사람
    suspend fun getRequests(): Result<List<MateRequest>> // 11c. 내가 받은 신청
    suspend fun acceptRequest(requestId: String): Result<Unit>
    suspend fun rejectRequest(requestId: String): Result<Unit>
    suspend fun sendRequest(userId: String): Result<Unit>
    suspend fun getProfile(userId: String): Result<MateProfile> // 15
    suspend fun removeMate(userId: String): Result<Unit> // 메이트 끊기 (화면 디자인 없음)
    suspend fun setMateNotification(userId: String, enabled: Boolean): Result<Unit> // 메이트별 알림 (화면 디자인 없음)
}
