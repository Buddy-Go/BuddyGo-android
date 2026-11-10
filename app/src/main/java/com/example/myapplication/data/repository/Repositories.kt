package com.example.myapplication.data.repository

import com.example.myapplication.data.api.Api
import com.example.myapplication.data.api.KakaoRegionLookup

// TODO(임시) 서버가 준비되기 전까지 모든 화면이 같은 목 저장소를 나눠 쓴다.
// 화면마다 따로 만들면 한 화면에서 신청·찜·읽음 처리한 결과가 다른 화면에 보이지 않는다.
// 서버가 준비되면 여기만 Real 구현으로 바꾼다. 화면과 ViewModel은 그대로다.
object Repositories {
    // 모임 목록·상세·신청·체크인·개설 등은 서버와 연결했다. (서버는 헬스 모임만 지원한다)
    val meetings: MeetingRepository by lazy {
        if (!Api.isInitialized) FakeMeetingRepository()
        else RealMeetingRepository(Api.client.meetingApi, Api.tokens, KakaoRegionLookup())
    }

    // 인증·내 정보는 서버와 연결했다. 나머지 기능은 아직 목 저장소가 맡는다. (RealUserRepository 설명 참고)
    // (앱이 뜨지 않은 JVM 단위 테스트에서는 Api가 없어 목 저장소를 그대로 쓴다.)
    val users: UserRepository by lazy {
        val mock = FakeUserRepository()
        if (!Api.isInitialized) mock
        else RealUserRepository(Api.client.authApi, Api.client.userApi, Api.tokens, Api.client.reviewApi, fallback = mock)
    }
    val notifications: NotificationRepository by lazy {
        if (!Api.isInitialized) FakeNotificationRepository() else RealNotificationRepository(Api.client.notificationApi)
    }
    val notices: NoticeRepository by lazy {
        if (!Api.isInitialized) FakeNoticeRepository() else RealNoticeRepository(Api.client.noticeApi)
    }
    val mates: MateRepository by lazy {
        if (!Api.isInitialized) FakeMateRepository()
        else RealMateRepository(Api.client.buddyApi, Api.client.userApi, Api.client.meetingApi, Api.client.reviewApi)
    }
    val reviews: ReviewRepository by lazy {
        if (!Api.isInitialized) FakeReviewRepository()
        else RealReviewRepository(Api.client.reviewApi, Api.client.meetingApi, Api.client.buddyApi)
    }
}
