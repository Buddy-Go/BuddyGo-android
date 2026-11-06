package com.example.myapplication.ui.explore.filter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.common.ActionButton
import com.example.myapplication.ui.common.sportLabel
import com.example.myapplication.ui.explore.iconButton
import com.example.myapplication.ui.explore.selectableItem
import com.example.myapplication.ui.record.SportIcon
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 4a~4c 조건 설정. 종목·날짜·시간대를 고르고, 기준·위치·상세 조건 줄을 눌러 시트나 화면으로 들어간다.
// 닫기(X)는 적용하지 않고, '모임 N개 찾기'를 눌러야 탐색에 적용된다.
@Composable
fun FilterScreen(
    onClose: () -> Unit,
    onApplied: () -> Unit,
    onLocationClick: (currentDistrict: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    FilterContent(
        state = state,
        onClose = onClose,
        onReset = viewModel::onReset,
        onSelectSport = viewModel::onSelectSport,
        onSelectDate = viewModel::onSelectDate,
        onOpenDatePicker = { viewModel.onOpenSheet(FilterSheet.DATE_PICKER) },
        onToggleTimeSlot = viewModel::onToggleTimeSlot,
        onOpenTimeRange = { viewModel.onOpenSheet(FilterSheet.TIME_RANGE) },
        onClearTimeRange = viewModel::onClearTimeRange,
        onOpenCriteria = { viewModel.onOpenSheet(FilterSheet.CRITERIA) },
        onLocationClick = { onLocationClick(state.draft.district) },
        onOpenDetail = { viewModel.onOpenSheet(FilterSheet.DETAIL) },
        onApply = {
            viewModel.onApply()
            onApplied()
        },
        modifier = modifier,
    )

    // 조건 설정 위에 뜨는 시트들. 닫으면(적용 없이) 열기 전 값으로 돌아간다.
    when (state.openSheet) {
        FilterSheet.CRITERIA -> HealthFilterBottomSheet(
            state = state,
            onTogglePart = viewModel::onTogglePart,
            onSetCriteriaOn = viewModel::onSetCriteriaOn,
            onResetToMine = viewModel::onResetToMine,
            onSetRange = viewModel::onSetRange,
            onSetArmLevel = viewModel::onSetArmLevel,
            onSetPaceRange = viewModel::onSetPaceRange,
            onSetHikingStyle = viewModel::onSetHikingStyle,
            onDismiss = viewModel::onDismissSheet,
            onApply = viewModel::onApplySheet,
        )
        FilterSheet.DETAIL -> DetailConditionBottomSheet(
            state = state,
            onSetGender = viewModel::onSetGender,
            onSetMannerOn = viewModel::onSetMannerOn,
            onSetMinManner = viewModel::onSetMinManner,
            onDismiss = viewModel::onDismissSheet,
            onApply = viewModel::onApplySheet,
        )
        FilterSheet.TIME_RANGE -> TimeRangeBottomSheet(
            range = state.draft.customRange ?: TimeOfDayRange.DEFAULT,
            onSetStart = viewModel::onSetTimeStart,
            onSetEnd = viewModel::onSetTimeEnd,
            onDismiss = viewModel::onDismissSheet,
            onConfirm = viewModel::onApplySheet,
        )
        FilterSheet.DATE_PICKER -> DatePickerDialogContent(
            selectedMillis = state.draft.dateMillis,
            todayMillis = state.nowMillis,
            onPick = viewModel::onPickDate,
            onDismiss = viewModel::onDismissSheet,
        )
        null -> Unit
    }
}

@Composable
private fun FilterContent(
    state: FilterState,
    onClose: () -> Unit,
    onReset: () -> Unit,
    onSelectSport: (Sport) -> Unit,
    onSelectDate: (Long) -> Unit,
    onOpenDatePicker: () -> Unit,
    onToggleTimeSlot: (TimeSlot) -> Unit,
    onOpenTimeRange: () -> Unit,
    onClearTimeRange: () -> Unit,
    onOpenCriteria: () -> Unit,
    onLocationClick: () -> Unit,
    onOpenDetail: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft
    Column(modifier.fillMaxSize().background(AppColor.Surface).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape).iconButton("조건 설정 닫기", onClose), contentAlignment = Alignment.Center) {
                CloseIcon(AppColor.TextPrimary)
            }
            Spacer(Modifier.width(AppSpace.xl))
            Text("조건 설정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                "초기화", color = AppColor.TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(RoundedCornerShape(AppRadius.sm)).clickable(onClick = onReset).padding(AppSpace.md),
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = AppSpace.x4),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
        ) {
            SectionTitle("종목")
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
                Sport.entries.forEach { sport ->
                    SportCard(sport, selected = draft.sport == sport, onClick = { onSelectSport(sport) }, modifier = Modifier.weight(1f))
                }
            }

            SectionTitle("날짜 · 시간대")
            DateRow(state, onSelectDate, onOpenDatePicker)
            TimeRow(draft, onToggleTimeSlot, onOpenTimeRange, onClearTimeRange)

            SettingsCard(
                criteria = criteriaSummary(draft),
                location = locationSummary(draft),
                detail = detailSummary(draft),
                onCriteriaClick = onOpenCriteria,
                onLocationClick = onLocationClick,
                onDetailClick = onOpenDetail,
            )
            Spacer(Modifier.height(AppSpace.xxl))
        }

        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))
            ActionButton(
                text = "모임 ${state.resultCount}개 찾기",
                onClick = onApply,
                modifier = Modifier.padding(horizontal = AppSpace.x4, vertical = AppSpace.xxl),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) =
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)

