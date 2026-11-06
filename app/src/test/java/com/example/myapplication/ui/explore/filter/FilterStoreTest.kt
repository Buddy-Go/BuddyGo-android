package com.example.myapplication.ui.explore.filter

import com.example.myapplication.data.model.Sport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterStoreTest {
    @After
    fun 비우기() = FilterStore.clear()

    @Test
    fun 적용한_조건이_저장소에_남는다() {
        FilterStore.apply(MeetingFilter(sport = Sport.RUNNING, minMannerTemperature = 50))
        assertEquals(Sport.RUNNING, FilterStore.applied.value.sport)
        assertEquals(50, FilterStore.applied.value.minMannerTemperature)
    }

    @Test
    fun 일부만_바꿔도_나머지_조건은_유지된다() {
        FilterStore.apply(MeetingFilter(minMannerTemperature = 30))
        FilterStore.update { it.withSport(Sport.HIKING) }
        assertEquals(30, FilterStore.applied.value.minMannerTemperature)
        assertEquals(Sport.HIKING, FilterStore.applied.value.sport)
    }

    @Test
    fun 비우면_조건이_없는_상태로_돌아간다() {
        FilterStore.apply(MeetingFilter(sport = Sport.HEALTH))
        FilterStore.clear()
        assertEquals(MeetingFilter(), FilterStore.applied.value)
    }
}
