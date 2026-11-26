package com.example.myapplication.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 키보드가 올라올 때 하단 버튼은 그대로 두고(키보드가 덮는다) 위쪽 스크롤 영역만 키보드 위로 줄이는 데 쓴다.
 *
 * 여러 칸을 채우는 긴 화면(모임 개설, 회원가입 정보, 신청, 후기)은 버튼이 입력 중인 칸과 바로 이어지지 않아
 * 버튼을 키보드와 같이 올리면 입력할 자리만 좁아진다. 반대로 댓글 입력줄, 바텀시트, 한 칸짜리 입력처럼
 * 입력하고 바로 누르는 버튼은 지금처럼 화면 전체에 imePadding()을 줘서 키보드 위로 같이 올린다.
 *
 * 쓰는 법: 스크롤 영역에 [keyboardOverBottomBar], 하단 버튼 묶음에 [measureBottomBar]를 붙인다.
 * 화면 바깥 Column에는 imePadding()을 주지 않는다.
 */
class BottomBarHeight internal constructor(internal val state: MutableState<Dp>)

@Composable
fun rememberBottomBarHeight(): BottomBarHeight = remember { BottomBarHeight(mutableStateOf(0.dp)) }

/** 하단 버튼 묶음의 높이를 잰다. 키보드가 이 높이만큼은 이미 버튼을 덮고 있으니 스크롤 영역 여백에서 뺀다. */
@Composable
fun Modifier.measureBottomBar(height: BottomBarHeight): Modifier {
    val density = LocalDensity.current
    return onSizeChanged { height.state.value = with(density) { it.height.toDp() } }
}

/** 스크롤 영역을 키보드 바로 위까지만 보이게 줄인다. 하단 버튼 높이만큼은 빼서 키보드 위에 빈 띠가 생기지 않게 한다. */
fun Modifier.keyboardOverBottomBar(height: BottomBarHeight): Modifier =
    consumeWindowInsets(PaddingValues(bottom = height.state.value)).imePadding()
