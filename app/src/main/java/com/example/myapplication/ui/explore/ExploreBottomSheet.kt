package com.example.myapplication.ui.explore

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.common.SkeletonList
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace

// 화면 아래에 붙는 모임 목록 시트.
// 3-After 올림 / 3-2-After 내림(헤더만) / 3-5 핀을 고르면 그 지번의 모임만.
// 모달이 아니라서 시트 뒤의 지도도 계속 움직일 수 있다.
@Composable
fun ExploreBottomSheet(
    state: ExploreState,
    expandedHeight: Dp,
    onSetExpanded: (Boolean) -> Unit,
    onMeetingClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
    collapsedHeight: Dp = CollapsedSheetHeight,
) {
    val height by animateDpAsState(if (state.sheetExpanded) expandedHeight else collapsedHeight, label = "sheetHeight")
    var dragged by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { dragged += it },
                onDragStopped = {
                    // 위로 끌면 올리고 아래로 끌면 내린다. 조금만 움직인 건 무시한다.
                    if (dragged < -DragThresholdPx) onSetExpanded(true)
                    else if (dragged > DragThresholdPx) onSetExpanded(false)
                    dragged = 0f
                },
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = AppColor.Surface,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth()
                    .clickable(onClickLabel = if (state.sheetExpanded) "목록 내리기" else "목록 올리기", role = Role.Button) { onSetExpanded(!state.sheetExpanded) }
                    .padding(vertical = AppSpace.xl),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(AppColor.Border))
            }
            SheetHeader(state)
            if (state.sheetExpanded) {
                SheetBody(state, onMeetingClick, onToggleFavorite)
            }
        }
    }
}

@Composable
private fun SheetHeader(state: ExploreState) {
    val place = state.sheetTitlePlace
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AppSpace.xxl).padding(bottom = AppSpace.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = place ?: "이 지역 모임 ${state.visibleMeetings.size}",
                color = AppColor.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
            if (place != null) {
                Text(
                    text = state.selectedPin?.meetings?.first()?.place?.address.orEmpty(),
                    color = AppColor.TextSecondary, fontSize = 14.sp,
                )
            }
        }
        Text(
            text = if (place != null) "모임 ${state.sheetMeetings.size}" else "거리순 ▾",
            color = AppColor.TextSecondary, fontSize = 15.sp,
        )
    }
}

@Composable
private fun SheetBody(
    state: ExploreState,
    onMeetingClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    when (state.meetings) {
        // 16e 부분 로딩: 지도는 그대로 두고 목록 자리만 스켈레톤으로 채운다.
        UiState.Loading -> SkeletonList()
        is UiState.Error -> Unit // 오류는 화면 전체를 덮는 FullScreenError가 맡는다.
        is UiState.Success -> {
            val meetings = state.sheetMeetings
            if (meetings.isEmpty()) {
                EmptyList(state.favoriteOnly)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = AppSpace.xxl, vertical = AppSpace.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpace.xl),
                ) {
                    items(meetings, key = { it.id }) { meeting ->
                        MeetingCard(
                            meeting = meeting,
                            onClick = { onMeetingClick(meeting.id) },
                            onToggleFavorite = { onToggleFavorite(meeting.id) },
                            showPlaceName = state.sheetTitlePlace == null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyList(favoriteOnly: Boolean) {
    Box(Modifier.fillMaxWidth().padding(vertical = AppSpace.x5 * 2), contentAlignment = Alignment.Center) {
        Text(
            text = if (favoriteOnly) "찜한 모임이 없어요" else "이 지역에 모집 중인 모임이 없어요",
            color = AppColor.TextSecondary, fontSize = 15.sp,
        )
    }
}

val CollapsedSheetHeight = 88.dp
private const val DragThresholdPx = 40f