@Composable
private fun SportCard(sport: Sport, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Surface(
        modifier = modifier.height(84.dp).clip(shape).selectableItem(selected, onClick),
        shape = shape,
        color = if (selected) AppColor.AccentSoft else AppColor.Surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            SportIcon(sport, if (selected) AppColor.AccentStrong else AppColor.TextPrimary, size = 24.dp)
            Spacer(Modifier.height(AppSpace.md))
            Text(
                sportLabel(sport), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                color = if (selected) AppColor.AccentStrong else AppColor.TextPrimary,
            )
        }
    }
}

@Composable
private fun DateRow(state: FilterState, onSelectDate: (Long) -> Unit, onOpenDatePicker: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            dateOptions(state.nowMillis, pickedStartOfDay = state.draft.dateMillis).forEach { option ->
                val selected = state.draft.dateMillis == option.startOfDayMillis
                Column(
                    Modifier
                        .width(48.dp).height(60.dp)
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(if (selected) AppColor.Accent else AppColor.Surface)
                        .selectableItem(selected, { onSelectDate(option.startOfDayMillis) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(option.weekdayLabel, fontSize = 12.sp, color = if (selected) AppColor.TextPrimary else AppColor.TextSecondary)
                    Text(option.dateLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                }
            }
        }
        Spacer(Modifier.width(AppSpace.md))
        // 달력: 5일 밖의 날짜를 고를 때 쓴다.
        Box(
            Modifier.size(40.dp).clip(CircleShape).iconButton("날짜 직접 고르기", onOpenDatePicker),
            contentAlignment = Alignment.Center,
        ) { CalendarIcon(AppColor.TextPrimary) }
    }
}

@Composable
private fun TimeRow(
    draft: MeetingFilter,
    onToggleSlot: (TimeSlot) -> Unit,
    onOpenTimeRange: () -> Unit,
    onClearTimeRange: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val container = Modifier
            .weight(1f).height(44.dp)
            .clip(RoundedCornerShape(AppRadius.md)).background(AppColor.ChipNeutralBg)
        val range = draft.customRange
        if (range != null) {
            // 4a-2: 직접 고른 시간 범위 알약. X를 누르면 시간대 선택으로 돌아간다.
            Row(container.padding(horizontal = AppSpace.xxl), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timeRangeLabel(range), color = AppColor.AccentStrong, fontSize = 15.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                )
                Box(Modifier.size(28.dp).clip(CircleShape).iconButton("시간 범위 지우기", onClearTimeRange), contentAlignment = Alignment.Center) {
                    CloseIcon(AppColor.TextSecondary, size = 18.dp)
                }
            }
        } else {
            Row(container.padding(4.dp)) {
                TimeSlot.entries.forEach { slot ->
                    val selected = slot in draft.timeSlots
                    Box(
                        Modifier
                            .weight(1f).fillMaxSize()
                            .clip(RoundedCornerShape(AppRadius.sm))
                            .background(if (selected) AppColor.Surface else AppColor.ChipNeutralBg)
                            .selectableItem(selected, { onToggleSlot(slot) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            slot.label, fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) AppColor.AccentStrong else AppColor.TextSecondary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(AppSpace.md))
        Box(
            Modifier.size(40.dp).clip(CircleShape).iconButton("시간 범위 설정", onOpenTimeRange),
            contentAlignment = Alignment.Center,
        ) { ClockIcon(if (range != null) AppColor.AccentStrong else AppColor.TextPrimary) }
    }
}

@Composable
private fun SettingsCard(
    criteria: String?,
    location: String,
    detail: String,
    onCriteriaClick: () -> Unit,
    onLocationClick: () -> Unit,
    onDetailClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, AppColor.Border, shape).padding(horizontal = AppSpace.xxl)) {
        // 기준은 종목을 골라야 설정할 수 있다.
        SettingRow("기준", criteria ?: "종목을 먼저 골라 주세요", enabled = criteria != null, onClick = onCriteriaClick)
        RowDivider()
        SettingRow("위치", location, onClick = onLocationClick)
        RowDivider()
        SettingRow("상세 조건", detail, onClick = onDetailClick)
    }
}

@Composable
private fun RowDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.Border))

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().height(62.dp).clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
        Text(
            value, fontSize = 15.sp, textAlign = TextAlign.End,
            color = if (enabled && !value.endsWith("관계없음")) AppColor.TextPrimary else AppColor.TextDisabled,
            modifier = Modifier.weight(1f).padding(start = AppSpace.xxl, end = AppSpace.md),
        )
        ChevronRightIcon(if (enabled) AppColor.TextSecondary else AppColor.TextDisabled)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun FilterScreenPreview() {
    MyApplicationTheme(dynamicColor = false) {
        FilterContent(
            state = FilterState(draft = MeetingFilter(sport = Sport.HEALTH, minMannerTemperature = 30), nowMillis = 1_798_761_600_000L),
            onClose = {}, onReset = {}, onSelectSport = {}, onSelectDate = {}, onOpenDatePicker = {}, onToggleTimeSlot = {},
            onOpenTimeRange = {}, onClearTimeRange = {}, onOpenCriteria = {}, onLocationClick = {}, onOpenDetail = {}, onApply = {},
        )
    }
}

// 날짜 선택 창. 오늘 이전은 고를 수 없다. 창이 주고받는 날짜는 UTC 그날 0시다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogContent(selectedMillis: Long?, todayMillis: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val todayUtc = utcMidnightOf(todayMillis)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis?.let { utcMidnightOf(it) },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let(onPick) },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text("확인", color = AppColor.AccentStrong, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = AppColor.TextSecondary) } },
        colors = DatePickerDefaults.colors(containerColor = AppColor.Surface),
    ) {
        DatePicker(
            state = pickerState,
            colors = DatePickerDefaults.colors(
                containerColor = AppColor.Surface,
                selectedDayContainerColor = AppColor.Accent, selectedDayContentColor = AppColor.TextPrimary,
                todayDateBorderColor = AppColor.Accent, todayContentColor = AppColor.AccentStrong,
            ),
        )
    }
}
