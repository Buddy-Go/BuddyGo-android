package com.example.myapplication.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.Sport
import com.example.myapplication.ui.record.SportIcon
import com.example.myapplication.ui.theme.AppColor
import com.example.myapplication.ui.theme.AppSpace
import com.example.myapplication.ui.theme.MyApplicationTheme

// 3-4-After 핀 색 체계
enum class PinLook {
    Default,   // 종목 색 (섞인 묶음은 회색)
    Selected,  // 선택된 핀만 라임
    Dimmed,    // 다른 핀이 선택되어 있을 때 회색으로 죽임
}

// 핀 한 개가 어떻게 보일지 정한다. sport가 null이면 종목이 섞인 묶음이다.
data class PinStyle(val sport: Sport?, val look: PinLook)

fun pinStyleOf(pin: PinGroup, selectedKey: String?): PinStyle {
    val sports = pin.meetings.map { it.sport }.distinct()
    val look = when {
        selectedKey == null -> PinLook.Default
        pin.key == selectedKey -> PinLook.Selected
        else -> PinLook.Dimmed
    }
    return PinStyle(sports.singleOrNull(), look)
}

private val MixedGray = Color(0xFF9CA3AF)
private val DimmedGray = Color(0xFF5B6270)
private val DimmedMixedGray = Color(0xFF9EA3AB)

// 핀 배경색과 아이콘·글자색. Compose 핀과 지도에 올리는 비트맵 핀이 같이 쓴다.
internal fun pinBackgroundColor(style: PinStyle): Color = when (style.look) {
    PinLook.Selected -> AppColor.Accent
    PinLook.Dimmed -> if (style.sport == null) DimmedMixedGray else DimmedGray
    PinLook.Default -> style.sport?.let { toneOf(it).solid } ?: MixedGray
}

internal fun pinContentColor(style: PinStyle): Color =
    if (style.look == PinLook.Selected) AppColor.TextPrimary else Color.White

// 핀은 종목 아이콘만 보여 주고, 같은 지번에 모임이 여럿이면 "+모임 수"를 붙인다.
@Composable
fun MapPin(
    style: PinStyle,
    count: Int,
    modifier: Modifier = Modifier,
    hasFavorite: Boolean = false,
) {
    val background = pinBackgroundColor(style)
    val content = pinContentColor(style)
    Box(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(if (count > 1) 20.dp else 22.dp))
                    .background(background)
                    .height(if (count > 1) 32.dp else 44.dp)
                    .padding(horizontal = if (count > 1) 14.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpace.md),
            ) {
                SportIcon(style.sport ?: Sport.HEALTH, content, size = if (count > 1) 16.dp else 20.dp)
                if (count > 1) {
                    Box(Modifier.width(1.dp).height(14.dp).background(content.copy(alpha = .5f)))
                    Text("+$count", color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(Modifier.size(width = 10.dp, height = 6.dp).background(background, RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp)))
        }
        if (hasFavorite) {
            Box(
                Modifier.align(Alignment.TopEnd).size(18.dp).clip(RoundedCornerShape(9.dp)).background(Color.White),
                contentAlignment = Alignment.Center,
            ) { HeartIcon(filled = true, color = Color(0xFFE5383B), size = 11.dp) }
        }
    }
}

// 묶음 안에 찜한 모임이 하나라도 있으면 핀에 하트를 단다.
fun PinGroup.hasFavorite(): Boolean = meetings.any(Meeting::isFavorite)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun MapPinPreview() {
    MyApplicationTheme(dynamicColor = false) {
        Row(Modifier.padding(AppSpace.xxl), horizontalArrangement = Arrangement.spacedBy(AppSpace.xxl)) {
            MapPin(PinStyle(Sport.HEALTH, PinLook.Default), count = 3)
            MapPin(PinStyle(Sport.RUNNING, PinLook.Default), count = 1)
            MapPin(PinStyle(Sport.HIKING, PinLook.Default), count = 1, hasFavorite = true)
            MapPin(PinStyle(null, PinLook.Default), count = 4)
            MapPin(PinStyle(Sport.HEALTH, PinLook.Selected), count = 3)
            MapPin(PinStyle(Sport.RUNNING, PinLook.Dimmed), count = 1)
        }
    }
}
