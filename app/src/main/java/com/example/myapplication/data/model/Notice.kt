package com.example.myapplication.data.model

data class Notice(
    val id: String = "",
    val meetingId: String = "",
    val title: String = "",
    val body: String = "",
    val createdAtMillis: Long = 0L,
    val isEdited: Boolean = false, // 수정하면 '수정됨' 표시
)
