package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.common.BottomNavBar
import com.example.myapplication.ui.record.SignUpInfoScreen
import com.example.myapplication.ui.record.SignUpProfileScreen

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
            ) { TempScreen("2c~2e 종목 기록") }

            // 3~4 탐색 · 조건 설정 · 알림
            composable(Routes.EXPLORE) { TempScreen("3 탐색") }
            composable(Routes.FILTER) { TempScreen("4a~4c 조건 설정") }
            composable(Routes.LOCATION_SEARCH) { TempScreen("4g 위치 검색") }
            composable(Routes.NOTIFICATION) { TempScreen("3-6 알림 목록") }

            // 5~6 모임 상세 · 참가 신청
            composable(Routes.MEETING_DETAIL, meetingArgs()) { TempScreen("5 모임 상세") }
            composable(Routes.APPLY, meetingArgs()) { TempScreen("6 참가 신청") }

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
