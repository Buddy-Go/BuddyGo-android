package com.example.myapplication.ui.mate.review

import com.example.myapplication.ui.common.InlineErrorBanner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.SuitFontFamily

// 한 명씩 평가 ← 12b-1 판정 미선택, 12b-2 좋음, 12b-3 보통, 12b-4 아쉬움(마지막)
@Composable
fun MemberReviewScreen(
    viewModel: ReviewViewModel,
    onClose: () -> Unit, // X → 12a로
    onAllDone: () -> Unit, // 마지막 제출 후 12c로
) {
    val state = viewModel.state
    LaunchedEffect(state.isAllDone) {
        if (state.isAllDone) onAllDone()
    }
    MemberReviewContent(
        state = state,
        onClose = onClose,
        onVerdictSelect = viewModel::onVerdictSelect,
        onTagToggle = viewModel::onTagToggle,
        onCommentChange = viewModel::onCommentChange,
        onSubmit = viewModel::onSubmit,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberReviewContent(
    state: ReviewState,
    onClose: () -> Unit,
    onVerdictSelect: (ReviewVerdict) -> Unit,
    onTagToggle: (ReviewTag) -> Unit,
    onCommentChange: (String) -> Unit,
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
        ReviewTopBar(
            iconRes = R.drawable.ic_close,
            iconDescription = "닫기",
            onIconClick = onClose,
            trailingText = state.progressText,
        )
        val target = state.currentTarget
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = AppSpace.x4, end = AppSpace.x4, top = AppSpace.xxl, bottom = AppSpace.x4),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
        ) {
            if (state.submitFailed) InlineErrorBanner("평가를 보내지 못했어요. 다시 시도해주세요.")
            if (target != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                    ReviewAvatar(userId = target.userId, nickname = target.nickname)
                    Text(text = target.nickname, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                }
            }
            Text(text = state.question, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)

            // 판정 세 개
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
                ReviewVerdict.entries.forEach { verdict ->
                    VerdictButton(
                        label = verdict.label,
                        selected = state.draft.verdict == verdict,
                        onClick = { onVerdictSelect(verdict) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.showDetails) {
                Line()
                SectionHeader(state.tagQuestion)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpace.md),
                ) {
                    state.availableTags.forEach { tag ->
                        TagChip(label = tag.label, selected = tag in state.draft.tags, onClick = { onTagToggle(tag) })
                    }
                }
                Line()
                SectionHeader("한마디 남기기")
                val style = TextStyle(fontFamily = SuitFontFamily, fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
                BasicTextField(
                    value = state.draft.comment,
                    onValueChange = onCommentChange,
                    textStyle = style,
                    cursorBrush = SolidColor(AppColor.AccentStrong),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    decorationBox = { inner ->
                        Box {
                            if (state.draft.comment.isEmpty()) {
                                Text(text = "같이 운동하기 좋았어요. 호흡이 잘 맞았습니다.", style = style.copy(color = AppColor.TextDisabled))
                            }
                            inner()
                        }
                    },
                )
                Line()
                Text(
                    text = state.commentCounter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.TextSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxs)) {
                Text(text = "실력이나 기록과 별개로 매너를 평가해주세요.", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
                Text(text = "남긴 글은 상대 프로필에 공개돼요.", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
            }
        }
        ReviewBottomButton(text = state.submitLabel, enabled = state.canSubmit, onClick = onSubmit, showTopLine = true)
    }
}

@Composable
private fun VerdictButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppRadius.md)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (selected) AppColor.Accent else AppColor.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, AppColor.Border, shape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) AppColor.OnAccent else AppColor.TextBody,
        )
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.pill)
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) AppColor.AccentSoft else AppColor.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, AppColor.Border, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpace.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) AppColor.AccentStrong else AppColor.TextBody,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(text = "선택", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextDisabled)
    }
}

@Composable
private fun Line() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
}

// ---------- 미리보기 ----------

private val previewReviewing = previewReviewState.copy(currentTargetId = "u4")

@Preview(name = "12b-1 판정 미선택 (2/3)", widthDp = 360, heightDp = 800)
@Composable
private fun Preview12b1() = PreviewContent(previewReviewing)

@Preview(name = "12b-2 좋음 선택 (2/3)", widthDp = 360, heightDp = 800)
@Composable
private fun Preview12b2() = PreviewContent(
    previewReviewing.copy(draft = ReviewDraft(ReviewVerdict.GOOD, setOf(ReviewTag.PUNCTUAL, ReviewTag.GOOD_MANNERS))),
)

@Preview(name = "12b-3 보통 선택 (2/3)", widthDp = 360, heightDp = 800)
@Composable
private fun Preview12b3() = PreviewContent(previewReviewing.copy(draft = ReviewDraft(ReviewVerdict.NORMAL)))

@Preview(name = "12b-4 아쉬움 선택 (3/3 마지막)", widthDp = 360, heightDp = 800)
@Composable
private fun Preview12b4() = PreviewContent(
    previewReviewState.copy(
        session = previewReviewState.session?.let { s ->
            s.copy(targets = s.targets.map { if (it.userId == "u6") it else it.copy(isReviewed = true) })
        },
        currentTargetId = "u6",
        draft = ReviewDraft(ReviewVerdict.BAD, setOf(ReviewTag.POOR_COMMUNICATION)),
    ),
)

@Composable
private fun PreviewContent(state: ReviewState) {
    MemberReviewContent(state = state, onClose = {}, onVerdictSelect = {}, onTagToggle = {}, onCommentChange = {}, onSubmit = {})
}
