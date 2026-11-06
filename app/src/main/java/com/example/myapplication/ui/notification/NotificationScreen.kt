package com.example.myapplication.ui.notification

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Notification
import com.example.myapplication.ui.common.FullScreenError
import com.example.myapplication.ui.common.SkeletonList
import com.example.myapplication.ui.common.UiState
import com.example.myapplication.ui.explore.filter.ChevronLeftIcon
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace

// 3-6 알림 목록, 3-6-2 빈 상태
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onOpen: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val nowMillis = remember { System.currentTimeMillis() }
    val data = (state as? UiState.Success)?.data.orEmpty()

    Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding()) {
        Header(onBack = onBack, showMarkAll = data.any { !it.isRead }, onMarkAll = viewModel::onMarkAllRead)
        when (val s = state) {
            UiState.Loading -> SkeletonList(Modifier.padding(horizontal = AppSpace.xxl))
            is UiState.Error -> FullScreenError(type = s.type, onRetry = viewModel::load)
            is UiState.Success -> {
                val sections = groupNotifications(s.data, nowMillis)
                if (sections.isEmpty) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("아직 받은 알림이 없어요", color = AppColor.TextDisabled, fontSize = 15.sp)
                    }
                } else {
                    val click: (Notification) -> Unit = { n ->
                        viewModel.onNotificationClick(n)
                        n.destination()?.let(onOpen)
                    }
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (sections.today.isNotEmpty()) {
                            item { SectionTitle("오늘") }
                            items(sections.today, key = { it.id }) { NotificationRow(it, nowMillis, click) }
                        }
                        if (sections.earlier.isNotEmpty()) {
                            item { SectionTitle("이전") }
                            items(sections.earlier, key = { it.id }) { NotificationRow(it, nowMillis, click) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit, showMarkAll: Boolean, onMarkAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = AppSpace.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).iconButton("뒤로 가기", onBack), contentAlignment = Alignment.Center) {
            ChevronLeftIcon(AppColor.TextPrimary)
        }
        Text("알림", Modifier.weight(1f).padding(start = AppSpace.md), color = AppColor.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (showMarkAll) {
            Text(
                "모두 읽음",
                Modifier.clip(CircleShape).clickable(onClickLabel = "모든 알림을 읽음으로 표시", onClick = onMarkAll)
                    .padding(horizontal = AppSpace.xl, vertical = AppSpace.md),
                color = AppColor.TextSecondary, fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().padding(horizontal = AppSpace.xxl).padding(top = AppSpace.x4, bottom = AppSpace.md),
        color = AppColor.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun NotificationRow(n: Notification, nowMillis: Long, onClick: (Notification) -> Unit) {
    val timeText = formatRelativeTime(n.createdAtMillis, nowMillis)
    val textColor = if (n.isRead) AppColor.TextSecondary else AppColor.TextPrimary
    Row(
        Modifier.fillMaxWidth()
            .clickable { onClick(n) }
            .semantics(mergeDescendants = true) { contentDescription = n.spokenText(timeText) }
            .padding(horizontal = AppSpace.xxl, vertical = AppSpace.xl),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(AppColor.ChipNeutralBg), contentAlignment = Alignment.Center) {
            TypeIcon(n.type.icon())
        }
        Spacer(Modifier.width(AppSpace.xl))
        Column(Modifier.weight(1f).clearAndSetSemantics { }) {
            Text(
                buildAnnotatedString {
                    n.subject?.let { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(it) } }
                    append(joinSubject(n.subject, n.message).removePrefix(n.subject.orEmpty()))
                },
                color = textColor, fontSize = 15.sp, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(timeText, color = AppColor.TextDisabled, fontSize = 13.sp)
        }
        if (!n.isRead) {
            Box(Modifier.padding(start = AppSpace.xl, top = 6.dp).size(8.dp).clip(CircleShape).background(AppColor.Accent))
        }
    }
}

@Composable
private fun TypeIcon(icon: NotificationIcon) {
    val c = AppColor.IconMuted
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(w * x, w * y)
        when (icon) {
            NotificationIcon.PIN_CHECK -> {
                drawCircle(c, w * .3f, p(.5f, .4f), style = stroke)
                drawPath(Path().apply { moveTo(w * .5f, w * .7f); lineTo(w * .5f, w * .95f) }, c, style = stroke)
                drawPath(Path().apply { moveTo(w * .4f, w * .4f); lineTo(w * .48f, w * .48f); lineTo(w * .62f, w * .32f) }, c, style = stroke)
            }
            NotificationIcon.MEGAPHONE -> {
                drawPath(Path().apply {
                    moveTo(w * .15f, w * .4f); lineTo(w * .4f, w * .4f); lineTo(w * .8f, w * .15f)
                    lineTo(w * .8f, w * .85f); lineTo(w * .4f, w * .6f); lineTo(w * .15f, w * .6f); close()
                }, c, style = stroke)
            }
            NotificationIcon.PEOPLE -> {
                drawCircle(c, w * .15f, p(.38f, .32f), style = stroke)
                drawCircle(c, w * .12f, p(.72f, .38f), style = stroke)
                drawPath(Path().apply { moveTo(w * .1f, w * .85f); cubicTo(w * .1f, w * .55f, w * .66f, w * .55f, w * .66f, w * .85f) }, c, style = stroke)
            }
            NotificationIcon.CHECK -> drawPath(Path().apply {
                moveTo(w * .2f, w * .52f); lineTo(w * .42f, w * .72f); lineTo(w * .8f, w * .3f)
            }, c, style = stroke)
            NotificationIcon.CLOSE -> {
                drawLine(c, p(.25f, .25f), p(.75f, .75f), stroke.width, StrokeCap.Round)
                drawLine(c, p(.75f, .25f), p(.25f, .75f), stroke.width, StrokeCap.Round)
            }
        }
    }
}
