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
import com.example.myapplication.data.model.WorkedOutPerson
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppRadius
import com.example.myapplication.ui.theme.AppSpace
import java.util.TimeZone

// 함께 운동한 사람 탭 ← 11b
// 종목 필터(11b-2 헬스 필터, 11b-3 필터 결과 없음)는 헬스만 하는 범위라 만들지 않는다. 이름 찾기는 둔다.
@Composable
fun WorkedOutTab(
    people: List<WorkedOutPerson>,
    emptyText: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
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

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColor.TextPrimary)
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
