package com.example.myapplication.data.local

import android.content.Context

// 앱을 껐다 켜도 남겨야 하는 작은 값(최근 검색 지역, 최근 장소)을 글자로 저장하는 곳.
// 화면과 ViewModel은 이 interface만 알고, 실제 저장은 앱 시작 때 AppStorage.init이 붙인다.
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

// 테스트와 저장소를 붙이기 전에 쓰는 메모리 구현. 앱을 끄면 사라진다.
class InMemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, String>()
    override fun get(key: String): String? = map[key]
    override fun put(key: String, value: String) { map[key] = value }
}

// SharedPreferences에 저장한다. 저장은 비동기(apply)라 화면을 막지 않는다.
class SharedPrefsKeyValueStore(context: Context, name: String = "buddygo_recents") : KeyValueStore {
    private val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String) { prefs.edit().putString(key, value).apply() }
}

object AppStorage {
    @Volatile var store: KeyValueStore = InMemoryKeyValueStore()
        private set

    fun init(context: Context) {
        store = SharedPrefsKeyValueStore(context)
    }
}

// ---- 글자 변환. 줄바꿈으로 항목을, 탭으로 칸을 나눈다. 값 안의 줄바꿈·탭은 공백으로 바꿔 깨지지 않게 한다. ----

private fun clean(s: String) = s.replace('\n', ' ').replace('\t', ' ').trim()

fun encodeStrings(list: List<String>): String = list.map(::clean).filter { it.isNotEmpty() }.joinToString("\n")

fun decodeStrings(text: String?): List<String> = text.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
