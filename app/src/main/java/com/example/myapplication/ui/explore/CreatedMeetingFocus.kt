package com.example.myapplication.ui.explore

import com.example.myapplication.data.model.GeoPoint

// 모임을 개설하면 지도를 그 장소로 옮기라고 탐색 화면에 알려 주는 곳. 탐색 화면이 한 번 받아 가면 비운다.
// 개설 화면과 탐색 화면은 따로 있고, 탐색 화면은 개설 도중 화면에 없으므로 값을 맡겨 둔다.
object CreatedMeetingFocus {
    private var pending: GeoPoint? = null

    fun set(point: GeoPoint) { pending = point }

    fun take(): GeoPoint? = pending.also { pending = null }

    fun clear() { pending = null }
}
