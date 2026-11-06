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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.Comment
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 공지 상세 화면 ← 8f 호스트, 8g 참가자, 8h 댓글 입력 중, 8i 댓글 없음
// ViewModel은 화면을 띄우는 쪽(AppNavHost)에서 noticeId로 만들어 넘겨준다.
@Composable
fun NoticeDetailScreen(
    viewModel: NoticeDetailViewModel,
    onBack: () -> Unit,
    onEditClick: (noticeId: String) -> Unit, // 8c 수정으로
    onDeleted: () -> Unit, // 공지를 지운 뒤 목록으로
) {
    val state = viewModel.state
    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onDeleted()
    }
    NoticeDetailContent(
        state = state,
        onBack = onBack,
        onEditClick = { state.notice?.let { onEditClick(it.id) } },
        onDeleteNotice = viewModel::onDeleteNotice,
        onCommentInputChange = viewModel::onCommentInputChange,
        onSend = viewModel::onSend,
        onDeleteComment = viewModel::onDeleteComment,
    )
}

@Composable
private fun NoticeDetailContent(
    state: NoticeDetailState,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteNotice: () -> Unit,
    onCommentInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onDeleteComment: (String) -> Unit,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        NoticeTopBar(
            leadingIcon = R.drawable.ic_arrow_back,
            leadingDescription = "뒤로 가기",
            title = "공지",
            onLeadingClick = onBack,
        ) {
            if (state.canManageNotice) {
                MoreMenuButton(
                    items = listOf(
                        MoreMenuItem("수정", onClick = onEditClick),
                        MoreMenuItem("삭제", isDestructive = true, onClick = onDeleteNotice),
                    ),
                    iconSize = 22.dp,
                    tint = AppColor.TextPrimary,
                    description = "공지 더보기",
                )
            }
        }

        val notice = state.notice
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xs, bottom = AppSpace.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
        ) {
            if (notice != null) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
                    Text(
                        text = notice.metaText(timeZone),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColor.TextSecondary,
                    )
                    Text(text = notice.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    Text(
                        text = notice.body,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColor.TextPrimary,
                    )
                }
                NoticeDivider()
                Text(text = state.commentHeader, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)

                if (state.comments.isEmpty()) {
                    // 8i
                    Text(
                        text = "궁금한 점이 있으면 댓글로 남겨주세요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColor.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppSpace.x5),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
                        state.comments.forEach { comment ->
                            CommentRow(
                                comment = comment,
                                timeText = noticeTimeText(comment.createdAtMillis, timeZone),
                                showMenu = state.isMine(comment),
                                onDelete = { onDeleteComment(comment.id) },
                            )
                        }
                    }
                }
            }
        }

        if (state.canWriteComment) {
            CommentInputBar(
                value = state.commentInput,
                canSend = state.canSend,
                onValueChange = onCommentInputChange,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, timeText: String, showMenu: Boolean, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
        // 호스트는 초록 원, 나머지는 회색 원 (8f)
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (comment.isAuthorHost) AppColor.HostAvatarBg else AppColor.NeutralSoft,
                    RoundedCornerShape(AppRadius.pill),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = comment.authorNickname.take(1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (comment.isAuthorHost) AppColor.HostAvatarText else AppColor.AccentStrong,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxs),
        ) {
            Row(
                modifier = Modifier.height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.sm),
            ) {
                Text(text = comment.authorNickname, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                if (comment.isAuthorHost) HostBadge()
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (showMenu) {
                    // 댓글 삭제는 정의서에 없음. 디자인의 ⋯만 보고 '삭제'를 둠 (팀에 질문 대상)
                    MoreMenuButton(
                        items = listOf(MoreMenuItem("삭제", isDestructive = true, onClick = onDelete)),
                        iconSize = 18.dp,
                        tint = AppColor.TextBody,
                        description = "댓글 더보기",
                    )
                }
            }
            Text(
                text = comment.body,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextPrimary,
            )
        }
    }
}

@Composable
private fun HostBadge() {
    Box(
        modifier = Modifier
            .height(18.dp)
            .background(AppColor.AccentSoft, RoundedCornerShape(AppRadius.xs))
            .padding(horizontal = AppSpace.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "호스트", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColor.AccentStrong)
    }
}

// 8f 아래 댓글 입력 (8h: 입력이 있으면 전송 버튼이 초록)
@Composable
private fun CommentInputBar(
    value: String,
    canSend: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
    Column(modifier = Modifier.background(AppColor.Surface)) {
        NoticeDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpace.xxl, vertical = AppSpace.lg)
                .height(46.dp)
                .clip(RoundedCornerShape(AppRadius.pill))
                .background(AppColor.ChipNeutralBg)
                .padding(start = AppSpace.xxl, end = AppSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(AppColor.AccentStrong),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(text = "댓글을 입력해주세요", style = textStyle.copy(color = AppColor.TextDisabled))
                        }
                        inner()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(if (canSend) AppColor.Accent else AppColor.NeutralSoft)
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "댓글 보내기",
                    tint = if (canSend) AppColor.OnAccent else AppColor.IconMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ---------- 미리보기 ----------

private val previewTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

// 9/7 21:42, 21:50, 22:05 (서울)
private val previewComments = listOf(
    Comment("c1", "n1", "u2", "서연", "후문 3층 맞죠? 몇 시까지 가면 될까요?", 1788784920000L),
    Comment("c2", "n1", "u1", "민준", "네 6시 50분까지 와주시면 돼요", 1788785400000L, isAuthorHost = true),
    Comment("c3", "n1", "u3", "지훈", "확인했어요!", 1788786300000L),
)

@Composable
private fun PreviewContent(state: NoticeDetailState) {
    NoticeDetailContent(
        state = state,
        onBack = {},
        onEditClick = {},
        onDeleteNotice = {},
        onCommentInputChange = {},
        onSend = {},
        onDeleteComment = {},
        timeZone = previewTimeZone,
    )
}

private val previewHost = NoticeDetailState(
    notice = previewNotices[0],
    comments = previewComments,
    myUserId = "u1",
    isHost = true,
)

@Preview(name = "8f 공지 상세 · 호스트", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8f() = PreviewContent(previewHost)

@Preview(name = "8g 공지 상세 · 참가자", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8g() = PreviewContent(previewHost.copy(myUserId = "u3", isHost = false))

@Preview(name = "8h 댓글 입력 중", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8h() = PreviewContent(previewHost.copy(myUserId = "u3", isHost = false, commentInput = "우산 챙겨갈게요"))

@Preview(name = "8i 댓글 없음", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8i() =
    PreviewContent(previewHost.copy(notice = previewNotices[1], comments = emptyList(), myUserId = "u3", isHost = false))
