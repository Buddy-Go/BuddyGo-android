package com.example.myapplication.ui.navigation

import com.example.myapplication.data.model.Sport

// 화면 이름은 여기에만 정의하고, 코드에서는 상수와 함수로만 쓴다. (project-rules 7번)
// 화면 사이에는 ID(meetingId, userId 등)만 넘기고, 나머지 데이터는 도착한 화면이 불러온다.
// 바텀시트와 다이얼로그는 route로 만들지 않는다.
object Routes {
    // 인자 이름
    const val ARG_MEETING_ID = "meetingId"
    const val ARG_USER_ID = "userId"
    const val ARG_NOTICE_ID = "noticeId"
    const val ARG_SPORT = "sport" // Sport enum의 이름 (HEALTH, RUNNING, HIKING)

    // 1~2 로그인 · 회원가입 · 종목 기록
    const val LOGIN = "login"
    const val SIGN_UP_INFO = "sign_up_info"
    const val SIGN_UP_PROFILE = "sign_up_profile"
    const val RECORD = "record/{$ARG_SPORT}"

    // 3~4 탐색 · 조건 설정 · 알림
    const val EXPLORE = "explore"
    const val FILTER = "filter"
    const val LOCATION_SEARCH = "location_search"
    const val NOTIFICATION = "notification"

    // 5~6 모임 상세 · 참가 신청
    const val MEETING_DETAIL = "meeting/{$ARG_MEETING_ID}"
    const val APPLY = "meeting/{$ARG_MEETING_ID}/apply"

    // 7 참가자 관리 (호스트)
    const val PARTICIPANT_MANAGE = "meeting/{$ARG_MEETING_ID}/manage"

    // 8 공지
    const val NOTICE_LIST = "meeting/{$ARG_MEETING_ID}/notices"
    const val NOTICE_DETAIL = "meeting/{$ARG_MEETING_ID}/notices/{$ARG_NOTICE_ID}"
    // noticeId가 없으면 새 공지(8a), 있으면 수정(8b, 8c)
    const val NOTICE_EDIT = "meeting/{$ARG_MEETING_ID}/notice_edit?$ARG_NOTICE_ID={$ARG_NOTICE_ID}"

    // 10 모임 개설
    const val MEETING_CREATE = "meeting_create"
    const val MEETING_EDIT = "meeting/{$ARG_MEETING_ID}/edit"
    const val PLACE_SEARCH = "place_search"
    const val PLACE_CONFIRM = "place_confirm"

    // 11, 15 메이트
    const val MATE = "mate"
    const val MATE_REQUEST = "mate_request"
    const val MATE_PROFILE = "mate/profile/{$ARG_USER_ID}"

    // 12 메이트 평가
    const val REVIEW_LIST = "review"
    const val MEMBER_REVIEW = "review/{$ARG_MEETING_ID}/{$ARG_USER_ID}"
    const val REVIEW_DONE = "review_done"

    // 13~14 활동 내역 · 마이페이지
    const val ACTIVITY = "activity"
    const val MYPAGE = "mypage"

    // 하단 탭 바에 있는 화면 (탐색·활동·개설·메이트·마이)
    val bottomTabs = listOf(EXPLORE, ACTIVITY, MEETING_CREATE, MATE, MYPAGE)

    // 인자가 있는 화면으로 이동할 때 쓴다.
    fun record(sport: Sport) = "record/${sport.name}"
    fun meetingDetail(meetingId: String) = "meeting/$meetingId"
    fun apply(meetingId: String) = "meeting/$meetingId/apply"
    fun participantManage(meetingId: String) = "meeting/$meetingId/manage"
    fun noticeList(meetingId: String) = "meeting/$meetingId/notices"
    fun noticeDetail(meetingId: String, noticeId: String) = "meeting/$meetingId/notices/$noticeId"
    fun meetingEdit(meetingId: String) = "meeting/$meetingId/edit"
    fun noticeEdit(meetingId: String, noticeId: String? = null) =
        "meeting/$meetingId/notice_edit" + if (noticeId != null) "?$ARG_NOTICE_ID=$noticeId" else ""
    fun mateProfile(userId: String) = "mate/profile/$userId"
    fun memberReview(meetingId: String, userId: String) = "review/$meetingId/$userId"
}
