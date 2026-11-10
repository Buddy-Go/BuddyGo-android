package com.example.myapplication.ui.navigation

import androidx.compose.runtime.produceState
import com.example.myapplication.ui.mypage.BlockManageScreen
import com.example.myapplication.ui.auth.FindPasswordScreen
import com.example.myapplication.data.local.ActivityAreaStore
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.myapplication.data.api.Api
import com.example.myapplication.data.local.SessionEvents
import com.example.myapplication.data.local.SessionReset
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.repository.Repositories
import com.example.myapplication.ui.activity.ActivityScreen
import com.example.myapplication.ui.activity.ActivityViewModel
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.auth.LoginViewModel
import com.example.myapplication.ui.auth.message
import com.example.myapplication.ui.common.LocalGoHome
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
import com.example.myapplication.ui.mate.review.MemberReviewScreen
import com.example.myapplication.ui.mate.review.ReviewDoneScreen
import com.example.myapplication.ui.mate.review.ReviewListScreen
import com.example.myapplication.ui.mate.review.ReviewViewModel
import com.example.myapplication.ui.meeting.create.MeetingCreateScreen
import com.example.myapplication.ui.meeting.create.MeetingCreateViewModel
import com.example.myapplication.ui.meeting.create.PlaceConfirmScreen
import com.example.myapplication.ui.meeting.create.PlaceSearchScreen
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
import com.example.myapplication.ui.mypage.AllReviewsScreen
import com.example.myapplication.ui.mypage.AllReviewsViewModel
import com.example.myapplication.ui.mypage.MyPageScreen
import com.example.myapplication.ui.notification.NotificationScreen
import com.example.myapplication.ui.record.RecordScreen
import com.example.myapplication.ui.record.RecordViewModel
import com.example.myapplication.ui.record.SignUpInfoScreen
import com.example.myapplication.ui.record.SignUpProfileScreen

// 위치 검색(4g)과 조건 설정 사이에 구·동을 주고받는 키. 결과가 없을 때는 NO_RESULT, 해제는 빈 문자열이다.
private const val KEY_DISTRICT_CURRENT = "district_current"
private const val KEY_DISTRICT_RESULT = "district_result"
private const val NO_RESULT = "\u0000none"
// 신청 화면(6)이 끝나면 모임 상세에 알리는 키
private const val KEY_APPLIED = "applied"


