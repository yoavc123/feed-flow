package com.prof18.feedflow.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlowPaceTest {
    @Test
    fun `paces use progressively wider windows`() {
        assertEquals(3L, FlowPace.FLASH.windowHours)
        assertEquals(12L, FlowPace.DAILY.windowHours)
        assertEquals(24L, FlowPace.STANDARD.windowHours)
        assertEquals(72L, FlowPace.SLOW.windowHours)
        assertEquals(168L, FlowPace.TIMELESS.windowHours)
    }

    @Test
    fun `item remains in flow through the edge of its pace window`() {
        val now = 1_000_000_000L
        val edge = now - FlowPace.DAILY.windowMillis

        assertTrue(FlowPace.DAILY.includes(edge, now))
        assertFalse(FlowPace.DAILY.includes(edge - 1, now))
    }

    @Test
    fun `undated items only appear in timeless flow`() {
        assertFalse(FlowPace.STANDARD.includes(pubDateMillis = null, nowMillis = 0L))
        assertTrue(FlowPace.TIMELESS.includes(pubDateMillis = null, nowMillis = 0L))
    }

    @Test
    fun `freshness fades smoothly instead of becoming unread debt`() {
        val now = 1_000_000_000L
        val halfway = now - FlowPace.STANDARD.windowMillis / 2

        assertEquals(1f, FlowPace.STANDARD.freshness(now, now))
        assertEquals(0.5f, FlowPace.STANDARD.freshness(halfway, now))
        assertEquals(0f, FlowPace.STANDARD.freshness(now - FlowPace.STANDARD.windowMillis, now))
    }
}
