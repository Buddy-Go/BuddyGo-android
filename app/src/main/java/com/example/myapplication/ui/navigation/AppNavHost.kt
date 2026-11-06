package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.FakeMateRepository
import com.example.myapplication.data.repository.FakeNoticeRepository
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.ui.activity.ActivityScreen
import com.example.myapplication.ui.activity.ActivityViewModel
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.common.BottomNavBar
import com.example.myapplication.ui.explore.ExploreScreen
import com.example.myapplication.ui.explore.filter.FilterScreen
import com.example.myapplication.ui.explore.filter.FilterViewModel
import com.example.myapplication.ui.explore.filter.LocationSearchScreen
import com.example.myapplication.ui.mate.MateProfileScreen
import com.example.myapplication.ui.mate.MateProfileViewModel
import com.example.myapplication.ui.mate.MateRequestScreen
import com.example.myapplication.ui.mate.MateRequestViewModel
import com.example.myapplication.ui.mate.MateScreen
import com.example.myapplication.ui.mate.MateViewModel
import com.example.myapplication.ui.meeting.create.MeetingCreateScreen
import com.example.myapplication.ui.meeting.create.MeetingCreateViewModel
import com.example.myapplication.ui.meeting.detail.ApplyScreen
import com.example.myapplication.ui.meeting.detail.MeetingDetailScreen
import com.example.myapplication.ui.meeting.manage.ParticipantManageScreen
import com.example.myapplication.ui.meeting.manage.ParticipantManageViewModel
import com.example.myapplication.ui.meeting.notice.NoticeDetailScreen
import com.example.myapplication.ui.meeting.notice.NoticeDetailViewModel
import com.example.myapplication.ui.meeting.notice.NoticeEditScreen
import com.example.myapplication.ui.meeting.notice.NoticeEditViewModel
import com.example.myapplication.ui.meeting.notice.NoticeListScreen
import com.example.myapplication.ui.meeting.notice.NoticeListViewModel
import com.example.myapplication.ui.mypage.MyPageScreen
import com.example.myapplication.ui.notification.NotificationScreen
import com.example.myapplication.ui.record.RecordScreen
import com.example.myapplication.ui.record.SignUpInfoScreen
import com.example.myapplication.ui.record.SignUpProfileScreen

// 위치 검색(4g)과 조건 설정 사이에 구·동을 주고받는 키. 결과가 없을 때는 NO_RESULT, 해제는 빈 문자열이다.
private const val KEY_DISTRICT_CURRENT = "district_current"
private const val KEY_DISTRICT_RESULT = "district_result"
private const val NO_RESULT = "\u0000none"
// 신청 화면(6)이 끝나면 모임 상세에 알리는 키
private const val KEY_APPLIED = "applied"


