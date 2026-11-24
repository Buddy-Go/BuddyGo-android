package com.example.myapplication.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapCameraStoreTest {
    @Test
    fun 저장한_위치를_다시_읽는다() {
        val s = MapCameraStore.Snapshot(37.5563, 126.9236, 15)
        assertEquals(s, MapCameraStore.decode(MapCameraStore.encode(s)))
    }

    @Test
    fun 깨진_값은_무시한다() {
        assertNull(MapCameraStore.decode(""))
        assertNull(MapCameraStore.decode("1,2"))
        assertNull(MapCameraStore.decode("a,b,c"))
    }

    @Test
    fun 앱을_껐다_켜면_저장소에서_되살린다() {
        val store = InMemoryKeyValueStore()
        MapCameraStore.attach(store)
        MapCameraStore.save(35.1, 129.0, 13)
        MapCameraStore.attach(store)
        assertEquals(MapCameraStore.Snapshot(35.1, 129.0, 13), MapCameraStore.last)
        MapCameraStore.clear()
        MapCameraStore.attach(store)
        assertNull(MapCameraStore.last)
    }
}
