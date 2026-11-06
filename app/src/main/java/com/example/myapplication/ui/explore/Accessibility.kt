package com.example.myapplication.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role

// 스크린 리더(TalkBack)용 설명. 아이콘만 있는 버튼은 눈에 보이는 글자가 없어서 이름을 따로 붙여야 읽힌다.

// 아이콘만 있는 버튼: 이름과 '버튼' 역할을 붙인다.
fun Modifier.iconButton(label: String, onClick: () -> Unit): Modifier =
    clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }

// 글자가 있는 선택 칸(칩·카드·날짜·시간대): 눌렀을 때 하는 일과 지금 선택됐는지를 알려 준다.
fun Modifier.selectableItem(selected: Boolean, onClick: () -> Unit, onClickLabel: String? = null): Modifier =
    clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
        .semantics { this.selected = selected }

// 찜 하트 버튼의 이름. 지금 상태가 아니라 누르면 일어날 일을 알려 준다.
fun favoriteLabel(isFavorite: Boolean): String = if (isFavorite) "찜 해제" else "찜하기"

// 알림 벨의 이름. 읽지 않은 알림이 있으면 빨간 점이 보이는 것과 같이 알려 준다.
fun notificationLabel(hasUnread: Boolean): String = if (hasUnread) "알림, 읽지 않은 알림이 있어요" else "알림"
