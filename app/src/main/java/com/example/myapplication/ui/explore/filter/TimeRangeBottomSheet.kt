package com.example.myapplication.ui.explore.filter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

private val HOURS = (0..23).toList()
private val MINUTES = (0 until 60 step TimeOfDayRange.STEP_MINUTE).toList()
private val ItemHeight = 44.dp
private const val VisibleItems = 5

// 4f 시간 범위 시트 (조건 설정 위에 뜬다): 모임 시작 시각을 "부터 ~ 까지"로 고른다.
// '확인'은 닫고, 밖을 누르거나 내리면 열기 전 값으로 돌아간다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeBottomSheet(
    range: TimeOfDayRange,
    onSetStart: (Int) -> Unit,
    onSetEnd: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.Surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = AppSpace.x4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("시간 범위 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
                Text(
                    "${formatMinuteOfDay(range.startMinute)} – ${formatMinuteOfDay(range.endMinute)}",
                    fontSize = 18.sp, color = AppColor.TextSecondary,
                )
            }
            Text("모임 시작 시간 기준이에요", fontSize = 14.sp, color = AppColor.TextSecondary, modifier = Modifier.padding(top = AppSpace.md))

            Row(Modifier.fillMaxWidth().padding(top = AppSpace.xxl)) {
                Text("부터", fontSize = 15.sp, color = AppColor.TextSecondary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Box(Modifier.width(48.dp))
                Text("까지", fontSize = 15.sp, color = AppColor.TextSecondary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().height(ItemHeight * VisibleItems)) {
                // 가운데 줄 두 개: 고른 값이 이 사이에 놓인다.
                Column(Modifier.align(Alignment.Center)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
                    Box(Modifier.fillMaxWidth().height(ItemHeight - 2.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    WheelPicker(HOURS, range.startMinute / 60, onSelected = { onSetStart(it * 60 + range.startMinute % 60) }, modifier = Modifier.weight(1f))
                    Text(":", fontSize = 22.sp, color = AppColor.TextPrimary)
                    WheelPicker(MINUTES, (range.startMinute % 60) / TimeOfDayRange.STEP_MINUTE, onSelected = { onSetStart(range.startMinute / 60 * 60 + it * TimeOfDayRange.STEP_MINUTE) }, modifier = Modifier.weight(1f))
                    Text("~", fontSize = 20.sp, color = AppColor.TextSecondary, modifier = Modifier.width(48.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    WheelPicker(HOURS, range.endMinute / 60, onSelected = { onSetEnd(it * 60 + range.endMinute % 60) }, modifier = Modifier.weight(1f))
                    Text(":", fontSize = 22.sp, color = AppColor.TextPrimary)
                    WheelPicker(MINUTES, (range.endMinute % 60) / TimeOfDayRange.STEP_MINUTE, onSelected = { onSetEnd(range.endMinute / 60 * 60 + it * TimeOfDayRange.STEP_MINUTE) }, modifier = Modifier.weight(1f))
                }
            }
            ActionButton(text = "확인", onClick = onConfirm, modifier = Modifier.padding(top = AppSpace.xl, bottom = AppSpace.x4))
        }
    }
}

// 위아래로 밀어 값을 고르는 휠. 가운데 칸이 선택값이고, 손을 떼면 가장 가까운 값에 붙는다.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(values: List<Int>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val padding = ItemHeight * (VisibleItems / 2)
    // 화면 가운데에 가장 가까운 칸
    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            // 칸의 offset은 위쪽 여백을 뺀 곳부터 잰다. 여백을 뺀 영역의 가운데에 가장 가까운 칸이 선택값이다.
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
        }
    }
    // 손을 떼어 멈췄을 때만 값을 알린다.
    LaunchedEffect(listState) {
        snapshotFlow { if (listState.isScrollInProgress) null else centerIndex }
            .filterNotNull().distinctUntilChanged().collect { if (it != selectedIndex) onSelected(it) }
    }
    // 바깥에서 값이 바뀌면(시작이 끝을 넘어 끝도 따라 움직이는 경우) 그 값으로 옮긴다.
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress && centerIndex != selectedIndex) listState.animateScrollToItem(selectedIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.height(ItemHeight * VisibleItems),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = padding),
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(values) { index, value ->
            val selected = index == (centerIndex ?: selectedIndex)
            Box(Modifier.height(ItemHeight), contentAlignment = Alignment.Center) {
                Text(
                    "%02d".format(value),
                    fontSize = if (selected) 30.sp else 22.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) AppColor.TextPrimary else AppColor.TextDisabled,
                )
            }
        }
    }
}
