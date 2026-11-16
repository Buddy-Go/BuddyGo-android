package com.example.myapplication.ui.meeting.create

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.Calendar
import java.util.TimeZone

// 시간 설정 시트 ← 10f. 시작 시각(시·분 휠) + 진행 시간(−/+). 종료 = 시작 + 진행 시간.

private const val MINUTE_STEP = 10 // 10f 휠이 50, 00, 10 단위
private const val DEFAULT_START = 19 * 60 // (추정) 아직 시간을 안 골랐을 때 처음 보일 값
private const val DEFAULT_DURATION = 60 // (추정)

// 진행 시간 규칙: 최소 30분에서 시작해 10분 단위로 늘리고 줄인다. 최대 4시간.
// 서버(gathering.min-duration 30m, duration-step 10m, max-duration 4h)와 같은 값이라 서버가 거절하는 시간은 고를 수 없다.
internal object DurationRules {
    const val MIN = 30
    const val STEP = 10
    const val MAX = 4 * 60

    fun minus(minutes: Int): Int = (minutes - STEP).coerceAtLeast(MIN)
    fun plus(minutes: Int): Int = (minutes + STEP).coerceAtMost(MAX)
    fun canMinus(minutes: Int): Boolean = minutes > MIN
    fun canPlus(minutes: Int): Boolean = minutes < MAX
}

// 시작 시각 규칙: 오늘을 골랐다면 이미 지난 시각은 고를 수 없다. (서버의 '지금+1시간 30분' 검사와 별개로, 지난 시간을 막는 화면 쪽 안전장치)
internal object StartTimeRules {
    private const val LAST_START = 23 * 60 + 50