// 이름표(Routes)와 실제 화면을 연결한다.
// 화면을 만든 사람은 해당 줄의 TempScreen(...)을 자기 화면으로 바꾼다.
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // TODO(임시): 서버 연결 전까지 가짜 저장소를 앱 전체에서 하나씩 같이 쓴다.
    // (화면마다 새로 만들면 수락·공지 작성 결과가 다른 화면에 안 보인다)
    val meetingRepository = Repositories.meetings
    val userRepository = Repositories.users
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
            composable(Routes.SIGN_UP_INFO) {
                SignUpInfoScreen(
                    onBack = { navController.popBackStack() },
                    onNext = { navController.navigate(Routes.SIGN_UP_PROFILE) },
                )
            }
            composable(Routes.SIGN_UP_PROFILE) {
                SignUpProfileScreen(
                    onBack = { navController.popBackStack() },
                    onEditRecord = { sport -> navController.navigate(Routes.record(sport)) },
                    // 가입이 끝나면 로그인·회원가입 화면을 모두 지우고 탐색으로 이동한다.
                    onComplete = {
                        navController.navigate(Routes.EXPLORE) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                Routes.RECORD,
                listOf(navArgument(Routes.ARG_SPORT) { type = NavType.StringType }),
            ) { entry ->
                val sport = Sport.valueOf(entry.arguments?.getString(Routes.ARG_SPORT) ?: Sport.HEALTH.name)
                RecordScreen(sport = sport, onBack = { navController.popBackStack() })
            }

            // 3~4 탐색 · 조건 설정 · 알림
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onFilterClick = { navController.navigate(Routes.FILTER) },
                    onNotificationClick = { navController.navigate(Routes.NOTIFICATION) },
                    onMeetingClick = { navController.navigate(Routes.meetingDetail(it)) },
                )
            }
            composable(Routes.FILTER) { entry ->
                val filterViewModel: FilterViewModel = viewModel(entry)
                // 위치 검색에서 돌아오면 고른 구·동(빈 문자열이면 해제)을 조건에 넣는다.
                val picked by entry.savedStateHandle.getStateFlow(KEY_DISTRICT_RESULT, NO_RESULT).collectAsState()
                LaunchedEffect(picked) {
                    if (picked != NO_RESULT) {
                        filterViewModel.onSetDistrict(picked)
                        entry.savedStateHandle[KEY_DISTRICT_RESULT] = NO_RESULT
                    }
                }
                FilterScreen(
                    onClose = { navController.popBackStack() },
                    onApplied = { navController.popBackStack() },
                    onLocationClick = { current ->
                        entry.savedStateHandle[KEY_DISTRICT_CURRENT] = current.orEmpty()
                        navController.navigate(Routes.LOCATION_SEARCH)
                    },
                    viewModel = filterViewModel,
                )
            }
            composable(Routes.LOCATION_SEARCH) {
                val filterEntry = navController.previousBackStackEntry
                LocationSearchScreen(
                    initialDistrict = filterEntry?.savedStateHandle?.get<String>(KEY_DISTRICT_CURRENT)?.takeIf { it.isNotBlank() },
                    onBack = { navController.popBackStack() },
                    onApplied = { district ->
                        filterEntry?.savedStateHandle?.set(KEY_DISTRICT_RESULT, district.orEmpty())
                        navController.popBackStack()
                    },
                )
            }
            composable(Routes.NOTIFICATION) {
                NotificationScreen(
                    onBack = { navController.popBackStack() },
                    onOpen = { navController.navigate(it) },
                )
            }

            // 5~6 모임 상세 · 참가 신청
            composable(Routes.MEETING_DETAIL, meetingArgs()) { entry ->
                val meetingId = entry.arguments?.getString(Routes.ARG_MEETING_ID).orEmpty()
                val applied by entry.savedStateHandle.getStateFlow(KEY_APPLIED, false).collectAsState()
                MeetingDetailScreen(
                    meetingId = meetingId,
                    justApplied = applied,
                    onAppliedHandled = { entry.savedStateHandle[KEY_APPLIED] = false },
                    onBack = { navController.popBackStack() },
                    onApply = { navController.navigate(Routes.apply(meetingId)) },
                    onReview = { navController.navigate(Routes.REVIEW_LIST) },
                    // TODO(임시) 지금은 회원가입용 종목 기록 화면을 연다. 프로필의 기록 수정 흐름이 생기면 그쪽으로 바꾼다.
                    onAddRecord = { sport -> navController.navigate(Routes.record(sport)) },
                )
            }
            composable(Routes.APPLY, meetingArgs()) { entry ->
                val meetingId = entry.arguments?.getString(Routes.ARG_MEETING_ID).orEmpty()
                ApplyScreen(
                    meetingId = meetingId,
                    onClose = { navController.popBackStack() },
                    onApplied = {
                        // 모임 상세가 신청 결과를 다시 불러오도록 알리고 돌아간다.
                        navController.previousBackStackEntry?.savedStateHandle?.set(KEY_APPLIED, true)
                        navController.popBackStack()
                    },
                    // TODO(임시) 지금은 회원가입용 종목 기록 화면을 연다. 프로필의 기록 수정 흐름이 생기면 그쪽으로 바꾼다.
                    onEditRecord = { sport -> navController.navigate(Routes.record(sport)) },
                )
            }

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
                    viewModel = viewModel { MeetingCreateViewModel(userRepository, meetingRepository) },
                    onClose = { navController.popBackStack() },
                    onPlaceClick = { navController.navigate(Routes.PLACE_SEARCH) },
                    onCreated = { meetingId ->
                        navController.navigate(Routes.meetingDetail(meetingId)) {
                            popUpTo(Routes.MEETING_CREATE) { inclusive = true }
                        }
                    },
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
                    viewModel = viewModel { ActivityViewModel(meetingRepository, userRepository) },
                    onReviewClick = { navController.navigate(Routes.REVIEW_LIST) }, // TODO: meetingId 넘기기 (12번 route 합의 후)
                    onMeetingClick = { meetingId -> navController.navigate(Routes.meetingDetail(meetingId)) },
                    onCreateClick = { navController.navigate(Routes.MEETING_CREATE) },
                    onExploreClick = { navController.navigate(Routes.EXPLORE) },
                )
            }
            composable(Routes.MYPAGE) {
                MyPageScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    },
                )
            }
        }
    }
}

private fun NavBackStackEntry.meetingId(): String =
    arguments?.getString(Routes.ARG_MEETING_ID).orEmpty()

private fun meetingArgs() = listOf(navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType })
