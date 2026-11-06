package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.CHECK_IN_RADIUS_METERS
import com.example.myapplication.ui.explore.formatDistance
import java.util.Calendar
import java.util.TimeZone

// 체크인 확인창(9a, 9b)의 상태와 문구 규칙. (service-rules 7장)

sealed interface CheckInPrompt {
    data object Locating : CheckInPrompt                              // 내 위치를 가져오는 중
    data object NoLocation : CheckInPrompt                            // 위치 권한이 없거나 위치를 못 얻음
    data class Ready(val distanceMeters: Int) : CheckInPrompt         // 거리를 알았다. 범위 안이면 9a, 밖이면 9b
    data class Submitting(val distanceMeters: Int) : CheckInPrompt    // 체크인을 보내는 중
    data object Failed : CheckInPrompt                                // 체크인을 보내지 못함(네트워크 등)
}

// 서버는 모임 장소 50m 안에서만 성공시킨다. 앱도 같은 거리로 확인창의 버튼을 미리 막는다.
fun canCheckIn(distanceMeters: Int): Boolean = distanceMeters <= CHECK_IN_RADIUS_METERS

// 1km가 넘으면 km로 줄여 보인다. (10655m → 10.7km)
private fun distanceText(meters: Int): String = formatDistance(meters) ?: "${meters}m"

// 확인창에 보이는 글자. distanceLine이 있으면 '모임 장소에서 40m' 상자를 보여 준다.
data class CheckInTexts(
    val title: String,
    val message: String?,
    val distanceLine: String?,
    val confirmText: String,
    val dismissText: String,
    val confirmEnabled: Boolean,
    val canRefresh: Boolean,       // 상자 오른쪽의 새로고침(거리를 다시 재기)
)

fun checkInTextsOf(dialog: CheckInPrompt): CheckInTexts = when (dialog) {
    CheckInPrompt.Locating -> CheckInTexts(
        "위치를 확인하고 있어요", "잠시만 기다려주세요", null, "체크인", "닫기", confirmEnabled = false, canRefresh = false,
    )
    CheckInPrompt.NoLocation -> CheckInTexts(
        "위치를 확인할 수 없어요", "위치 권한과 위치 서비스를 켜고 다시 시도해주세요", null, "다시 시도", "닫기", confirmEnabled = true, canRefresh = false,
    )
    is CheckInPrompt.Ready ->
        if (canCheckIn(dialog.distanceMeters)) CheckInTexts(
            "체크인할까요?", "지금 체크인할 수 있어요", "모임 장소에서 ${distanceText(dialog.distanceMeters)}", "체크인", "아니요",
            confirmEnabled = true, canRefresh = true,
        ) else CheckInTexts(
            "아직 조금 멀어요", "반경 ${CHECK_IN_RADIUS_METERS}m 안에서 체크인할 수 있어요", "모임 장소에서 ${distanceText(dialog.distanceMeters)}", "체크인", "닫기",
            confirmEnabled = false, canRefresh = true,
        )
    is CheckInPrompt.Submitting -> CheckInTexts(
        "체크인하는 중이에요", null, "모임 장소에서 ${distanceText(dialog.distanceMeters)}", "체크인 중", "닫기", confirmEnabled = false, canRefresh = false,
    )
    CheckInPrompt.Failed -> CheckInTexts(
        "체크인하지 못했어요", "잠시 후 다시 시도해주세요", null, "다시 시도", "닫기", confirmEnabled = true, canRefresh = false,
    )
}

// "오전 7:02" (9c 체크인 완료 아래 시각)
fun formatCheckInTime(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
    val hour24 = c.get(Calendar.HOUR_OF_DAY)
    val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
    return "${if (hour24 < 12) "오전" else "오후"} $hour12:${c.get(Calendar.MINUTE).toString().padStart(2, '0')}"
}