    // 고를 수 있는 가장 이른 시작 시각(자정부터 분). 오늘이 아니면 null(제한 없음). 10분 단위로 올림한다.
    fun minStart(date: DateOption?, nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int? {
        date ?: return null
        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val isToday = now.get(Calendar.YEAR) == date.year &&
            now.get(Calendar.MONTH) + 1 == date.month &&
            now.get(Calendar.DAY_OF_MONTH) == date.day
        if (!isToday) return null
        val minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val rounded = (minutes + MINUTE_STEP - 1) / MINUTE_STEP * MINUTE_STEP
        return rounded.coerceAtMost(LAST_START)
    }

    // 처음 보일 시작 시각: 이미 고른 값이 있으면 그 값, 없으면 19:00. 둘 다 제한보다 이르면 제한 시각.
    // 고른 시(時)에서 고를 수 있는 가장 이른 분 칸 번호.
    fun minMinuteIndex(minStart: Int?, hour: Int): Int =
        if (minStart != null && hour == minStart / 60) minStart % 60 / MINUTE_STEP else 0

    fun initialStart(chosen: Int?, minStart: Int?): Int = (chosen ?: DEFAULT_START).coerceAtLeast(minStart ?: 0)
}

@Composable
fun TimeBottomSheet(
    initialStartMinutes: Int?,
    initialDurationMinutes: Int?,
    minStartMinutes: Int?,
    onApply: (startMinutes: Int, durationMinutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = StartTimeRules.initialStart(initialStartMinutes, minStartMinutes)
    var hour by remember { mutableIntStateOf(start / 60) }
    var minuteIndex by remember { mutableIntStateOf(start % 60 / MINUTE_STEP) }
    var duration by remember { mutableIntStateOf(initialDurationMinutes ?: DEFAULT_DURATION) }

    CreateSheetFrame(onDismiss = onDismiss) {
        TimeSheetBody(
            hour = hour,
            minuteIndex = minuteIndex,
            durationMinutes = duration,
            minStartMinutes = minStartMinutes,
            onHourSelect = {
                hour = it
                // 제한 시각의 시(時)로 오면 지난 분은 제한 분으로 끌어올린다.
                val minIdx = StartTimeRules.minMinuteIndex(minStartMinutes, it)
                if (minuteIndex < minIdx) minuteIndex = minIdx
            },
            onMinuteSelect = { minuteIndex = it },
            onDurationMinus = { duration = DurationRules.minus(duration) },
            onDurationPlus = { duration = DurationRules.plus(duration) },
            onConfirm = { onApply(hour * 60 + minuteIndex * MINUTE_STEP, duration) },
        )
    }
}

@Composable
internal fun TimeSheetBody(
    hour: Int,
    minuteIndex: Int,
    durationMinutes: Int,
    minStartMinutes: Int? = null,
    onHourSelect: (Int) -> Unit,
    onMinuteSelect: (Int) -> Unit,
    onDurationMinus: () -> Unit,
    onDurationPlus: () -> Unit,
    onConfirm: () -> Unit,
) {
    val startMinutes = hour * 60 + minuteIndex * MINUTE_STEP
    val endMinutes = startMinutes + durationMinutes
    val endText = if (endMinutes >= 24 * 60) "다음 날 ${clock24(endMinutes)}" else clock24(endMinutes)

    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "시간 설정",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.TextPrimary,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = "${clock24(startMinutes)} – ${clock24(endMinutes)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.TextSecondary,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
            SheetSectionLabel("시작 시간")
            Box(contentAlignment = Alignment.Center) {
                // 가운데 선택 줄 위아래 선
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
                    Spacer(Modifier.height(WHEEL_ITEM_HEIGHT - 2.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpace.x5),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelColumn(
                        values = remember { List(24) { it.toString().padStart(2, '0') } },
                        selectedIndex = hour,
                        minIndex = (minStartMinutes ?: 0) / 60,
                        onSelect = onHourSelect,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = ":", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                    WheelColumn(
                        values = remember { List(60 / MINUTE_STEP) { (it * MINUTE_STEP).toString().padStart(2, '0') } },
                        selectedIndex = minuteIndex,
                        minIndex = StartTimeRules.minMinuteIndex(minStartMinutes, hour),
                        onSelect = onMinuteSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Divider))

        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SheetSectionLabel("진행 시간")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.xs),
                ) {
                    StepButton(
                        iconRes = R.drawable.ic_minus,
                        description = "진행 시간 줄이기",
                        enabled = DurationRules.canMinus(durationMinutes),
                        onClick = onDurationMinus,
                    )
                    Text(
                        text = durationText(durationMinutes),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColor.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(96.dp),
                    )
                    StepButton(
                        iconRes = R.drawable.ic_plus,
                        description = "진행 시간 늘리기",
                        enabled = DurationRules.canPlus(durationMinutes),
                        onClick = onDurationPlus,
                    )
                }
            }
            Text(
                text = "${endText}에 종료돼요",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SheetFilledButton(text = "확인", onClick = onConfirm)
    }
}

private val WHEEL_ITEM_HEIGHT = 44.dp

// 위·아래로 굴려 고르는 숫자 휠. 세 줄이 보이고 가운데 줄이 선택값이다.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    values: List<String>,
    selectedIndex: Int,
    minIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 맨 위·아래에 빈 줄을 하나씩 둔다. 그래서 '화면 맨 위 줄 번호'가 곧 '가운데 값 번호'다.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val itemPx = with(LocalDensity.current) { WHEEL_ITEM_HEIGHT.toPx() }
    val centerIndex by remember(values) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex +
                if (listState.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
            index.coerceIn(0, values.lastIndex)
        }
    }
    // 멈춘 자리가 지난 시각이면 고를 수 있는 첫 칸으로 되돌린다. 제한이 바뀌어도(시를 옮겨 분 제한이 달라질 때) 같다.
    LaunchedEffect(listState.isScrollInProgress, minIndex) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        if (centerIndex < minIndex) {
            listState.animateScrollToItem(minIndex)
            onSelect(minIndex)
        } else {
            onSelect(centerIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(WHEEL_ITEM_HEIGHT * 3),
    ) {
        item { Spacer(Modifier.height(WHEEL_ITEM_HEIGHT)) }
        itemsIndexed(values) { index, value ->
            val selected = index == centerIndex
            val disabled = index < minIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WHEEL_ITEM_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value,
                    fontSize = if (selected) 20.sp else 16.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        disabled -> AppColor.TextDisabled.copy(alpha = 0.4f) // 지난 시각: 더 흐리게
                        selected -> AppColor.TextPrimary
                        else -> AppColor.TextDisabled
                    },
                )
            }
        }
        item { Spacer(Modifier.height(WHEEL_ITEM_HEIGHT)) }
    }
}

@Composable
private fun StepButton(@DrawableRes iconRes: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(AppColor.ChipNeutralBg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = if (enabled) AppColor.TextPrimary else AppColor.TextDisabled,
            modifier = Modifier.size(16.dp),
        )
    }
}
