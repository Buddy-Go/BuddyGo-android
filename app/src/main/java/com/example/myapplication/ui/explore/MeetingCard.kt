package com.example.myapplication.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.mock.MockMeetings
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.record.bodyPartLabel
import com.example.myapplication.ui.record.hikingStyleLabel
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 종목별 색 (3-After 카드 칩, 3-4-After 핀). 이 화면에서만 쓴다.
internal data class SportTone(val solid: Color, val soft: Color, val text: Color)

internal fun toneOf(sport: Sport): SportTone = when (sport) {
    Sport.HEALTH -> SportTone(Color(0xFFF5A524), Color(0xFFFDEBD0), Color(0xFFB45309))
    Sport.RUNNING -> SportTone(Color(0xFF3B82F6), Color(0xFFE0EBFF), Color(0xFF1D4ED8))
    Sport.HIKING -> SportTone(Color(0xFF2F7D4F), Color(0xFFDDF3E4), Color(0xFF166534))
}

fun sportLabel(sport: Sport): String = when (sport) {
    Sport.HEALTH -> "헬스"
    Sport.RUNNING -> "러닝"
    Sport.HIKING -> "등산"
}

// 3-3-After 떠 있는 카드의 두 번째 칩. 기준을 짧게 요약한다.
fun criteriaChipText(meeting: Meeting): String = when (val c = meeting.criteria) {
    is MeetingCriteria.Health -> c.parts.sortedBy { it.ordinal }.joinToString("·") { bodyPartLabel(it) }
    is MeetingCriteria.Running -> "${c.distanceKm.toString().removeSuffix(".0")}km"
    is MeetingCriteria.Hiking -> hikingStyleLabel(c.style)
}

// 3-After 목록 카드 / 3-3-After 떠 있는 카드.
// selected는 목록에서 선택된 모임(라임 테두리), showCriteriaChip은 떠 있는 카드에서만 켠다.
// showPlaceName은 시트 제목에 장소명이 이미 있을 때(3-5) 끈다.
@Composable
fun MeetingCard(
    meeting: Meeting,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showCriteriaChip: Boolean = false,
    showPlaceName: Boolean = true,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val shape = RoundedCornerShape(AppRadius.lg)
    val tone = toneOf(meeting.sport)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = AppColor.Surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AppColor.Accent else AppColor.Border),
    ) {
        Column(Modifier.padding(AppSpace.xxl), verticalArrangement = Arrangement.spacedBy(AppSpace.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(sportLabel(meeting.sport), tone.soft, tone.text)
                if (showCriteriaChip) {
                    Spacer(Modifier.width(AppSpace.md))
                    Chip(criteriaChipText(meeting), AppColor.ChipNeutralBg, AppColor.TextPrimary)
                }
                Spacer(Modifier.weight(1f))
                formatDistance(meeting.distanceMeters)?.let {
                    Text(it, color = AppColor.AccentStrong, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(AppSpace.md))
                }
                FavoriteButton(meeting.isFavorite, onToggleFavorite)
            }
            Text(
                meeting.title, color = AppColor.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatPlaceAndTime(meeting, nowMillis, showPlaceName),
                color = AppColor.TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                HostInfo(meeting)
                Spacer(Modifier.weight(1f))
                Capacity(meeting.confirmedCount, meeting.capacity)
            }
        }
    }
}

@Composable
private fun Chip(text: String, background: Color, textColor: Color) {
    Text(
        text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.dp, AppColor.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        HeartIcon(
            filled = isFavorite,
            color = if (isFavorite) Color(0xFFE5383B) else AppColor.TextDisabled,
        )
    }
}

@Composable
private fun HostInfo(meeting: Meeting) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(AppColor.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                meeting.hostNickname.take(1), color = AppColor.AccentStrong,
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(AppSpace.md))
        Text(meeting.hostNickname, color = AppColor.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(AppSpace.xl))
        ThermometerIcon(AppColor.AccentStrong)
        Spacer(Modifier.width(2.dp))
        Text("${meeting.hostMannerTemperature}°C", color = AppColor.AccentStrong, fontSize = 14.sp)
    }
}

// 확정/정원과 채워진 정도 막대
@Composable
private fun Capacity(confirmed: Int, capacity: Int) {
    val ratio = if (capacity > 0) (confirmed.toFloat() / capacity).coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$confirmed/$capacity", color = AppColor.AccentStrong, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(AppSpace.md))
        Box(Modifier.width(52.dp).height(6.dp).clip(CircleShape).background(AppColor.AccentSoft)) {
            Box(Modifier.fillMaxWidth(ratio).height(6.dp).clip(CircleShape).background(AppColor.Accent))
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MeetingCardPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Column(Modifier.padding(AppSpace.xxl), verticalArrangement = Arrangement.spacedBy(AppSpace.xl)) {
            MeetingCard(MockMeetings.recruiting, {}, {}, selected = true)
            MeetingCard(MockMeetings.pending.copy(isFavorite = true), {}, {}, showCriteriaChip = true)
        }
    }
}
