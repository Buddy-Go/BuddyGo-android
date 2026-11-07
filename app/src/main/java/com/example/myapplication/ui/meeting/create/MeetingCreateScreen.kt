package com.example.myapplication.ui.meeting.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.ui.common.ToastType
import com.example.myapplication.ui.common.ToastState
import com.example.myapplication.ui.common.ToastHost
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace

// 모임 개설 화면 ← 10a 헬스, 10a-2 헬스 기록 숨김
// 2026-10-30 범위 변경으로 헬스만 개설한다. 디자인의 종목 선택 영역과 10b·10c는 만들지 않는다.
// 시트: 10d HealthCriteriaBottomSheet, 10e DescriptionBottomSheet, 10f TimeBottomSheet
// ViewModel은 화면을 띄우는 쪽(AppNavHost)에서 넘겨준다.
@Composable
fun MeetingCreateScreen(
    viewModel: MeetingCreateViewModel,
    onClose: () -> Unit,
    onPlaceClick: () -> Unit, // 10g 장소 검색 화면으로 이동
    onCreated: (meetingId: String) -> Unit, // 개설 후 5번 모임 상세로
    onUpdated: () -> Unit = {}, // 수정 후 모임 상세로 돌아간다
) {
    val state = viewModel.state
    LaunchedEffect(state.createdMeetingId) {
        state.createdMeetingId?.let(onCreated)
    }
    LaunchedEffect(state.updated) {
        if (state.updated) onUpdated()
    }
    // 실패 안내는 토스트로 한 번 보여 주고 지운다.
    val toast = remember { ToastState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            viewModel.onErrorShown()
            toast.show(it, ToastType.Error)
        }
    }
    Box(Modifier.fillMaxSize()) {
    MeetingCreateContent(
        state = state,
        onClose = onClose,
        onSportSelect = viewModel::onSportSelect,
        onTitleChange = viewModel::onTitleChange,
        onDateSelect = viewModel::onDateSelect,
        onCalendarClick = viewModel::onCalendarClick,
        onTimeClick = viewModel::onTimeClick,
        onCriteriaClick = viewModel::onCriteriaClick,
        onPlaceClick = if (state.isEditMode) viewModel::onDetailAddressClick else { { viewModel.onPlaceClick(); onPlaceClick() } },
        onCapacityClick = viewModel::onCapacityClick,
        onDescriptionClick = viewModel::onDescriptionClick,
        onSubmit = viewModel::onSubmit,
    )
    ToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp))
    }

    if (state.showCalendar) {
        CalendarDialog(onPick = viewModel::onCalendarPick, onDismiss = viewModel::onCalendarDismiss)
    }
    when (state.openSheet) {
        CreateSheet.RUNNING_CRITERIA -> RunningCriteriaBottomSheet(
            initial = state.runningCriteria,
            onApply = viewModel::onRunningCriteriaApply,
            onDismiss = viewModel::onSheetDismiss,
        )
        CreateSheet.HIKING_CRITERIA -> HikingCriteriaBottomSheet(
            initial = state.hikingStyle,
            onApply = viewModel::onHikingCriteriaApply,
            onDismiss = viewModel::onSheetDismiss,
        )
        CreateSheet.CAPACITY -> CapacityBottomSheet(
            range = state.capacityRange,
            initialCapacity = state.capacity,
            initialGender = state.gender,
            hostGender = state.hostGender,
            onApply = viewModel::onCapacityApply,
            onDismiss = viewModel::onSheetDismiss,
        )
        CreateSheet.DETAIL_ADDRESS -> DetailAddressBottomSheet(
            placeName = state.place?.name.orEmpty(),
            initial = state.place?.detail.orEmpty(),
            onApply = viewModel::onDetailAddressApply,
            onDismiss = viewModel::onSheetDismiss,
        )
        CreateSheet.HEALTH_CRITERIA -> HealthCriteriaBottomSheet(
            initial = state.healthCriteria,
            onApply = viewModel::onHealthCriteriaApply,
            onDismiss = viewModel::onSheetDismiss,
        )
        CreateSheet.DESCRIPTION -> DescriptionBottomSheet(
            initial = state.description,
            onApply = viewModel::onDescriptionApply,
            onDismiss = viewModel::onSheetDismiss,
        )
        CreateSheet.TIME -> TimeBottomSheet(
            initialStartMinutes = state.startMinutes,
            initialDurationMinutes = state.durationMinutes,
            onApply = viewModel::onTimeApply,
            onDismiss = viewModel::onSheetDismiss,
        )
        null -> Unit
    }
}

