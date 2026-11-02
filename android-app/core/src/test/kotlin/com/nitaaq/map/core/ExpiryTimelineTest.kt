package com.nitaaq.map.core

import com.nitaaq.map.core.domain.ExpiryPhase
import com.nitaaq.map.core.domain.ExpiryTimeline
import com.nitaaq.map.core.domain.formatRemaining
import com.nitaaq.map.core.domain.phaseFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryTimelineTest {

    private val serverNow = 1_800_000_000_000L          // server time when it answered
    private val expiresAt = serverNow + 5 * 60_000L       // server TTL is exactly 5 minutes
    private val serverDateHeader = serverNow / 1000 * 1000 // Date header has 1 s precision

    @Test
    fun countsFiveMinutesOnTheMonotonicClock() {
        val t = ExpiryTimeline.fromServer(expiresAt, serverDateHeader, deviceWallMs = serverNow, elapsedMs = 10_000, bootId = 1)
        // Never shows more than the server allows (Date header rounding is absorbed).
        assertTrue(t.remainingMs(10_000, serverNow, 1) <= 5 * 60_000L)
        assertTrue(t.remainingMs(10_000, serverNow, 1) >= 5 * 60_000L - 1_000L)
        assertEquals(0L, t.remainingMs(10_000 + 5 * 60_000L, serverNow + 5 * 60_000L, 1))
        assertEquals(ExpiryPhase.Expired, phaseFor(t.remainingMs(10_000 + 5 * 60_000L, 0, 1)))
    }

    @Test
    fun deviceClockAheadOrBehindDoesNotChangeTheCountdown() {
        val oneHour = 3_600_000L
        val ahead = ExpiryTimeline.fromServer(expiresAt, serverDateHeader, serverNow + oneHour, 500, 7)
        val behind = ExpiryTimeline.fromServer(expiresAt, serverDateHeader, serverNow - oneHour, 500, 7)
        val exact = ExpiryTimeline.fromServer(expiresAt, serverDateHeader, serverNow, 500, 7)
        val at = 500L + 60_000L
        assertEquals(exact.remainingMs(at, 0, 7), ahead.remainingMs(at, 0, 7))
        assertEquals(exact.remainingMs(at, 0, 7), behind.remainingMs(at, 0, 7))
    }

    @Test
    fun changingThePhoneTimeAfterCreationHasNoEffect() {
        val t = ExpiryTimeline.fromServer(expiresAt, serverDateHeader, serverNow, 1_000, 3)
        val honest = t.remainingMs(nowElapsedMs = 61_000, nowWallMs = serverNow + 60_000, currentBootId = 3)
        // User moves the clock back one day to "extend" the code.
        val cheated = t.remainingMs(nowElapsedMs = 61_000, nowWallMs = serverNow - 86_400_000L, currentBootId = 3)
        assertEquals(honest, cheated)
        // ...and moving it forward does not expire it early either.
        val forward = t.remainingMs(nowElapsedMs = 61_000, nowWallMs = serverNow + 86_400_000L, currentBootId = 3)
        assertEquals(honest, forward)
    }

    @Test
    fun afterRebootFallsBackToWallClockCorrectedByServerOffset() {
        // Device clock is 10 minutes behind the server.
        val skew = -600_000L
        val t = ExpiryTimeline.fromServer(expiresAt, serverDateHeader, serverNow + skew, 50_000, bootId = 4)
        // Reboot: elapsed restarts near 0, boot id changes. Two minutes have passed in real time.
        val remaining = t.remainingMs(nowElapsedMs = 5_000, nowWallMs = serverNow + skew + 120_000, currentBootId = 5)
        assertTrue("remaining=$remaining", remaining in (3 * 60_000L - 1_000L)..(3 * 60_000L))
    }

    @Test
    fun neverExceedsTtlEvenIfServerClockLooksWrong() {
        val t = ExpiryTimeline.fromServer(expiresAt, serverDateMs = serverNow - 3_600_000L, deviceWallMs = serverNow, elapsedMs = 0, bootId = 1)
        assertEquals(ExpiryTimeline.DEFAULT_TTL_MS, t.remainingMs(0, serverNow, 1))
    }

    @Test
    fun withoutDateHeaderUsesDeviceClockButStillClamps() {
        val t = ExpiryTimeline.fromServer(expiresAt, serverDateMs = null, deviceWallMs = serverNow + 120_000, elapsedMs = 0, bootId = 1)
        assertEquals(3 * 60_000L, t.remainingMs(0, 0, 1))
    }

    @Test
    fun alreadyExpiredOnArrival() {
        val t = ExpiryTimeline.fromServer(expiresAt, serverDateMs = expiresAt + 5_000, deviceWallMs = serverNow, elapsedMs = 0, bootId = 1)
        assertEquals(0L, t.remainingMs(0, serverNow, 1))
    }

    @Test
    fun phasesAndFormatting() {
        assertEquals(ExpiryPhase.Active, phaseFor(61_000))
        assertEquals(ExpiryPhase.Expiring, phaseFor(60_000))
        assertEquals(ExpiryPhase.Expiring, phaseFor(1))
        assertEquals(ExpiryPhase.Expired, phaseFor(0))
        assertEquals("5:00", formatRemaining(300_000))
        assertEquals("4:05", formatRemaining(244_001))
        assertEquals("0:01", formatRemaining(1))
        assertEquals("0:00", formatRemaining(0))
    }
}
