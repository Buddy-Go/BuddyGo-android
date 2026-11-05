package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.navArgument
import com.example.myapplication.data.repository.FakeMateRepository
import com.example.myapplication.data.repository.FakeMeetingRepository
import com.example.myapplication.data.repository.FakeNoticeRepository
import com.example.myapplication.data.repository.FakeUserRepository
import com.example.myapplication.ui.activity.ActivityScreen
import com.example.myapplication.ui.activity.ActivityViewModel
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.common.BottomNavBar
import com.example.myapplication.ui.mate.MateProfileScreen
import com.example.myapplication.ui.mate.MateProfileViewModel
import com.example.myapplication.ui.mate.MateRequestScreen
import com.example.myapplication.ui.mate.MateRequestViewModel
import com.example.myapplication.ui.mate.MateScreen
import com.example.myapplication.ui.mate.MateViewModel
import com.example.myapplication.ui.meeting.create.MeetingCreateScreen
import com.example.myapplication.ui.meeting.create.MeetingCreateViewModel
import com.example.myapplication.ui.meeting.manage.ParticipantManageScreen
import com.example.myapplication.ui.meeting.manage.ParticipantManageViewModel
import com.example.myapplication.ui.meeting.notice.NoticeDetailScreen
import com.example.myapplication.ui.meeting.notice.NoticeDetailViewModel
import com.example.myapplication.ui.meeting.notice.NoticeEditScreen
import com.example.myapplication.ui.meeting.notice.NoticeEditViewModel
import com.example.myapplication.ui.meeting.notice.NoticeListScreen
import com.example.myapplication.ui.meeting.notice.NoticeListViewModel