@Composable
private fun MeetingCreateContent(
    state: MeetingCreateState,
    onClose: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDateSelect: (Int) -> Unit,
    onCalendarClick: () -> Unit,
    onTimeClick: () -> Unit,
    onCriteriaClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onCapacityClick: () -> Unit,
    onDescriptionClick: () -> Unit,
    onSubmit: () -> Unit,
    onSportSelect: (com.example.myapplication.data.model.Sport) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Header(title = if (state.isEditMode) "모임 수정" else "모임 개설", onClose = onClose)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppSpace.x4,
                    end = AppSpace.x4,
                    top = AppSpace.xs,
                    bottom = AppSpace.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpace.xxl),
        ) {
            if (state.lockedCore) LockedNotice()
            // 종목 선택은 새로 만들 때만. 수정에서는 종목을 바꿀 수 없다.
            if (!state.isEditMode) SportPicker(selected = state.sport, onSelect = onSportSelect)
            TitleSection(title = state.title, onTitleChange = onTitleChange)
            DateTimeSection(
                state = state,
                onDateSelect = onDateSelect,
                onCalendarClick = onCalendarClick,
                onTimeClick = onTimeClick,
            )
            DetailCard(
                rows = listOf(
                    DetailRow(state.criteriaTitle, state.criteriaSummary, onCriteriaClick, locked = state.lockedCore),
                    // 확정 참가자가 있어도 상세주소는 고칠 수 있어 장소 행은 잠그지 않는다.
                    DetailRow("장소", state.placeSummary, onPlaceClick),
                    DetailRow("정원 · 성별", state.capacitySummary, onCapacityClick, locked = state.lockedCore),
                    DetailRow("설명", state.descriptionSummary, onDescriptionClick),
                ),
            )
        }

        BottomBar(
            text = if (state.isEditMode) "수정 완료" else "모임 개설하기",
            enabled = state.canSubmit && !state.isSubmitting,
            onSubmit = onSubmit,
        )
    }
}

@Composable
private fun Header(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "닫기",
            tint = AppColor.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onClose),
        )
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.TextPrimary,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
}

@Composable
private fun TitleSection(title: String, onTitleChange: (String) -> Unit) {
    val textStyle = TextStyle(
        fontSize = 19.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        color = AppColor.TextPrimary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(AppSpace.lg)) {
        SectionLabel("제목")
        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                textStyle = textStyle,
                cursorBrush = SolidColor(AppColor.AccentStrong),
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            // 빈 제목 프레임은 디자인에 없음. 안내 문구는 임시.
                            Text(text = "제목을 입력해 주세요", style = textStyle.copy(color = AppColor.TextSecondary))
                        }
                        innerTextField()
                    }
                },
            )
            HorizontalLine(AppColor.Border)
        }
    }
}

@Composable
private fun DateTimeSection(
    state: MeetingCreateState,
    onDateSelect: (Int) -> Unit,
    onCalendarClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Column {
        SectionLabel("날짜 · 시간")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpace.xxl),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.dateOptions.forEachIndexed { index, option ->
                DateChip(
                    option = option,
                    selected = index == state.selectedDateIndex,
                    locked = state.lockedCore,
                    onClick = { onDateSelect(index) },
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .clickable(enabled = !state.lockedCore, onClick = onCalendarClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = "달력에서 날짜 선택",
                    tint = AppColor.TextBody,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        HorizontalLine(AppColor.Divider)
        ValueRow(
            label = "시간",
            value = state.timeSummary,
            onClick = onTimeClick,
            modifier = Modifier.padding(vertical = AppSpace.xxl),
            locked = state.lockedCore,
        )
    }
}

@Composable
private fun DateChip(option: DateOption, selected: Boolean, locked: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.pill)
    Column(
        modifier = Modifier
            .width(48.dp)
            .height(60.dp)
            .clip(shape)
            .background(if (selected) (if (locked) AppColor.AccentSoft else AppColor.Accent) else Color.Transparent)
            .clickable(enabled = !locked, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpace.xxs, Alignment.CenterVertically),
    ) {
        Text(
            text = option.topLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) AppColor.OnAccent else AppColor.TextSecondary,
        )
        Text(
            text = option.dateLabel,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) AppColor.OnAccent else AppColor.TextPrimary,
        )
    }
}

private data class DetailRow(val label: String, val value: String?, val onClick: () -> Unit, val locked: Boolean = false)

@Composable
private fun DetailCard(rows: List<DetailRow>) {
    val shape = RoundedCornerShape(AppRadius.lg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColor.Border, shape)
            .clip(shape)
            .padding(horizontal = AppSpace.xxl),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalLine(AppColor.DividerSoft)
            ValueRow(
                label = row.label,
                value = row.value,
                onClick = row.onClick,
                modifier = Modifier.height(52.dp),
                locked = row.locked,
            )
        }
    }
}

