package com.example.myapplication.data.repository

import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.ProfileStats
import com.example.myapplication.data.model.ReceivedReview
import com.example.myapplication.data.model.SignUpDraft
import com.example.myapplication.data.model.User

interface UserRepository {
    suspend fun login(email: String, password: String, keepLogin: Boolean): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun getUser(userId: String): Result<User>

    // 회원가입 중복 검사 (service-rules 1장). true = 사용할 수 있음
    // 회원가입 요청. 성공하면 가입된 사용자를 돌려준다.
    suspend fun signUp(draft: SignUpDraft): Result<User>

    suspend fun isNicknameAvailable(nickname: String): Result<Boolean>
    suspend fun isEmailAvailable(email: String): Result<Boolean>

    // 프로필 숫자(받은 후기 수, 개설한 모임 수, 많이 받은 태그)와 받은 후기(최신순)
    suspend fun getProfileStats(userId: String): Result<ProfileStats>
    suspend fun getReceivedReviews(userId: String): Result<List<ReceivedReview>>

    // 마이페이지 알림 설정: 켜져 있으면 true. 끄면 전체 알림을 받지 않는다. (service-rules 12장)
    suspend fun isNotificationEnabled(): Result<Boolean>
    suspend fun setNotificationEnabled(enabled: Boolean): Result<Unit>

    suspend fun logout(): Result<Unit>

    // 비밀번호 찾기: 이 이메일로 재설정 메일을 보낸다. 가입된 이메일인지는 알려 주지 않는다. (보안상 항상 같은 안내)
    suspend fun requestPasswordReset(email: String): Result<Unit>

    // 내 운동 기록을 저장한다 (service-rules 1장). 모임 신청·기준 비교에 쓰이는 프로필 기록이다. 종목별로 일부만 바꿔도 전체를 보낸다.
    // 이미 개설한 모임의 기록 복사본에는 영향이 없다. (개설 시점 값이 그대로 남는다)
    suspend fun updateRecord(record: ExerciseRecord): Result<Unit>
}
