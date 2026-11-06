package com.example.myapplication.ui.meeting.detail

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.Gender
import com.example.myapplication.data.model.GenderLimit
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.ui.explore.filter.filterPartOrder
import com.example.myapplication.ui.explore.filter.myValue
import com.example.myapplication.ui.explore.filter.paceText
import com.example.myapplication.ui.record.bodyPartLabel
import com.example.myapplication.ui.record.hikingStyleLabel
import com.example.myapplication.ui.record.skillLevelLabel
import java.util.Calendar
import java.util.TimeZone

// 참가 신청(6a, 6b)의 규칙. 한마디는 선택이고 최대 200자(임시값)다. (service-rules 5장)

const val MAX_MESSAGE_LENGTH = 200

private val weekdayNames = listOf("일", "월", "화", "수", "목", "금", "토")

// 입력은 200자까지만 받는다.
fun limitMessage(text: String): String = text.take(MAX_MESSAGE_LENGTH)

// 서버로 보내는 한마디. 비어 있거나 공백뿐이면 보내지 않는다(null).
fun messageToSend(text: String): String? = text.trim().ifBlank { null }

// "9/8 (월) 오전 7:00 · 북한산 우이동 들머리"
fun applySubtitle(meeting: Meeting, timeZone: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(timeZone).apply { timeInMillis = meeting.startAtMillis }
    val hour24 = c.get(Calendar.HOUR_OF_DAY)
    val hour12 = (hour24 % 12).let { if (it == 0) 12 else it }
    val time = "${if (hour24 < 12) "오전" else "오후"} $hour12:${c.get(Calendar.MINUTE).toString().padStart(2, '0')}"
    val date = "${c.get(Calendar.MONTH) + 1}/${c.get(Calendar.DAY_OF_MONTH)} (${weekdayNames[c.get(Calendar.DAY_OF_WEEK) - 1]})"
    return listOf("$date $time", meeting.place.name.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · ")
}

// 호스트에게 전달되는 '이 종목의 내 기록' 요약. 기록이 하나도 없으면 null(미설정)이다.
// 헬스는 이 모임이 보는 부위만, 러닝은 페이스, 등산은 성향을 보여 준다.
fun myRecordSummary(meeting: Meeting, myRecord: ExerciseRecord?): String? = when (val c = meeting.criteria) {
    is MeetingCriteria.Health -> filterPartOrder.filter { it in c.parts }.mapNotNull { part ->
        when (part) {
            BodyPart.ARM -> myRecord?.armLevel?.let { "${bodyPartLabel(part)} ${skillLevelLabel(it)}" }
            BodyPart.BACK -> myValue(part, myRecord)?.let { "${bodyPartLabel(part)} ${it}회" }
            else -> myValue(part, myRecord)?.let { "${bodyPartLabel(part)} ${it}kg" }
        }
    }.joinToString(" · ").ifEmpty { null }
    is MeetingCriteria.Running -> myRecord?.runningPaceSec?.let { "${paceText(it)}/km" }
    is MeetingCriteria.Hiking -> myRecord?.hikingStyle?.let(::hikingStyleLabel)
}

// 성별 조건에 맞는지. 서버가 막는 경우이지만 미리 알려 주면 헛걸음을 줄인다. 내 성별을 모르면 막지 않는다. (5장)
fun genderAllowed(limit: GenderLimit, myGender: Gender?): Boolean = when (limit) {
    GenderLimit.ANYONE -> true
    GenderLimit.FEMALE_ONLY -> myGender == null || myGender == Gender.FEMALE
    GenderLimit.MALE_ONLY -> myGender == null || myGender == Gender.MALE
}
