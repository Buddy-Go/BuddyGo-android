package com.example.myapplication.ui.meeting.detail

// 모임 상세의 호스트 영역(5a-H-After, 5a-H2-After, 5a-H4-After, 5a-H5)에 필요한 값.
// 화면 본체(MeetingDetailScreen, 문찬주)가 isHost일 때 이 값을 만들어 HostSection 부품들에 넘긴다.
// TODO(임시): 공용 Meeting 모델이 정해지면 MeetingDetailViewModel에서 이 값을 채운다.

data class HostNoticePreview(
    val noticeId: String,
    val timeText: String, // "9/7 21:30"
    val title: String,
    val body: String,
)

data class HostSectionState(
    val hostNickname: String,
    val confirmedNicknames: List<String>, // 확정 명단. 호스트가 맨 앞
    val capacity: Int, // 호스트 포함. 헬스 2~3명
    val isClosed: Boolean, // 마감 = 확정이 정원과 같음
    val isStartedOrLater: Boolean = false, // 진행중·종료면 수정·취소 불가 (service-rules 4장)
    val pendingApplicantCount: Int = 0,
    val latestNotice: HostNoticePreview? = null,
) {
    val confirmedCount: Int get() = confirmedNicknames.size

    // 호스트를 뺀 확정 참가자 수. 삭제/취소를 가르는 기준 (service-rules 4장)
    val confirmedOthersCount: Int get() = (confirmedCount - 1).coerceAtLeast(0)

    // "확정 2 / 3"
    val confirmedLabel: String get() = "확정 $confirmedCount / $capacity"

    // 진행 막대 0.0~1.0
    val progress: Float get() = if (capacity <= 0) 0f else (confirmedCount.toFloat() / capacity).coerceIn(0f, 1f)

    // "민준 외 2명" / 혼자면 "민준"
    val rosterSummary: String
        get() = when {
            confirmedNicknames.isEmpty() -> ""
            confirmedOthersCount == 0 -> confirmedNicknames.first()
            else -> "${confirmedNicknames.first()} 외 ${confirmedOthersCount}명"
        }

    // 5a-H 모집중 / 5a-H5 마감
    val rosterHint: String
        get() = if (isClosed) {
            "마감됐어요 · 참가자에게도 명단이 공개됐어요"
        } else {
            "인원이 다 차면 자동으로 마감돼요 · 참가자에게는 마감 후 명단이 공개돼요"
        }

    // 5a-H: 신청이 있으면 초록 버튼 + 숫자 / 5a-H2·5a-H5: 테두리 버튼
    val highlightManageButton: Boolean get() = !isClosed && pendingApplicantCount > 0

    // 공지가 있으면 '전체 >', 없으면 '작성 >' (5a-H4 / 5a-H)
    val noticeActionLabel: String get() = if (latestNotice == null) "작성" else "전체"

    // ⋯ 메뉴에 보일 항목 (메뉴 모양 디자인은 없음, 항목은 service-rules 4장 기준)
    val menuActions: List<HostAction>
        get() {
            if (isStartedOrLater) return emptyList()
            return listOf(
                HostAction.EDIT,
                if (confirmedOthersCount == 0) HostAction.DELETE else HostAction.CANCEL,
            )
        }

    // 5a-X2 "확정된 메이트 N명에게 취소 알림이 가요" (호스트 제외)
    val cancelNoticeText: String get() = "확정된 메이트 ${confirmedOthersCount}명에게 취소 알림이 가요"
}

enum class HostAction(val label: String) {
    EDIT("모임 수정"),
    CANCEL("모임 취소"), // 확정 참가자(호스트 제외) 1명 이상 → 5a-X2 시트
    DELETE("모임 삭제"), // 확정 참가자(호스트 제외) 0명 → 5a-X3 확인
}

// 취소 사유 최대 글자 수 (service-rules 4장 '필수, 100자')
const val CANCEL_REASON_MAX_LENGTH = 100

fun canSubmitCancelReason(reason: String): Boolean = reason.isNotBlank() && reason.length <= CANCEL_REASON_MAX_LENGTH
