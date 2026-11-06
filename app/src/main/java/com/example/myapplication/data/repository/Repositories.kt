package com.example.myapplication.data.repository

// TODO(임시) 서버가 준비되기 전까지 모든 화면이 같은 목 저장소를 나눠 쓴다.
// 화면마다 따로 만들면 한 화면에서 신청·찜·읽음 처리한 결과가 다른 화면에 보이지 않는다.
// 서버가 준비되면 여기만 Real 구현으로 바꾼다. 화면과 ViewModel은 그대로다.
object Repositories {
    val meetings: MeetingRepository by lazy { FakeMeetingRepository() }
    val users: UserRepository by lazy { FakeUserRepository() }
    val notifications: NotificationRepository by lazy { FakeNotificationRepository() }
}
