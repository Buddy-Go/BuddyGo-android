package com.example.myapplication.ui.explore

import kotlin.math.floor
import kotlin.math.pow

// 지도를 멀리서 볼 때 가까운 핀들을 하나로 묶는다 (지쿠터처럼 일정 범위마다 묶어 개수로 표시).
// 이 배율보다 가까이 보면 묶지 않고 핀을 하나씩 보여 준다. (카카오맵 배율: 숫자가 클수록 가까이)
const val CLUSTER_MAX_ZOOM = 14

// 묶음 핀의 key 앞에 붙는다. 이 key로 눌리면 모임 목록 대신 그 자리로 확대한다.
const val CLUSTER_KEY_PREFIX = "cluster:"

fun isClusterKey(key: String): Boolean = key.startsWith(CLUSTER_KEY_PREFIX)

// 화면에서 대략 이만큼(px) 안에 있는 핀끼리 묶는다.
private const val CLUSTER_CELL_PX = 60.0

// 배율에서 CLUSTER_CELL_PX가 몇 도(위도·경도)인지. 256px 타일 기준 웹 메르카토르 근사.
fun clusterCellDegrees(zoomLevel: Int): Double = CLUSTER_CELL_PX * 360.0 / (256.0 * 2.0.pow(zoomLevel))

// 같은 칸에 들어간 핀을 하나로 묶는다. 고른 핀(keepKey)은 묶지 않고 그대로 둔다.
// 하나만 든 칸은 원래 핀 그대로 돌려준다.
fun clusterPins(pins: List<PinGroup>, zoomLevel: Int, keepKey: String? = null): List<PinGroup> {
    if (zoomLevel > CLUSTER_MAX_ZOOM || pins.size < 2) return pins
    val cell = clusterCellDegrees(zoomLevel)
    val (kept, rest) = pins.partition { it.key == keepKey }
    val clustered = rest
        .groupBy { floor(it.latitude / cell).toLong() to floor(it.longitude / cell).toLong() }
        .values
        .map { group ->
            if (group.size == 1) group.first()
            else PinGroup(
                key = CLUSTER_KEY_PREFIX + group.joinToString(",") { it.key },
                meetings = group.flatMap { it.meetings }.sortedByDistance(),
                latitude = group.map { it.latitude }.average(),
                longitude = group.map { it.longitude }.average(),
            )
        }
    return kept + clustered
}
