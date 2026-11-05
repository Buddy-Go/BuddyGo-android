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
) {
    val state = viewModel.state
    LaunchedEffect(state.createdMeetingId) {
        state.createdMeetingId?.let(onCreated)
    }
    MeetingCreateContent(
        state = state,
        onClose = onClose,
        onTitleChange = viewModel::onTitleChange,
        onDateSelect = viewModel::onDateSelect,
        onCalendarClick = viewModel::onCalendarClick,
        onTimeClick = viewModel::onTimeClick,
        onCriteriaClick = viewModel::onCriteriaClick,
        onPlaceClick = onPlaceClick,
        onCapacityClick = viewModel::onCapacityClick,
        onDescriptionClick = viewModel::onDescriptionClick,
        onSubmit = viewModel::onSubmit,
    )

    when (state.openSheet) {
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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Header(onClose = onClose)

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
            TitleSection(title = state.title, onTitleChange = onTitleChange)
            DateTimeSection(
                state = state,
                onDateSelect = onDateSelect,
                onCalendarClick = onCalendarClick,
                onTimeClick = onTimeClick,
            )
            DetailCard(
                rows = listOf(
                    DetailRow("기준", state.criteriaSummary, onCriteriaClick),
                    DetailRow("장소", state.placeSummary, onPlaceClick),
                    DetailRow("정원 · 성별", state.capacitySummary, onCapacityClick),
                    DetailRow("설명", state.descriptionSummary, onDescriptionClick),
                ),
            )
        }

        BottomBar(enabled = state.canSubmit && !state.isSubmitting, onSubmit = onSubmit)
    }
}

@Composable
private fun Header(onClose: () -> Unit) {
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
            text = "모임 개설",
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
                    onClick = { onDateSelect(index) },
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .clickable(onClick = onCalendarClick),
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
        )
    }
}

@Composable
private fun DateChip(option: DateOption, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppRadius.pill)
    Column(
        modifier = Modifier
            .width(48.dp)
            .height(60.dp)
            .clip(shape)
            .background(if (selected) AppColor.Accent else Color.Transparent)
            .clickable(onClick = onClick),
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

private data class DetailRow(val label: String, val value: String?, val onClick: () -> Unit)

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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            color = if (value == null) AppColor.TextSecondary else AppColor.TextBody,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = AppColor.IconMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun BottomBar(enabled: Boolean, onSubmit: () -> Unit) {
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
                text = "모임 개설하기",
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
