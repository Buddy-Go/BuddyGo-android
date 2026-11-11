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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.common.InlineErrorBanner
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.SuitFontFamily

// 공지 작성·수정 화면 ← 8a 빈 상태, 8b 입력 중, 8c 수정. 호스트 전용.
// ViewModel은 화면을 띄우는 쪽(AppNavHost)에서 meetingId(·noticeId)로 만들어 넘겨준다.
@Composable
fun NoticeEditScreen(
    viewModel: NoticeEditViewModel,
    onClose: () -> Unit,
    onDone: () -> Unit, // 등록·저장 후 이전 화면으로
) {
    val state = viewModel.state
    LaunchedEffect(state.isDone) {
        if (state.isDone) onDone()
    }
    NoticeEditContent(
        state = state,
        onClose = onClose,
        onTitleChange = viewModel::onTitleChange,
        onBodyChange = viewModel::onBodyChange,
        onSubmit = viewModel::onSubmit,
    )
}

@Composable
private fun NoticeEditContent(
    state: NoticeEditState,
    onClose: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSubmit: () -> Unit,
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
            leadingIcon = R.drawable.ic_close,
            leadingDescription = "닫기",
            title = state.screenTitle,
            onLeadingClick = onClose,
        ) {
            Text(
                text = state.submitLabel,
                fontSize = 15.sp,
                fontWeight = if (state.canSubmit) FontWeight.Bold else FontWeight.Medium,
                color = if (state.canSubmit) AppColor.AccentStrong else AppColor.TextDisabled,
                modifier = Modifier.clickable(enabled = state.canSubmit, onClick = onSubmit),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpace.lg),
        ) {
            if (state.submitFailed) InlineErrorBanner("저장하지 못했어요. 다시 시도해주세요.")
            val titleStyle = TextStyle(fontFamily = SuitFontFamily, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            PlaceholderField(
                value = state.title,
                onValueChange = onTitleChange,
                placeholder = "제목을 입력해주세요",
                style = titleStyle,
                singleLine = true,
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))

            val bodyStyle = TextStyle(
                fontFamily = SuitFontFamily,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextPrimary,
            )
            PlaceholderField(
                value = state.body,
                onValueChange = onBodyChange,
                placeholder = "집합 장소나 준비물처럼 참여자가 알아야 할 내용을 적어주세요",
                style = bodyStyle,
                singleLine = false,
                modifier = Modifier.heightIn(min = 160.dp),
            )
            Text(
                text = state.counterText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = AppColor.TextBody,
                    modifier = Modifier.size(16.dp),
                )
                // 정의서 9장: 작성·수정 모두 확정 참가자에게 알림
                Text(
                    text = "확정된 메이트에게 알림이 가요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextBody,
                )
            }
        }
    }
}

@Composable
private fun PlaceholderField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = singleLine,
        cursorBrush = SolidColor(AppColor.AccentStrong),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(text = placeholder, style = style.copy(color = AppColor.TextDisabled))
                inner()
            }
        },
    )
}

// ---------- 미리보기 ----------

@Composable
private fun PreviewContent(state: NoticeEditState) {
    NoticeEditContent(state = state, onClose = {}, onTitleChange = {}, onBodyChange = {}, onSubmit = {})
}

private const val PREVIEW_BODY = "비 예보가 있어서 정문 대신 후문 3층 프리웨이트 존에서 만나요."

@Preview(name = "8a 공지 작성 · 빈 상태", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8a() = PreviewContent(NoticeEditState())

@Preview(name = "8b 공지 작성 · 입력 중", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8b() = PreviewContent(NoticeEditState(title = "집합 장소 변경", body = PREVIEW_BODY))

@Preview(name = "8c 공지 수정", widthDp = 360, heightDp = 800)
@Composable
private fun Preview8c() =
    PreviewContent(NoticeEditState(isEditMode = true, title = "집합 장소 변경", body = PREVIEW_BODY))
