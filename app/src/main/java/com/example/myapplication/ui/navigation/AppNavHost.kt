package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.common.BottomNavBar
import com.example.myapplication.ui.explore.ExploreScreen
import com.example.myapplication.ui.explore.filter.FilterScreen
import com.example.myapplication.ui.explore.filter.FilterViewModel
import com.example.myapplication.ui.explore.filter.LocationSearchScreen
import com.example.myapplication.ui.meeting.detail.ApplyScreen
import com.example.myapplication.ui.meeting.detail.MeetingDetailScreen
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
            composable(Routes.PARTICIPANT_MANAGE, meetingArgs()) { TempScreen("7 참가자 관리") }

            // 8 공지
            composable(Routes.NOTICE_LIST, meetingArgs()) { TempScreen("8d~8e 공지 목록") }
            composable(
                Routes.NOTICE_DETAIL,
                listOf(
                    navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType },
                    navArgument(Routes.ARG_NOTICE_ID) { type = NavType.StringType },
                ),
            ) { TempScreen("8f~8i 공지 상세") }
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
            ) { TempScreen("8a~8c 공지 작성·수정") }

            // 10 모임 개설
            composable(Routes.MEETING_CREATE) { TempScreen("10 모임 개설") }
            composable(Routes.PLACE_SEARCH) { TempScreen("10g 장소 검색") }
            composable(Routes.PLACE_CONFIRM) { TempScreen("10g-3~4 장소 확인·집결지") }

            // 11, 15 메이트
            composable(Routes.MATE) { TempScreen("11 메이트") }
            composable(Routes.MATE_REQUEST) { TempScreen("11c 메이트 신청") }
            composable(
                Routes.MATE_PROFILE,
                listOf(navArgument(Routes.ARG_USER_ID) { type = NavType.StringType }),
            ) { TempScreen("15 메이트 프로필") }

            // 12 메이트 평가
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
            composable(Routes.ACTIVITY) { TempScreen("13 활동 내역") }
            composable(Routes.MYPAGE) { TempScreen("14 마이페이지") }
        }
    }
}

private fun meetingArgs() = listOf(navArgument(Routes.ARG_MEETING_ID) { type = NavType.StringType })
