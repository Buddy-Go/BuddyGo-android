package com.example.myapplication.data.mock

import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.ProfileStats
import com.example.myapplication.data.model.ReceivedReview
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.TagCount
import com.example.myapplication.data.model.User

object MockUsers {
    // 로그인한 나. 기록이 있는 사용자
    val me = User(
        id = "u1", name = "김민준", nickname = "민준", email = "minjun.kim@gmail.com",
        gender = Gender.MALE, mannerTemperature = 36.5f,
        preferredSports = listOf(Sport.HEALTH, Sport.RUNNING),
        record = ExerciseRecord(
            benchPressKg = 80, pullUpCount = 8, squatKg = 100, overheadPressKg = 50,
            armLevel = SkillLevel.INTERMEDIATE, runningPaceSec = 330, hikingStyle = HikingStyle.HOBBY,
        ),
    )

    // 기록이 없는 사용자 ('미설정' 상태 확인용)
    val noRecord = User(
        id = "u2", name = "이서연", nickname = "서연", email = "seoyeon@gmail.com",
        gender = Gender.FEMALE, mannerTemperature = 38.2f,
    )

    val hostA = User(
        id = "u3", name = "박지훈", nickname = "지훈", email = "jihoon@gmail.com",
        gender = Gender.MALE, mannerTemperature = 41.0f,
        preferredSports = listOf(Sport.HEALTH),
        record = ExerciseRecord(benchPressKg = 90, pullUpCount = 10, squatKg = 120, armLevel = SkillLevel.INTERMEDIATE),
    )

    val hostB = User(
        id = "u4", name = "최유나", nickname = "유나", email = "yuna@gmail.com",
        gender = Gender.FEMALE, mannerTemperature = 37.0f,
        preferredSports = listOf(Sport.RUNNING, Sport.HIKING),
        record = ExerciseRecord(runningPaceSec = 345, hikingStyle = HikingStyle.PRO),
    )

    val all = listOf(me, noRecord, hostA, hostB)

    // 내 프로필 숫자와 받은 후기 (마이페이지 14)
    val meStats = ProfileStats(
        receivedReviewCount = 32, hostedMeetingCount = 9,
        topTags = listOf(TagCount("시간 잘 지킴", 21), TagCount("잘 이끌어줌", 14), TagCount("또 하고 싶어요", 11)),
    )
    val meReviews = listOf(
        ReceivedReview(
            id = "rv1", reviewerNickname = "서연", sport = Sport.HIKING, verdict = ReviewVerdict.GOOD,
            comment = "시간 딱 맞춰 오시고 초보도 잘 챙겨주셨어요!", tags = listOf("시간 잘 지킴"),
            createdAtMillis = 1_788_220_800_000L,
        ),
        ReceivedReview(
            id = "rv2", reviewerNickname = "지훈", sport = Sport.HEALTH, verdict = ReviewVerdict.NORMAL,
            comment = null, createdAtMillis = 1_787_000_000_000L,
        ),
    )
}
