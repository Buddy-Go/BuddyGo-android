package com.example.myapplication.data.model

// 회원가입 3단계(정보 → 프로필 → 종목 기록)에서 입력 중인 값. 마지막에 한 번에 가입 요청을 보낸다.
// 비밀번호는 가입 요청을 보내기 전까지 이 화면 흐름 안에서만 들고 있고, User 모델에는 넣지 않는다.
data class SignUpDraft(
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val password: String = "",
    val gender: Gender? = null,
    val preferredSports: List<Sport> = emptyList(),
    val record: ExerciseRecord = ExerciseRecord(),
)
