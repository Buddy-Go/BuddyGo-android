package com.example.myapplication.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 14 위치 설정: 계정별로 저장하고, 로그아웃 후 같은 계정으로 다시 로그인하면 되살린다
class ActivityAreaStoreTest {

    private class MemoryStore : KeyValueStore {
        val map = mutableMapOf<String, String>()
        override fun get(key: String): String? = map[key]
        override fun put(key: String, value: String) { map[key] = value }
    }

    @Test
    fun areaSurvivesLogoutAndLoginForSameAccount() {
        val store = MemoryStore()
        ActivityAreaStore.attach(store, "u1")
        ActivityAreaStore.set("강남구")

        ActivityAreaStore.reset() // 로그아웃
        assertEquals(ActivityAreaStore.DEFAULT_AREA, ActivityAreaStore.area.value)

        ActivityAreaStore.switchUser("u1") // 같은 계정으로 다시 로그인
        assertEquals("강남구", ActivityAreaStore.area.value)
    }

    @Test
    fun otherAccountDoesNotSeeMyArea() {
        val store = MemoryStore()
        ActivityAreaStore.attach(store, "u1")
        ActivityAreaStore.set("강남구")
        ActivityAreaStore.switchUser("u2")
        assertEquals(ActivityAreaStore.DEFAULT_AREA, ActivityAreaStore.area.value)
    }

    @Test
    fun keptLoginLoadsAreaOnAppStart() {
        val store = MemoryStore()
        store.put("activity_area:u1", "송파구")
        ActivityAreaStore.attach(store, "u1")
        assertEquals("송파구", ActivityAreaStore.area.value)
    }

    @Test
    fun clearedAreaStaysCleared() {
        val store = MemoryStore()
        ActivityAreaStore.attach(store, "u1")
        ActivityAreaStore.set(null)
        ActivityAreaStore.switchUser("u1")
        assertNull(ActivityAreaStore.area.value)
    }
}