// 이름표(Routes)와 실제 화면을 연결한다.
// 화면을 만든 사람은 해당 줄의 TempScreen(...)을 자기 화면으로 바꾼다.
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // TODO(임시): 서버 연결 전까지 가짜 저장소를 앱 전체에서 하나씩 같이 쓴다.
    // (화면마다 새로 만들면 수락·공지 작성 결과가 다른 화면에 안 보인다)
    val meetingRepository = remember { FakeMeetingRepository() }
    val userRepository = remember { FakeUserRepository() }
    val noticeRepository = remember { FakeNoticeRepository() }
    val mateRepository = remember { FakeMateRepository() }

    // 탭 5개 화면에서만 하단 탭 바를 보여 준다.
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentRoute in Routes.bottomTabs) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onTabClick = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.EXPLORE) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding),
        ) {
            // 1~2 로그인 · 회원가입 · 종목 기록
            composable(Routes.LOGIN) {
                LoginScreen(
                    // TODO(임시) 인증은 아직 연결하지 않아 누르면 바로 탐색으로 이동한다.
                    onLoginClick = { _, _, _ ->
                        navController.navigate(Routes.EXPLORE) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onSignUpClick = { navController.navigate(Routes.SIGN_UP_INFO) },
                )
            }
            composable(Routes.SIGN_UP_INFO) { TempScreen("2a 회원가입 정보 입력") }
            composable(Routes.SIGN_UP_PROFILE) { TempScreen("2b 프로필 설정") }
            composable(Routes.RECORD) { TempScreen("2c~2e 종목 기록") }

            // 3~4 탐색 · 조건 설정 · 알림
            composable(Routes.EXPLORE) { TempScreen("3 탐색") }
            composable(Routes.FILTER) { TempScreen("4a~4c 조건 설정") }
            composable(Routes.LOCATION_SEARCH) { TempScreen("4g 위치 검색") }
            composable(Routes.NOTIFICATION) { TempScreen("3-6 알림 목록") }

            // 5~6 모임 상세 · 참가 신청
            composable(Routes.MEETING_DETAIL, meetingArgs()) { TempScreen("5 모임 상세") }
            composable(Routes.APPLY, meetingArgs()) { TempScreen("6 참가 신청") }

            // 7 참가자 관리
            composable(Routes.PARTICIPANT_MANAGE, meetingArgs()) { entry ->
                val meetingId = entry.meetingId()
                ParticipantManageScreen(
                    viewModel = viewModel { ParticipantManageViewModel(meetingId, meetingRepository, userRepository) },
                    onBack = { navController.popBackStack() },
                )
            }

            // 8 공지
            composable(Routes.NOTICE_LIST, meetingArgs()) { entry ->
                val meetingId = entry.meetingId()
                NoticeListScreen(
                    viewModel = viewModel { NoticeListViewModel(meetingId, noticeRepository, meetingRepository, userRepository) },
                    onBack = { navController.popBackStack() },
                    onNoticeClick = { noticeId -> navController.navigate(Routes.noticeDetail(meetingId, noticeId)) },
                    onWriteClick = { navController.navigate(Routes.noticeEdit(meetingId)) },
                    onEditClick = { noticeId -> navController.navigate(Routes.noticeEdit(meetingId, noticeId)) },
                )
            }
            composable(
                Routes.NOTICE_DETAIL,
                listOf(
                    navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType },
                    navArgument(Routes.ARG_NOTICE_ID) { type = NavType.StringType },
                ),
            ) { entry ->
                val meetingId = entry.meetingId()
                val noticeId = entry.arguments?.getString(Routes.ARG_NOTICE_ID).orEmpty()
                NoticeDetailScreen(
                    viewModel = viewModel {
                        NoticeDetailViewModel(meetingId, noticeId, noticeRepository, meetingRepository, userRepository)
                    },
                    onBack = { navController.popBackStack() },
                    onEditClick = { id -> navController.navigate(Routes.noticeEdit(meetingId, id)) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                Routes.NOTICE_EDIT,
                listOf(
                    navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType },
                    navArgument(Routes.ARG_NOTICE_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val meetingId = entry.meetingId()
                val noticeId = entry.arguments?.getString(Routes.ARG_NOTICE_ID)
                NoticeEditScreen(
                    viewModel = viewModel { NoticeEditViewModel(meetingId, noticeId, noticeRepository) },
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }

            // 10 모임 개설
            composable(Routes.MEETING_CREATE) {
                MeetingCreateScreen(
                    viewModel = viewModel { MeetingCreateViewModel(userRepository) },
                    onClose = { navController.popBackStack() },
                    onPlaceClick = { navController.navigate(Routes.PLACE_SEARCH) },
                )
            }
            composable(Routes.PLACE_SEARCH) { TempScreen("10g 장소 검색") }
            composable(Routes.PLACE_CONFIRM) { TempScreen("10g-3~4 장소 확인·집결지") }

            // 11, 15 메이트
            composable(Routes.MATE) {
                MateScreen(
                    viewModel = viewModel { MateViewModel(mateRepository) },
                    onRequestsClick = { navController.navigate(Routes.MATE_REQUEST) },
                    onProfileClick = { userId -> navController.navigate(Routes.mateProfile(userId)) },
                )
            }
            composable(Routes.MATE_REQUEST) {
                MateRequestScreen(
                    viewModel = viewModel { MateRequestViewModel(mateRepository) },
                    onBack = { navController.popBackStack() },
                    onProfileClick = { userId -> navController.navigate(Routes.mateProfile(userId)) },
                )
            }
            composable(
                Routes.MATE_PROFILE,
                listOf(navArgument(Routes.ARG_USER_ID) { type = NavType.StringType }),
            ) { entry ->
                val userId = entry.arguments?.getString(Routes.ARG_USER_ID).orEmpty()
                MateProfileScreen(
                    viewModel = viewModel { MateProfileViewModel(userId, mateRepository) },
                    onBack = { navController.popBackStack() },
                    onActivityClick = { meetingId -> navController.navigate(Routes.meetingDetail(meetingId)) },
                    onAllReviewsClick = { /* TODO(임시): 후기 전체 화면은 디자인 없음 */ },
                )
            }

            // 12 메이트 평가
            // TODO(문찬주 확인): 12a~12c는 한 모임의 평가 흐름이라 meetingId가 필요하다.
            // REVIEW_LIST를 "review/{meetingId}"로 바꾸는 데 합의하면 ReviewListScreen 등을 연결한다.
            composable(Routes.REVIEW_LIST) { TempScreen("12a 평가 목록") }
            composable(
                Routes.MEMBER_REVIEW,
                listOf(
                    navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType },
                    navArgument(Routes.ARG_USER_ID) { type = NavType.StringType },
                ),
            ) { TempScreen("12b 멤버 평가") }
            composable(Routes.REVIEW_DONE) { TempScreen("12c 평가 완료") }

            // 13~14 활동 내역 · 마이페이지
            composable(Routes.ACTIVITY) {
                ActivityScreen(
                    viewModel = viewModel { ActivityViewModel() },
                    onReviewClick = { navController.navigate(Routes.REVIEW_LIST) }, // TODO: meetingId 넘기기 (12번 route 합의 후)
                    onMeetingClick = { meetingId -> navController.navigate(Routes.meetingDetail(meetingId)) },
                    onCreateClick = { navController.navigate(Routes.MEETING_CREATE) },
                    onExploreClick = { navController.navigate(Routes.EXPLORE) },
                )
            }
            composable(Routes.MYPAGE) { TempScreen("14 마이페이지") }
        }
    }
}

private fun NavBackStackEntry.meetingId(): String =
    arguments?.getString(Routes.ARG_MEETING_ID).orEmpty()

private fun meetingArgs() = listOf(navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType })
