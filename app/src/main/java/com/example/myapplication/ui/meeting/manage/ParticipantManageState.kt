package com.example.myapplication.ui.meeting.manage

import com.example.myapplication.data.model.ExerciseRecord
import com.example.myapplication.data.model.JoinRequest
import com.example.myapplication.data.model.JoinRequestStatus
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingStatus
import com.example.myapplication.data.model.SkillLevel
import com.example.myapplication.data.model.User

// 참가자 관리 화면(7a~7d)의 상태. 호스트 전용.
// 공용 모델(Meeting, JoinRequest, User)을 화면에 보일 모양으로 바꿔 담는다. 수락·마감 판정은 서버가 한다.

enum class ManageTab {
    CONFIRMED, // 7b
    APPLICANTS, // 7a, 7c
}

// 확정 참가자 한 명 (7b, 7d)
data class ConfirmedMember(
    val userId: String,
    val nickname: String,
    val mannerTemperature: Double,
    val isHost: Boolean = false,
) {
    val mannerLabel: String get() = mannerText(mannerTemperature)
}

// 대기 중인 신청 한 건 (7a). 호스트에게 가는 정보: 닉네임, 매너온도, 내 헬스 기록, 한마디 (service-rules 5장)
// 2026-10-30 범위 변경으로 헬스만 있다. 디자인 7a의 '· 취미'(등산 성향) 자리에 헬스 기록 요약을 보여준다.
data class Applicant(
    val applicationId: String,
    val userId: String,
    val nickname: String,
    val mannerTemperature: Double,
    val healthRecord: String?, // 예: "가슴 80kg · 팔 중급". 기록이 없으면 null → '미설정'
    val message: String?, // 한마디(선택)
) {
    val recordLabel: String get() = healthRecord ?: "미설정"
    val mannerLabel: String get() = mannerText(mannerTemperature)
}

// "매너온도 44.1°C" (소수 첫째 자리)
internal fun mannerText(temperature: Double): String {
    val tenths = Math.round(temperature * 10)
    return "매너온도 ${tenths / 10}.${Math.abs(tenths % 10)}°C"
}

data class ParticipantManageState(
    val meetingId: String = "",
    val capacity: Int = 0, // 호스트 포함. 헬스 2~3명
    val isClosed: Boolean = false, // 마감 = 확정 인원이 정원과 같음 (service-rules 5장)
    val confirmed: List<ConfirmedMember> = emptyList(),
    val applicants: List<Applicant> = emptyList(),
    val selectedTab: ManageTab = ManageTab.APPLICANTS,
    val processingApplicationId: String? = null, // 수락·거절 요청 중인 신청 (버튼 중복 클릭 방지)
) {
    val confirmedCount: Int get() = confirmed.size
    val applicantCount: Int get() = applicants.size

    // 7d: 마감되면 탭 없이 확정 명단만 보여준다.
    val showTabs: Boolean get() = !isClosed

    // 마감 후에는 신청 탭이 없으므로 항상 확정 명단
    val visibleTab: ManageTab get() = if (isClosed) ManageTab.CONFIRMED else selectedTab

    val confirmedHint: String
        get() = if (isClosed) "모집이 마감돼서 더 이상 신청을 받지 않아요" else "참가자에게는 마감 후 명단이 공개돼요"
}

// ---------- 공용 모델 → 화면 상태 ----------

// 모임·신청 목록·확정자 정보를 화면 상태로 만든다. 호스트가 명단 맨 앞.
fun buildParticipantState(
    meeting: Meeting,
    requests: List<JoinRequest>,
    usersById: Map<String, User>,
    selectedTab: ManageTab = ManageTab.APPLICANTS,
): ParticipantManageState {
    val confirmedIds = listOf(meeting.hostId) + meeting.confirmedIds.filter { it != meeting.hostId }
    return ParticipantManageState(
        meetingId = meeting.id,
        capacity = meeting.capacity,
        isClosed = meeting.status == MeetingStatus.CLOSED || meeting.isFull,
        confirmed = confirmedIds.map { id ->
            val user = usersById[id]
            ConfirmedMember(
                userId = id,
                nickname = user?.nickname ?: if (id == meeting.hostId) meeting.hostNickname else "",
                mannerTemperature = (user?.mannerTemperature ?: meeting.hostMannerTemperature).toDouble(),
                isHost = id == meeting.hostId,
            )
        },
        applicants = requests
            .filter { it.status == JoinRequestStatus.PENDING }
            .map { r ->
                Applicant(
                    applicationId = r.id,
                    userId = r.applicantId,
                    nickname = r.applicantNickname,
                    mannerTemperature = r.applicantMannerTemperature.toDouble(),
                    healthRecord = healthRecordSummary(r.applicantRecord),
                    message = r.message,
                )
            },
        selectedTab = selectedTab,
    )
}

// "가슴 80kg · 등 8회 · 팔 중급". 헬스 기록이 하나도 없으면 null (service-rules 5장: 없으면 '미설정')
fun healthRecordSummary(record: ExerciseRecord): String? {
    val parts = listOfNotNull(
        record.benchPressKg?.let { "가슴 ${it}kg" },
        record.pullUpCount?.let { "등 ${it}회" },
        record.squatKg?.let { "하체 ${it}kg" },
        record.overheadPressKg?.let { "어깨 ${it}kg" },
        record.armLevel?.let {
            "팔 " + when (it) {
                SkillLevel.BEGINNER -> "초급"
                SkillLevel.INTERMEDIATE -> "중급"
                SkillLevel.ADVANCED -> "고급"
            }
        },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
