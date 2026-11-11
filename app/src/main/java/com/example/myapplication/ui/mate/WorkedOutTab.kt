package com.example.myapplication.ui.mate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.model.Sport
import com.example.myapplication.data.model.SportSupport
import com.example.myapplication.data.model.WorkedOutPerson
import com.example.myapplication.ui.common.sportLabel
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone
import com.example.myapplication.ui.theme.SuitFontFamily

// 함께 운동한 사람 탭 ← 11b
// 이름 찾기 아래 종목 칩(전체·헬스·러닝·등산, 숫자는 그 종목을 함께한 사람 수) ← 11b-2.
// 고른 종목에 사람이 없으면 목록 자리에 안내 문구 ← 11b-3.
@Composable
fun WorkedOutTab(
    people: List<WorkedOutPerson>,
    emptyText: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sportFilter: Sport?,
    sportCount: (Sport?) -> Int,
    onSportFilterSelect: (Sport?) -> Unit,
    onPersonClick: (userId: String) -> Unit, // 15 프로필
    onRequest: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    Column(modifier = modifier) {
        Text(
            text = "모임에서 실제로 만난 사람만 표시돼요",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppColor.TextDisabled,
            modifier = Modifier.padding(horizontal = AppSpace.x4, vertical = AppSpace.lg),
        )
        SearchField(value = searchQuery, onValueChange = onSearchChange)
        SportChips(selected = sportFilter, count = sportCount, onSelect = onSportFilterSelect)
        if (people.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = emptyText, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
            }
            return@Column
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpace.x4),
        ) {
            people.forEachIndexed { index, person ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColor.DividerSoft))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPersonClick(person.userId) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpace.xl),
                    ) {
                        MateAvatar(userId = person.userId, nickname = person.nickname)
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpace.xxs)) {
                            Text(text = person.nickname, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                            Text(text = person.lastText(timeZone), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColor.TextSecondary)
                        }
                    }
                    RelationAction(relation = person.relation, onRequest = { onRequest(person.userId) })
                }
            }
        }
    }
}

// 11b-2 종목 칩. 고른 칩은 라임 알약, 나머지는 글자만. 서버가 받는 종목이 하나뿐이면 거를 게 없어 그리지 않는다.
@Composable
private fun SportChips(selected: Sport?, count: (Sport?) -> Int, onSelect: (Sport?) -> Unit) {
    if (!SportSupport.hasChoice) return
    val options = listOf<Sport?>(null) + SportSupport.supported
    Row(
        modifier = Modifier
            .padding(start = AppSpace.x4 - AppSpace.xl, end = AppSpace.x4, top = AppSpace.xs, bottom = AppSpace.xs)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpace.xs),
    ) {
        options.forEach { sport ->
            val isSelected = sport == selected
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(AppRadius.pill))
                    .background(if (isSelected) AppColor.Accent else AppColor.Surface)
                    .clickable { onSelect(sport) }
                    .padding(horizontal = AppSpace.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.xs),
            ) {
                Text(
                    text = if (sport == null) "전체" else sportLabel(sport),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) AppColor.OnAccent else AppColor.TextSecondary,
                )
                Text(
                    text = count(sport).toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) AppColor.OnAccent else AppColor.TextDisabled,
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val style = TextStyle(fontFamily = SuitFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
    Row(
        modifier = Modifier
            .padding(start = AppSpace.x4, end = AppSpace.x4, bottom = AppSpace.md)
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppColor.ChipNeutralBg)
            .padding(horizontal = AppSpace.xxl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = AppColor.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            singleLine = true,
            cursorBrush = SolidColor(AppColor.AccentStrong),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(text = "이름으로 찾기", style = style.copy(color = AppColor.IconMuted))
                    inner()
                }
            },
        )
    }
}
