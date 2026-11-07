package com.example.myapplication.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.ProfileReview
import com.example.myapplication.data.model.ReceivedReview
import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.common.SkeletonList
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.common.sportLabel
import com.example.myapplication.ui.explore.filter.ChevronLeftIcon
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 받은 후기 전체 (마이페이지 '전체 보기', 메이트 프로필 '전체 >'). 디자인 프레임이 없어 14의 후기 카드를 그대로 세로로 나열한다.
// 두 화면의 후기 모양이 달라서(ReceivedReview, ProfileReview) 같은 한 줄 모델로 바꿔 그린다.
data class ReviewRow(
    val id: String,
    val author: String,
    val meta: String?,       // "등산 · 9/1" 처럼 작은 글자. 없으면 숨김
    val verdict: ReviewVerdict?,
    val comment: String?,
    val tags: List<String> = emptyList(),
    val createdAtMillis: Long = 0L,
)

fun ReceivedReview.toRow(): ReviewRow =
    ReviewRow(id, reviewerNickname, "${sportLabel(sport)} · ${formatReviewDate(createdAtMillis)}", verdict, comment, tags, createdAtMillis)

fun ProfileReview.toRow(): ReviewRow =
    ReviewRow("$authorId-$createdAtMillis", authorNickname, formatReviewDate(createdAtMillis), null, comment, listOfNotNull(tag), createdAtMillis)

// 최신순으로 정렬한다. 비어 있는 한마디는 그대로 두고 태그만 보인다.
fun List<ReviewRow>.newestFirst(): List<ReviewRow> = sortedByDescending { it.createdAtMillis }

@Composable
fun AllReviewsScreen(
    title: String,
    state: UiState<List<ReviewRow>>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = AppSpace.md), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).iconButton("뒤로 가기", onBack), contentAlignment = Alignment.Center) {
                ChevronLeftIcon(AppColor.TextPrimary)
            }
            Text(title, Modifier.padding(start = AppSpace.md), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        }
        when (state) {
            UiState.Loading -> SkeletonList(Modifier.padding(horizontal = AppSpace.xxl))
            is UiState.Error -> FullScreenError(state.type, onRetry = onRetry)
            is UiState.Success -> if (state.data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("아직 받은 후기가 없어요", color = AppColor.TextDisabled, fontSize = 15.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpace.x4), verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                    items(state.data, key = { it.id }) { RowCard(it) }
                }
            }
        }
    }
}

@Composable
private fun RowCard(r: ReviewRow) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, AppColor.Border, shape).padding(AppSpace.xxl)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(r.author, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            r.meta?.let {
                Spacer(Modifier.width(AppSpace.md))
                Text(it, fontSize = 13.sp, color = AppColor.TextDisabled, modifier = Modifier.weight(1f))
            } ?: Spacer(Modifier.weight(1f))
            r.verdict?.let { v ->
                val (bg, fg) = when (v) {
                    ReviewVerdict.GOOD -> AppColor.AccentSoft to AppColor.AccentStrong
                    ReviewVerdict.NORMAL -> AppColor.ChipNeutralBg to AppColor.TextSecondary
                    ReviewVerdict.BAD -> AppColor.DestructiveSoftBg to AppColor.DestructiveText
                }
                Text(verdictLabel(v), Modifier.clip(RoundedCornerShape(AppRadius.sm)).background(bg).padding(horizontal = AppSpace.xl, vertical = 4.dp), color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        r.comment?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(AppSpace.xl))
            Text(it, fontSize = 15.sp, lineHeight = 22.sp, color = AppColor.TextPrimary)
        }
        if (r.tags.isNotEmpty()) {
            Spacer(Modifier.height(AppSpace.xl))
            Text(r.tags.joinToString(" · "), fontSize = 13.sp, color = AppColor.TextSecondary)
        }
    }
}
