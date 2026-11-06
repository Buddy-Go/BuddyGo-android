package com.example.myapplication.data.model

// 매너온도 변화 규칙 (service-rules 11장). 실제 계산은 서버가 하고 사유와 함께 이력에 남긴다.
// 앱은 평가 화면에서 '이 평가가 매너온도에 얼마나 반영되는지' 같은 안내가 필요할 때만 이 값을 쓴다.
//
// TODO(임시) 정의서가 점수를 정하지 않았다(미정). 아래 값은 자리만 잡은 임시값이고, 정해지면 이 표만 바꾼다.
//   T = 판정 기본 점수 + (태그 수) × (판정별 태그 점수)
object MannerRules {
    const val START_TEMPERATURE = 36.5f // 가입 시 시작값 (가정)

    data class Score(val base: Float, val perTag: Float)

    val reviewScores: Map<ReviewVerdict, Score> = mapOf(
        ReviewVerdict.GOOD to Score(base = 0.3f, perTag = 0.1f),
        ReviewVerdict.NORMAL to Score(base = 0.0f, perTag = 0.05f), // 좋음보다 낮거나 0
        ReviewVerdict.BAD to Score(base = -0.5f, perTag = -0.1f),
    )

    // 평가 한 건이 상대의 매너온도를 바꾸는 양. 태그 개수만 센다(문구와 관계없이).
    fun reviewDelta(verdict: ReviewVerdict, tagCount: Int): Float {
        val score = reviewScores.getValue(verdict)
        return score.base + tagCount.coerceAtLeast(0) * score.perTag
    }

    // 시스템이 자동으로 내리는 경우. 값은 모두 임시이고 순서(크기 관계)만 정의서를 따른다.
    enum class Penalty(val delta: Float) {
        LATE(-0.5f),                  // 지각: 노쇼보다 작게
        NO_SHOW(-1.5f),               // 노쇼
        LEAVE_BEFORE_DEADLINE(-0.5f), // 마감된 모임에서 나가기, 데드라인 전(마감 해제)
        LEAVE_AFTER_DEADLINE(-2.0f),  // 데드라인 후(방폭 유발): 더 크게
        HOST_CANCEL_BEFORE_CLOSED(-1.0f), // 호스트 취소: 마감 전 + 확정자 있음
        HOST_CANCEL_AFTER_CLOSED(-3.0f),  // 마감 후: 더 크게
    }

    // 매너온도에 더하고, 정의서에 하한 상한이 없어 0 미만만 막는다.
    fun applied(current: Float, delta: Float): Float = (current + delta).coerceAtLeast(0f)
}
