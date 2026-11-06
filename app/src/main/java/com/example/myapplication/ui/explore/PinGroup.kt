package com.example.myapplication.ui.explore

import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingStatus

// 같은 지번의 모임을 묶은 핀 하나 (service-rules 2장 '핀').
data class PinGroup(
    val key: String,              // 지번. 지번이 비어 있으면 모임 id로 따로 둔다.
    val meetings: List<Meeting>,  // 거리순
    val latitude: Double,
    val longitude: Double,
) {
    val count: Int get() = meetings.size
    val isMultiple: Boolean get() = meetings.size > 1
}

// 거리순. 거리를 모르는 모임은 맨 뒤로 보낸다.
fun List<Meeting>.sortedByDistance(): List<Meeting> =
    sortedBy { it.distanceMeters ?: Int.MAX_VALUE }

// 지도에는 모집중 모임만 나온다. 마감·진행중·종료·취소는 뺀다.
fun List<Meeting>.onlyRecruiting(): List<Meeting> =
    filter { it.status == MeetingStatus.RECRUITING }

fun pinKeyOf(meeting: Meeting): String =
    meeting.place.lotAddress.ifBlank { "meeting:${meeting.id}" }

// 지번이 같은 모임은 핀 하나로 묶는다. 핀 위치는 묶음 안 첫 모임의 좌표다.
fun List<Meeting>.groupIntoPins(): List<PinGroup> =
    sortedByDistance()
        .groupBy(::pinKeyOf)
        .map { (key, meetings) ->
            PinGroup(
                key = key,
                meetings = meetings,
                latitude = meetings.first().place.latitude,
                longitude = meetings.first().place.longitude,
            )
        }
