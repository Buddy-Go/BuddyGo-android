package com.example.myapplication.ui.mate.review

import com.example.myapplication.data.model.ReviewVerdict
import com.example.myapplication.data.repository.ReviewSession
import com.example.myapplication.data.repository.ReviewTargetInfo
import java.util.Calendar
import java.util.TimeZone

// 메이트 평가 화면 세 개(12a 목록, 12b 한 명씩 평가, 12c 완료)가 같이 쓰는 상태.
// 흐름: 12a '평가하기' → 12b(그 사람부터, 남은 사람 차례로) → 마지막 제출 → 12c

// 판정 글자 (공용 모델 ReviewVerdict에는 글자가 없어 화면에서 정한다)
val ReviewVerdict.label: String
    get() = when (this) {
        ReviewVerdict.GOOD -> "좋음"
        ReviewVerdict.NORMAL -> "보통"
        ReviewVerdict.BAD -> "아쉬움"
    }

// 태그 목록. 좋음·보통은 긍정 태그, 아쉬움은 아쉬움 태그를 쓴다 (service-rules 11장).
// 계약(docs/data-model.md)상 태그 목록은 개발하며 정한다 → 서버에는 label 문자열로 보낸다(Review.tags).
enum class ReviewTag(val label: String, val isPositive: Boolean) {
    ON_TIME("시간 약속을 지켜요", true),
    CONSIDERATE("서로 배려해요", true),
    GOOD_COMMUNICATION("소통이 원활해요", true),
    SAFE("안전하게 운동해요", true),
    LATE("늦게 왔어요", false),
    HARD_TO_REACH("연락이 잘 안 됐어요", false),
    LEVEL_MISMATCH("운동 수준이 안 맞았어요", false), // 디자인 '페이스가 안 맞았어요'를 헬스에 맞게 바꿈
    BAD_MANNER("매너가 아쉬웠어요", false),
}

// 한마디 최대 글자 수 (service-rules 11장)
const val REVIEW_COMMENT_MAX_LENGTH = 200

// 12b에서 작성 중인 평가 한 건
data class ReviewDraft(
    val verdict: ReviewVerdict? = null,
    val tags: Set<ReviewTag> = emptySet(),
    val comment: String = "",
)

data class ReviewState(
    val session: ReviewSession? = null,
    val currentTargetId: String? = null, // 12b에서 평가 중인 사람
    val draft: ReviewDraft = ReviewDraft(),
    val isSubmitting: Boolean = false,
    val isAllDone: Boolean = false, // 12c로 갈 때
    val loadFailed: Boolean = false, // 12a 평가 대상을 불러오지 못함 → 16g 오류 화면
    val submitFailed: Boolean = false, // 12b 제출 실패 → 16i 안내 띠 (기한 지남·이미 평가함 포함)
    val now: Long = 0L,
) {
    val targets: List<ReviewTargetInfo> get() = session?.targets.orEmpty()

    // ---------- 12a ----------

    // "9/14 · 스포애니 합정점 · 9/21까지 평가 가능"
    fun meetingSubtitle(timeZone: TimeZone = TimeZone.getDefault()): String {
        val s = session ?: return ""
        return "${monthDay(s.meetingStartAtMillis, timeZone)} · ${s.placeName} · ${monthDay(s.reviewDueAtMillis, timeZone)}까지 평가 가능"
    }

    val targetHeader: String get() = "함께한 메이트 ${targets.size}"

    // ---------- 12b ----------

    val currentTarget: ReviewTargetInfo? get() = targets.find { it.userId == currentTargetId }

    // "2 / 3": 전체 명단에서 이 사람의 순서
    val progressText: String
        get() {
            val index = targets.indexOfFirst { it.userId == currentTargetId }
            return if (index < 0) "" else "${index + 1} / ${targets.size}"
        }

    val question: String get() = currentTarget?.let { "${it.nickname}님과 함께한 시간은 어땠나요?" }.orEmpty()

    // 판정을 골라야 태그·한마디가 나온다 (12b-1 → 12b-2)
    val showDetails: Boolean get() = draft.verdict != null

    val tagQuestion: String get() = if (draft.verdict == ReviewVerdict.BAD) "어떤 점이 아쉬웠나요?" else "어떤 점이 좋았나요?"

    val availableTags: List<ReviewTag>
        get() = when (draft.verdict) {
            null -> emptyList()
            ReviewVerdict.BAD -> ReviewTag.entries.filter { !it.isPositive }
            else -> ReviewTag.entries.filter { it.isPositive }
        }

    val commentCounter: String get() = "${draft.comment.length} / $REVIEW_COMMENT_MAX_LENGTH"

    // 지금 사람 다음으로 평가할 사람 (명단 순서, 끝에 닿으면 앞에서부터)
    val nextTargetId: String?
        get() {
            val pending = targets.filter { !it.isReviewed && it.userId != currentTargetId }
            if (pending.isEmpty()) return null
            val currentIndex = targets.indexOfFirst { it.userId == currentTargetId }
            return pending.firstOrNull { targets.indexOf(it) > currentIndex }?.userId ?: pending.first().userId
        }

    // 12b-2 '제출하고 다음 메이트' / 12b-4 마지막 '평가 제출'
    val submitLabel: String get() = if (nextTargetId != null) "제출하고 다음 메이트" else "평가 제출"

    val canSubmit: Boolean get() = draft.verdict != null && !isSubmitting

    // ---------- 12c ----------

    val reviewedCount: Int get() = targets.count { it.isReviewed }

    val doneSubtitle: String get() = "함께한 메이트 ${reviewedCount}명 평가 완료"

    // 메이트로 추가할 사람: 아직 메이트가 아닌 사람 (신청함 포함). 없으면 12c-2
    val mateSuggestions: List<ReviewTargetInfo> get() = targets.filter { !it.isMate }
}

// 판정을 바꿀 때 다른 묶음의 태그는 지운다 (좋음·보통 ↔ 아쉬움)
fun ReviewDraft.withVerdict(verdict: ReviewVerdict): ReviewDraft {
    val positive = verdict != ReviewVerdict.BAD
    return copy(verdict = verdict, tags = tags.filter { it.isPositive == positive }.toSet())
}

fun ReviewDraft.toggled(tag: ReviewTag): ReviewDraft = copy(tags = if (tag in tags) tags - tag else tags + tag)

private fun monthDay(epochMillis: Long, timeZone: TimeZone): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
    return "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
}