// 이름표(Routes)와 실제 화면을 연결한다.
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // 저장소는 Repositories에서 하나씩 같이 쓴다. (화면마다 새로 만들면 수락·공지 작성 결과가 다른 화면에 안 보인다)
    val meetingRepository = Repositories.meetings
    val userRepository = Repositories.users
    val noticeRepository = Repositories.notices
    val mateRepository = Repositories.mates
    val reviewRepository = Repositories.reviews

    // 앱을 켠 순간의 로그인 여부로 첫 화면을 정한다. (이후 로그인·로그아웃은 화면 이동으로 처리)
    val startRoute = remember { if (Api.tokens.hasSession) Routes.EXPLORE else Routes.LOGIN }

    // 로그인이 풀렸을 때(리프레시 토큰 만료·재사용) 이전 사용자의 흔적을 지우고 로그인 화면으로 돌려보낸다.
    LaunchedEffect(Unit) {
        SessionEvents.expired.collect {
            SessionReset.onLogout()
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    // 탭으로 옮기는 방법은 하나로 맞춘다. 같은 탭 화면이 겹겹이 쌓이지 않고, 탭의 상태가 남는다.
    val goTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.EXPLORE) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // 탭 5개 화면에서만 하단 탭 바를 보여 준다.
    Scaffold(
        modifier = modifier,
        bottomBar = {
            // 개설(10a)은 디자인이 탭 바 없는 전체 화면이라 개설 화면에서는 탭 바를 숨긴다. X로 닫으면 이전 화면으로 돌아간다.
            if (currentRoute in Routes.bottomTabs && currentRoute != Routes.MEETING_CREATE) {
                // 받은 메이트 신청이 있으면 메이트 탭에 점을 보인다. 화면을 옮길 때마다 새로 센다.
                val mateBadge by produceState(false, currentRoute) {
                    value = mateRepository.getRequests().getOrNull()?.isNotEmpty() == true
                }
                BottomNavBar(
                    currentRoute = currentRoute,
                    mateBadge = mateBadge,
                    onTabClick = goTab,
                )
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(LocalGoHome provides { goTab(Routes.EXPLORE) }) {
            NavHost(
                navController = navController,
                // 로그인 상태 유지로 저장된 로그인이 있으면 로그인 화면을 건너뛴다.
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding),
            ) {
                // 1~2 로그인 · 회원가입 · 종목 기록
                composable(Routes.LOGIN) {
                    val loginViewModel: LoginViewModel = viewModel()
                    val loginState by loginViewModel.state.collectAsState()
                    LoginScreen(
                        onLoginClick = { email, password, keepLogin ->
                            loginViewModel.onLogin(email, password, keepLogin) {
                                navController.navigate(Routes.EXPLORE) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                        },
                        submitting = loginState.submitting,
                        errorMessage = loginState.error?.message(),
                        onInputChange = loginViewModel::onInputChanged,
                        onSignUpClick = { navController.navigate(Routes.SIGN_UP_INFO) },
                        onFindPasswordClick = { navController.navigate(Routes.FIND_PASSWORD) },
                    )
                }
                composable(Routes.FIND_PASSWORD) {
                    FindPasswordScreen(onBack = { navController.popBackStack() })
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

                // 프로필 기록 수정: 모임 신청·상세의 '기록 추가'·'기준 줄'에서 들어온다. 저장하면 내 프로필 기록이 바뀐다.
                composable(
                    Routes.PROFILE_RECORD,
                    listOf(navArgument(Routes.ARG_SPORT) { type = NavType.StringType }),
                ) { entry ->
                    val sport = Sport.valueOf(entry.arguments?.getString(Routes.ARG_SPORT) ?: Sport.HEALTH.name)
                    RecordScreen(
                        sport = sport,
                        onBack = { navController.popBackStack() },
                        profileEdit = true,
                        viewModel = viewModel(key = "profile-record-${sport.name}") { RecordViewModel(sport, userRepository) },
                    )
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
                        onReview = { navController.navigate(Routes.reviewList(meetingId)) },
                        onManageParticipants = { navController.navigate(Routes.participantManage(meetingId)) },
                        onWriteNotice = { navController.navigate(Routes.noticeEdit(meetingId)) },
                        onOpenNotices = { navController.navigate(Routes.noticeList(meetingId)) },
                        onOpenNotice = { noticeId -> navController.navigate(Routes.noticeDetail(meetingId, noticeId)) },
                        onEditMeeting = { navController.navigate(Routes.meetingEdit(meetingId)) },
                        onAddRecord = { sport -> navController.navigate(Routes.profileRecord(sport)) },
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
                        onEditRecord = { sport -> navController.navigate(Routes.profileRecord(sport)) },
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
                composable(Routes.MEETING_EDIT, meetingArgs()) { entry ->
                    val meetingId = entry.meetingId()
                    MeetingCreateScreen(
                        viewModel = viewModel(key = "edit-$meetingId") { MeetingCreateViewModel(userRepository, meetingRepository, editMeetingId = meetingId) },
                        onClose = { navController.popBackStack() },
                        onPlaceClick = {},
                        onCreated = {},
                        onUpdated = { navController.popBackStack() },
                    )
                }
                composable(Routes.PLACE_SEARCH) {
                    PlaceSearchScreen(
                        onBack = { navController.popBackStack() },
                        onSelected = { navController.navigate(Routes.PLACE_CONFIRM) },
                    )
                }
                composable(Routes.PLACE_CONFIRM) {
                    PlaceConfirmScreen(
                        onBack = { navController.popBackStack() },
                        // 설정하면 장소 검색까지 닫고 개설 화면으로 돌아간다.
                        onConfirmed = { navController.popBackStack(Routes.MEETING_CREATE, inclusive = false) },
                    )
                }

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
                        onAllReviewsClick = { navController.navigate(Routes.mateReviews(userId)) },
                    )
                }

                // 12 메이트 평가: 12a·12b·12c가 한 모임의 평가 흐름이라 같은 ViewModel을 나눠 쓴다. (12a 화면이 주인)
                composable(Routes.REVIEW_LIST, meetingArgs()) { entry ->
                    val meetingId = entry.meetingId()
                    ReviewListScreen(
                        viewModel = viewModel(key = "review-$meetingId") { ReviewViewModel(meetingId, reviewRepository, mateRepository) },
                        onBack = { navController.popBackStack() },
                        onStartReview = { navController.navigate(Routes.memberReview(meetingId, "next")) },
                    )
                }
                composable(
                    Routes.MEMBER_REVIEW,
                    listOf(
                        navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType },
                        navArgument(Routes.ARG_USER_ID) { type = NavType.StringType },
                    ),
                ) { entry ->
                    val parent = remember(entry) { navController.getBackStackEntry(Routes.REVIEW_LIST) }
                    MemberReviewScreen(
                        viewModel = viewModel<ReviewViewModel>(parent, key = "review-${entry.meetingId()}"),
                        onClose = { navController.popBackStack() },
                        onAllDone = {
                            navController.navigate(Routes.REVIEW_DONE) { popUpTo(Routes.MEMBER_REVIEW) { inclusive = true } }
                        },
                    )
                }
                composable(Routes.REVIEW_DONE) { entry ->
                    val parent = remember(entry) { navController.getBackStackEntry(Routes.REVIEW_LIST) }
                    ReviewDoneScreen(
                        viewModel = viewModel<ReviewViewModel>(parent, key = "review-${parent.meetingId()}"),
                        onGoActivity = {
                            navController.navigate(Routes.ACTIVITY) {
                                popUpTo(Routes.REVIEW_LIST) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }

                // 13~14 활동 내역 · 마이페이지
                composable(Routes.ACTIVITY) {
                    ActivityScreen(
                        viewModel = viewModel { ActivityViewModel(meetingRepository, userRepository, reviewRepository) },
                        onReviewClick = { meetingId -> navController.navigate(Routes.reviewList(meetingId)) },
                        onMeetingClick = { meetingId -> navController.navigate(Routes.meetingDetail(meetingId)) },
                        onCreateClick = { goTab(Routes.MEETING_CREATE) },
                        onExploreClick = { goTab(Routes.EXPLORE) },
                    )
                }
                composable(Routes.MY_REVIEWS) {
                    val vm = viewModel<AllReviewsViewModel>(key = "my-reviews") { AllReviewsViewModel(users = userRepository) }
                    val state by vm.state.collectAsState()
                    AllReviewsScreen("받은 후기", state, onBack = { navController.popBackStack() }, onRetry = vm::load)
                }
                composable(
                    Routes.MATE_REVIEWS,
                    listOf(navArgument(Routes.ARG_USER_ID) { type = NavType.StringType }),
                ) { entry ->
                    val userId = entry.arguments?.getString(Routes.ARG_USER_ID).orEmpty()
                    val vm = viewModel<AllReviewsViewModel>(key = "mate-reviews-$userId") { AllReviewsViewModel(userId, userRepository, mateRepository) }
                    val state by vm.state.collectAsState()
                    AllReviewsScreen("받은 후기", state, onBack = { navController.popBackStack() }, onRetry = vm::load)
                }
                composable(Routes.ACTIVITY_AREA) {
                    // 활동 지역은 구·동 하나. 위치 검색(4g)을 그대로 쓰고, 적용하면 저장한다.
                    val area by ActivityAreaStore.area.collectAsState()
                    LocationSearchScreen(
                        initialDistrict = area,
                        onBack = { navController.popBackStack() },
                        onApplied = { district ->
                            ActivityAreaStore.set(district)
                            navController.popBackStack()
                        },
                    )
                }
                composable(Routes.BLOCK_MANAGE) {
                    BlockManageScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.MYPAGE) {
                    MyPageScreen(
                        onAllReviewsClick = { navController.navigate(Routes.MY_REVIEWS) },
                        onLocationClick = { navController.navigate(Routes.ACTIVITY_AREA) },
                        onBlockManageClick = { navController.navigate(Routes.BLOCK_MANAGE) },
                        onEditRecord = { sport -> navController.navigate(Routes.profileRecord(sport)) },
                        onLogout = {
                            SessionReset.onLogout()
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        },
                    )
                }
            }
        }
    }
}

private fun NavBackStackEntry.meetingId(): String =
    arguments?.getString(Routes.ARG_MEETING_ID).orEmpty()

private fun meetingArgs() = listOf(navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType })
