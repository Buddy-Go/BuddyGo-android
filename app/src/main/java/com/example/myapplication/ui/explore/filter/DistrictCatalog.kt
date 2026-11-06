package com.example.myapplication.ui.explore.filter

// 위치 검색(4g)에서 고를 수 있는 구·동. 이름(name)이 조건에 저장되고, 전체 이름(fullName)이 목록에 보인다.
data class District(val name: String, val fullName: String)

// TODO(임시) 서버가 행정구역 목록을 주기 전까지 서울 25개 구와 일부 동만 둔다.
object DistrictCatalog {
    private val gu = listOf(
        "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구",
        "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구",
    )

    private val dongs = mapOf(
        "마포구" to listOf("합정동", "망원동", "상암동", "서교동", "연남동", "성산동", "공덕동", "상수동"),
        "강북구" to listOf("우이동", "수유동", "미아동", "번동"),
        "종로구" to listOf("청운동", "혜화동", "평창동"),
        "영등포구" to listOf("여의도동", "당산동", "문래동"),
        "용산구" to listOf("이태원동", "한남동"),
        "강남구" to listOf("역삼동", "삼성동", "논현동", "청담동"),
    )

    // 구가 먼저, 그 아래 동이 오는 순서
    val all: List<District> = gu.flatMap { g ->
        listOf(District(g, "서울 $g")) + dongs[g].orEmpty().map { District(it, "서울 $g $it") }
    }

    // 전체 이름에 검색어가 들어 있는 곳. 공백과 대소문자는 무시하고, 검색어가 비어 있으면 아무것도 보여 주지 않는다.
    fun search(query: String): List<District> {
        val q = query.filterNot { it.isWhitespace() }
        if (q.isEmpty()) return emptyList()
        return all.filter { it.fullName.filterNot { c -> c.isWhitespace() }.contains(q, ignoreCase = true) }
    }
}

// 글자 조각. 검색어와 겹치는 조각(matched)은 굵게 보여 준다. (4g-2)
data class TextPiece(val text: String, val matched: Boolean)

fun highlightPieces(text: String, query: String): List<TextPiece> {
    val q = query.trim()
    if (q.isEmpty()) return listOf(TextPiece(text, false))
    val start = text.indexOf(q, ignoreCase = true)
    if (start < 0) return listOf(TextPiece(text, false))
    return listOf(
        TextPiece(text.substring(0, start), false),
        TextPiece(text.substring(start, start + q.length), true),
        TextPiece(text.substring(start + q.length), false),
    ).filter { it.text.isNotEmpty() }
}

// 최근 검색. 최신이 맨 앞이고, 같은 곳은 하나만 남기며, 최대 [MAX_RECENT]개까지 둔다.
const val MAX_RECENT = 4

fun List<String>.withRecent(name: String): List<String> = (listOf(name) + (this - name)).take(MAX_RECENT)
