package com.example.myapplication.ui.explore

import com.example.myapplication.data.model.BodyPart
import com.example.myapplication.data.model.HikingStyle
import com.example.myapplication.data.model.Meeting
import com.example.myapplication.data.model.MeetingCriteria
import com.example.myapplication.data.model.MeetingPlace
import com.example.myapplication.data.model.Sport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPinStyleTest {
    private val place = MeetingPlace(name = "A", lotAddress = "서교동 1-1")
    private val health = MeetingCriteria.Health(setOf(BodyPart.CHEST))
    private val running = MeetingCriteria.Running(distanceKm = 5f, hostPaceSec = 360)
    private val hiking = MeetingCriteria.Hiking(HikingStyle.HOBBY)

    private fun pinOf(vararg criteria: MeetingCriteria, favorite: Boolean = false): PinGroup =
        criteria.mapIndexed { i, c -> Meeting(id = "$i", criteria = c, place = place, isFavorite = favorite && i == 0) }
            .groupIntoPins().single()

    @Test
    fun 선택한_핀이_없으면_모두_기본색이다() {
        assertEquals(PinStyle(Sport.HEALTH, PinLook.Default), pinStyleOf(pinOf(health), null))
    }

    @Test
    fun 선택한_핀만_선택색이고_나머지는_죽인다() {
        val pin = pinOf(health)
        assertEquals(PinLook.Selected, pinStyleOf(pin, pin.key).look)
        assertEquals(PinLook.Dimmed, pinStyleOf(pin, "다른 지번").look)
    }

    @Test
    fun 한_종목만_있는_묶음은_그_종목_색이다() {
        assertEquals(Sport.RUNNING, pinStyleOf(pinOf(running, running), null).sport)
    }

    @Test
    fun 종목이_섞인_묶음은_회색_핀이다() {
        assertNull(pinStyleOf(pinOf(health, running, hiking), null).sport)
    }

    @Test
    fun 묶음_안에_찜한_모임이_있으면_하트가_붙는다() {
        assertTrue(pinOf(health, running, favorite = true).hasFavorite())
        assertFalse(pinOf(health, running).hasFavorite())
    }

    @Test
    fun 카드_두_번째_칩은_종목별_기준_요약이다() {
        val chest = Meeting(criteria = MeetingCriteria.Health(setOf(BodyPart.ARM, BodyPart.CHEST)))
        assertEquals("가슴·팔", criteriaChipText(chest))
        assertEquals("5km", criteriaChipText(Meeting(criteria = running)))
        assertEquals("5.5km", criteriaChipText(Meeting(criteria = MeetingCriteria.Running(5.5f, 360))))
        assertEquals("취미", criteriaChipText(Meeting(criteria = hiking)))
    }
}