// 왼쪽 라벨, 오른쪽 값(없으면 '미입력'), 끝에 화살표. 시간 행과 카드 안 행이 같이 쓴다.
@Composable
private fun ValueRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false, // 수정 모드에서 확정 참가자가 있으면 바꿀 수 없다
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !locked, onClick = onClick)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.lg),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.TextPrimary,
            maxLines = 1,
        )
        Text(
            text = value ?: "미입력",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (value == null || locked) AppColor.TextSecondary else AppColor.TextBody,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (locked) {
            Text(text = "잠김", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextDisabled)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = AppColor.IconMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// 수정 모드 + 확정 참가자 있음: 무엇이 잠기는지 알려 준다. (service-rules 4장)
@Composable
private fun LockedNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppColor.SurfaceMuted)
            .padding(AppSpace.xxl),
        verticalArrangement = Arrangement.spacedBy(AppSpace.xxs),
    ) {
        Text("확정된 메이트가 있어요", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Text(
            "제목, 설명, 상세주소만 바꿀 수 있어요. 일시·기준·정원은 공지로 안내해 주세요.",
            fontSize = 13.sp, lineHeight = 19.sp, color = AppColor.TextSecondary,
        )
    }
}

@Composable
private fun BottomBar(text: String, enabled: Boolean, onSubmit: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.md)
    Column(modifier = Modifier.background(AppColor.Surface)) {
        HorizontalLine(AppColor.DividerSoft)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpace.x4,
                    end = AppSpace.x4,
                    top = AppSpace.xl,
                    bottom = AppSpace.xxl,
                )
                .height(54.dp)
                .clip(shape)
                .background(if (enabled) AppColor.Accent else AppColor.DisabledBg)
                .clickable(enabled = enabled, onClick = onSubmit),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) AppColor.OnAccent else AppColor.TextDisabled,
            )
        }
    }
}

@Composable
private fun HorizontalLine(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

// ---------- 미리보기 (디자인 프레임과 같은 값) ----------

private val previewWeekdayDates = listOf(
    DateOption(2026, 9, 8, "오늘"),
    DateOption(2026, 9, 9, "화"),
    DateOption(2026, 9, 10, "수"),
    DateOption(2026, 9, 11, "목"),
    DateOption(2026, 9, 12, "금"),
)

private val preview10a = MeetingCreateState(
    title = "퇴근 후 가슴·팔 같이 치실 분 💪",
    dateOptions = previewWeekdayDates,
    selectedDateIndex = 0,
    startMinutes = 19 * 60,
    durationMinutes = 90,
    healthCriteria = HealthCriteria(
        parts = listOf(BodyPart.CHEST, BodyPart.ARM),
        showMyRecord = true,
        myRecord = ExerciseRecord(benchPressKg = 80, armLevel = SkillLevel.INTERMEDIATE),
    ),
    place = PlaceSelection("스포애니 합정점", "3층 프리웨이트 존"),
    capacity = 2,
    gender = GenderLimit.ANYONE,
    description = "벤치 5×5 먼저 하고 팔 운동 같이 해요",
)

@Composable
private fun PreviewContent(state: MeetingCreateState) {
    MeetingCreateContent(
        state = state,
        onClose = {},
        onTitleChange = {},
        onDateSelect = {},
        onCalendarClick = {},
        onTimeClick = {},
        onCriteriaClick = {},
        onPlaceClick = {},
        onCapacityClick = {},
        onDescriptionClick = {},
        onSubmit = {},
    )
}

@Preview(name = "10a 헬스", widthDp = 360, heightDp = 800)
@Composable
private fun Preview10a() = PreviewContent(preview10a)

@Preview(name = "10a-2 헬스 기록 숨김", widthDp = 360, heightDp = 800)
@Composable
private fun Preview10a2() =
    PreviewContent(preview10a.copy(healthCriteria = preview10a.healthCriteria.copy(showMyRecord = false)))

@Preview(name = "빈 상태", widthDp = 360, heightDp = 800)
@Composable
private fun PreviewEmpty() = PreviewContent(MeetingCreateState(dateOptions = previewWeekdayDates, selectedDateIndex = 0))

// 달력 창: 오늘부터 고를 수 있다. 확인하면 그 날짜 칩이 선택된다.
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDialog(onPick: (year: Int, month: Int, day: Int) -> Unit, onDismiss: () -> Unit) {
    val todayStart = remember {
        val utc = java.util.TimeZone.getTimeZone("UTC")
        val now = java.util.Calendar.getInstance()
        java.util.Calendar.getInstance(utc).apply {
            clear()
            set(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH), now.get(java.util.Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    val pickerState = androidx.compose.material3.rememberDatePickerState(
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayStart
        },
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
                        onPick(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
                    }
                },
            ) { Text("확인") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("취소") } },
    ) { androidx.compose.material3.DatePicker(state = pickerState) }
}
