package com.example.myapplication.ui.meeting.notice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.Notice
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.common.ErrorType
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 공지 목록 화면 ← 8d 호스트, 8e 참가자
// ViewModel은 화면을 띄우는 쪽(AppNavHost)에서 meetingId로 만들어 넘겨준다.
// 작성·수정 화면에서 돌아오면 viewModel.refresh()를 불러 목록을 새로 받는다.
@Composable
fun NoticeListScreen(
    viewModel: NoticeListViewModel,
    onBack: () -> Unit,
    onNoticeClick: (noticeId: String) -> Unit, // 8f·8g 상세로
    onWriteClick: () -> Unit, // 8a 작성으로
    onEditClick: (noticeId: String) -> Unit, // 8c 수정으로
) {
    // 작성·수정 화면에서 돌아왔을 때 목록을 새로 받는다.
    LaunchedEffect(Unit) { viewModel.refresh() }
    NoticeListContent(
        state = viewModel.state,
        onBack = onBack,
        onNoticeClick = onNoticeClick,
        onWriteClick = onWriteClick,
        onEditClick = onEditClick,
        onDeleteClick = viewModel::onDelete,
        onRetry = viewModel::refresh,
    )
}

@Composable
private fun NoticeListContent(
    state: NoticeListState,
    onBack: () -> Unit,
    onNoticeClick: (String) -> Unit,
    onWriteClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onRetry: () -> Unit = {},
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        NoticeTopBar(
            leadingIcon = R.drawable.ic_arrow_back,
            leadingDescription = "뒤로 가기",
            title = "공지",
            onLeadingClick = onBack,
        )

        if (state.loadFailed) {
            FullScreenError(type = ErrorType.SERVER, onRetry = onRetry, modifier = Modifier.weight(1f))
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xs, bottom = AppSpace.md),
        ) {
            if (!state.isLoading && state.notices.isEmpty()) {
                // 빈 목록 프레임은 디자인에 없음. 문구는 임시.
                Text(
                    text = "아직 올라온 공지가 없어요",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColor.TextSecondary,
                    modifier = Modifier.padding(top = AppSpace.x5),
                )
            }
            state.notices.forEachIndexed { index, notice ->
                if (index > 0) NoticeDivider()
                NoticeItem(
                    notice = notice,
                    timeText = noticeTimeText(notice.createdAtMillis, timeZone),
                    showMenu = state.canManage,
                    onClick = { onNoticeClick(notice.id) },
                    onEdit = { onEditClick(notice.id) },
                    onDelete = { onDeleteClick(notice.id) },
                )
            }
        }

        if (state.canManage) {
            WriteButtonBar(onClick = onWriteClick)
        }
    }
}

@Composable
private fun NoticeItem(
    notice: Notice,
    timeText: String,
    showMenu: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpace.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            if (showMenu) {
                MoreMenuButton(
                    items = listOf(
                        MoreMenuItem("수정", onClick = onEdit),
                        MoreMenuItem("삭제", isDestructive = true, onClick = onDelete),
                    ),
                    iconSize = 18.dp,
                    tint = AppColor.TextBody,
                    description = "공지 더보기",
                )
            }
        }
        Text(text = notice.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Text(
            text = notice.body,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            color = AppColor.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (notice.commentCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.xs),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_comment),
                    contentDescription = "댓글",
                    tint = AppColor.TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = notice.commentCount.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextSecondary,
                )
            }
        }
    }
}

// 8d 아래 '+ 공지 작성' (호스트만)
@Composable
private fun WriteButtonBar(onClick: () -> Unit) {
    Column(modifier = Modifier.background(AppColor.Surface)) {
        NoticeDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xl, bottom = AppSpace.xxl)
                .height(54.dp)
                .clip(RoundedCornerShape(AppRadius.md))
                .background(AppColor.Accent)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(AppSpace.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = AppColor.OnAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(text = "공지 작성", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.OnAccent)
        }
    }
}

// ---------- 미리보기 ----------

private val previewTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

// 2026-09-07 21:30, 2026-09-05 14:10 (서울)
internal val previewNotices = listOf(
    Notice("n1", "m1", "집합 장소 변경", "비 예보가 있어서 정문 대신 후문 3층 프리웨이트 존에서 만나요.", 1788784200000L, isEdited = true, commentCount = 3),
    Notice("n2", "m1", "준비물 안내", "스트랩이랑 개인 수건 꼭 챙겨주세요. 물은 1L 이상 챙기시면 좋습니다.", 1788585000000L),
)

@Composable
private fun PreviewContent(isHost: Boolean) {
    NoticeListContent(
        state = NoticeListState(notices = previewNotices, isHost = isHost, isLoading = false),
        onBack = {},
        onNoticeClick = {},
        onWriteClick = {},
        onEditClick = {},
        onDeleteClick = {},
        timeZone = previewTimeZone,
    )
}

@Preview(name = "8d 공지 목록 · 호스트", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8d() = PreviewContent(isHost = true)

@Preview(name = "8e 공지 목록 · 참가자", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8e() = PreviewContent(isHost = false)
