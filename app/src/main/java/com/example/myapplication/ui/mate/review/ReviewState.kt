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

// 태그 목록. 서버 ReviewTagType과 같은 18개(긍정 10 · 부정 8)이고, 이름이 서버 태그 코드다.
// 서버는 좋음·보통·아쉬움 판정과 상관없이 어느 태그든 고를 수 있게 했다. (점수는 태그 성격대로 서버가 매긴다)
// 서버에는 label 문자열로 넘기고(Review.tags), RealReviewRepository가 코드로 바꿔 보낸다.
// 태그를 묶어 보여 주는 분류. 순서가 화면 순서다. (네이버 리뷰 키워드처럼 분류별로 모아 보여 준다)
enum class ReviewTagCategory(val label: String) {
    PROMISE("약속·모임"),
    MANNER("매너·소통"),
    WORKOUT("운동 방식"),
    LEVEL("수준·기구"),
}

enum class ReviewTag(val label: String, val isPositive: Boolean, val category: ReviewTagCategory) {
    PUNCTUAL("시간 약속을 잘 지켜요", true, ReviewTagCategory.PROMISE),
    GOOD_MANNERS("매너가 좋아요", true, ReviewTagCategory.MANNER),
    KIND("친절하고 말투가 편안해요", true, ReviewTagCategory.MANNER),
    FUN_ATMOSPHERE("운동 분위기를 즐겁게 만들어줘요", true, ReviewTagCategory.WORKOUT),
    GOOD_COACHING("자세·운동법을 잘 알려줘요", true, ReviewTagCategory.WORKOUT),
    SIMILAR_LEVEL("운동 수준이 비슷해서 잘 맞았어요", true, ReviewTagCategory.LEVEL),
    SHARES_EQUIPMENT("기구를 배려하며 같이 써요", true, ReviewTagCategory.LEVEL),
    GOOD_SPOTTER("보조를 잘 해줘요", true, ReviewTagCategory.WORKOUT),
    GOOD_COMMUNICATION("소통이 잘 돼요", true, ReviewTagCategory.MANNER),
    WANT_AGAIN("또 같이 운동하고 싶어요", true, ReviewTagCategory.PROMISE),

    NOT_PUNCTUAL("시간 약속을 안 지켜요 (지각 포함)", false, ReviewTagCategory.PROMISE),
    ON_PHONE("운동 중 휴대폰만 봐요", false, ReviewTagCategory.WORKOUT),
    UNCOMFORTABLE_TONE("말투가 불편했어요", false, ReviewTagCategory.MANNER),
    LEVEL_MISMATCH("운동 수준이 기준과 많이 달랐어요", false, ReviewTagCategory.LEVEL),
    HOGS_EQUIPMENT("기구를 독점하거나 정리를 안 해요", false, ReviewTagCategory.LEVEL),
    UNWANTED_ADVICE("운동 방식을 강요하거나 훈수를 둬요", false, ReviewTagCategory.WORKOUT),
    POOR_COMMUNICATION("소통이 잘 안 돼요", false, ReviewTagCategory.MANNER),
    NOT_AS_DESCRIBED("모임 설명과 달랐어요", false, ReviewTagCategory.PROMISE);

    companion object {
        fun availableFor(verdict: ReviewVerdict): List<ReviewTag> = when (verdict) {
            ReviewVerdict.GOOD -> entries.filter { it.isPositive }
            ReviewVerdict.BAD -> entries.filter { !it.isPositive }
            ReviewVerdict.NORMAL -> entries.toList()
        }
    }
}

// 한마디 최대 글자 수 (service-rules 11장)
const val REVIEW_COMMENT_MAX_LENGTH = 200

// 태그는 1개 이상 5개까지 고른다. (네이버 리뷰 키워드와 같은 방식)
const val REVIEW_TAG_MIN = 1
const val REVIEW_TAG_MAX = 5

// 분류마다 처음에는 이만큼만 보여 주고, 펼치면 전부 보인다. (고른 태그는 접혀 있어도 보인다)
const val REVIEW_TAG_COLLAPSED_COUNT = 3

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

    // 평가 기한이 지났으면 더 평가할 수 없다. (서버가 거절하므로 화면에서도 미리 막는다)
    val isExpired: Boolean get() = session?.let { now >= it.reviewDueAtMillis } ?: false

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

    val tagQuestion: String
        get() = when (draft.verdict) {
            ReviewVerdict.BAD -> "어떤 점이 아쉬웠나요?"
            ReviewVerdict.NORMAL -> "어떤 점이 기억에 남나요?"
            else -> "어떤 점이 좋았나요?"
        }

    // 판정에 맞는 태그만 보여 준다. 좋음은 긍정 10개, 아쉬움은 부정 8개, 보통은 서버에 전용 태그가 없어 18개 모두.
    // (서버는 판정과 상관없이 어느 태그든 받으므로 화면에서만 걸러 보여 준다. 판정을 고르기 전에는 태그가 없다)
    val availableTags: List<ReviewTag>
        get() = draft.verdict?.let { ReviewTag.availableFor(it) }.orEmpty()

    // 분류별로 묶은 태그. 분류 순서대로, 분류 안에서는 긍정 태그가 먼저다.
    val tagGroups: List<Pair<ReviewTagCategory, List<ReviewTag>>>
        get() = availableTags.groupBy { it.category }.toList().sortedBy { it.first.ordinal }

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

    val tagCounter: String get() = "${draft.tags.size} / $REVIEW_TAG_MAX"

    val tagLimitReached: Boolean get() = draft.tags.size >= REVIEW_TAG_MAX

    val canSubmit: Boolean get() = draft.verdict != null && draft.tags.size in REVIEW_TAG_MIN..REVIEW_TAG_MAX && !isSubmitting

    // ---------- 12c ----------

    val reviewedCount: Int get() = targets.count { it.isReviewed }

    val doneSubtitle: String get() = "함께한 메이트 ${reviewedCount}명 평가 완료"

    // 메이트로 추가할 사람: 아직 메이트가 아닌 사람 (신청함 포함). 없으면 12c-2
    val mateSuggestions: List<ReviewTargetInfo> get() = targets.filter { !it.isMate }
}

// 판정을 바꾸면 새 판정에서 볼 수 없는 태그는 고른 것에서 뺀다. (안 보이는 태그가 개수에 잡히거나 제출되지 않게)
// 이미 5개를 골랐으면 더 고를 수 없고, 고른 것을 다시 누르면 해제된다.
fun ReviewDraft.withVerdict(verdict: ReviewVerdict): ReviewDraft =
    copy(verdict = verdict, tags = tags.filterTo(mutableSetOf()) { it in ReviewTag.availableFor(verdict) })

// 접힌 분류에서 보일 태그: 앞의 REVIEW_TAG_COLLAPSED_COUNT개와 고른 태그
fun List<ReviewTag>.collapsed(selected: Set<ReviewTag>): List<ReviewTag> =
    filterIndexed { index, tag -> index < REVIEW_TAG_COLLAPSED_COUNT || tag in selected }

fun ReviewDraft.toggled(tag: ReviewTag): ReviewDraft = copy(
    tags = when {
        tag in tags -> tags - tag
        tags.size >= REVIEW_TAG_MAX -> tags
        else -> tags + tag
    },
)

private fun monthDay(epochMillis: Long, timeZone: TimeZone): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
    return "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
}
