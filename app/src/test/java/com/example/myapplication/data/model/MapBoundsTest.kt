package com.example.myapplication.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapBoundsTest {
    private val b = MapBounds(37.0, 126.0, 38.0, 127.0)

    @Test
    fun 안에_있는_점과_경계_위의_점은_포함한다() {
        assertTrue(b.contains(37.5, 126.5))
        assertTrue(b.contains(37.0, 126.0))
        assertTrue(b.contains(38.0, 127.0))
    }

    @Test
    fun 밖에_있는_점은_포함하지_않는다() {
        assertFalse(b.contains(36.99, 126.5))
        assertFalse(b.contains(37.5, 127.01))
        assertFalse(b.contains(38.01, 126.5))
        assertFalse(b.contains(37.5, 125.99))
    }

    @Test
    fun 모서리_두_점은_순서와_관계없이_같은_영역이_된다() {
        assertEquals(b, MapBounds.of(37.0, 126.0, 38.0, 127.0))
        assertEquals(b, MapBounds.of(38.0, 127.0, 37.0, 126.0))
        assertEquals(b, MapBounds.of(38.0, 126.0, 37.0, 127.0))
    }
}
