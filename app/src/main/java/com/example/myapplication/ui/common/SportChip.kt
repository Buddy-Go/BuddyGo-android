package com.example.myapplication.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.explore.toneOf
import com.example.myapplication.ui.theme.AppRadius

// 종목 이름 알약. 헬스(주황)·러닝(파랑)·등산(초록)으로 색을 나눈다. 메이트 목록(11a)과 프로필(15a)에서 쓴다.
@Composable
fun SportChip(sport: Sport, modifier: Modifier = Modifier, fontSize: Int = 12) {
    val tone = toneOf(sport)
    Text(
        text = sportLabel(sport),
        color = tone.text,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(tone.soft)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
