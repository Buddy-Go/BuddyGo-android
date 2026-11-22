package com.example.myapplication.ui.explore

import com.example.myapplication.data.model.Meeting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 3 탐색 지도: 멀리서 볼 때 가까운 핀을 묶는다
class PinClusterTest {

    private fun pin(key: String, lat: Double, lng: Double, n: Int = 1) =
        PinGroup(key, List(n) { Meeting(id = "$key-$it") }, lat, lng)

    // 홍대 근처 두 핀(약 200m 차이)과 강남 핀
    private val a = pin("a", 37.5563, 126.9236)
    private val b = pin("b", 37.5575, 126.9250, n = 2)
    private val far = pin("far", 37.4979, 127.0276)

    @Test
    fun closeZoomKeepsEveryPin() {
        assertEquals(listOf(a, b, far), clusterPins(listOf(a, b, far), zoomLevel = CLUSTER_MAX_ZOOM + 1))
    }

    @Test
    fun farZoomMergesNearbyPinsAndCountsMeetings() {
        val shown = clusterPins(listOf(a, b, far), zoomLevel = 12)
        assertEquals(2, shown.size)
        val cluster = shown.single { isClusterKey(it.key) }
        assertEquals(3, cluster.count) // a 1개 + b 2개
        assertTrue(shown.any { it.key == "far" })
    }

    @Test
    fun selectedPinIsNeverMerged() {
        val shown = clusterPins(listOf(a, b, far), zoomLevel = 12, keepKey = "a")
        assertTrue(shown.any { it.key == "a" })
        assertTrue(shown.none { isClusterKey(it.key) })
    }

    @Test
    fun cellShrinksWhenZoomingIn() {
        assertTrue(clusterCellDegrees(13) < clusterCellDegrees(12))
    }
}
