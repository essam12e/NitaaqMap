package com.nitaaq.map.presentation

import com.nitaaq.map.core.domain.ExpiryPhase
import com.nitaaq.map.core.domain.TemporaryTrip
import com.nitaaq.map.core.domain.phaseFor
import com.nitaaq.map.core.utils.DeviceClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class Countdown(val remainingMs: Long, val fraction: Float, val phase: ExpiryPhase)

fun TemporaryTrip.countdown(clock: DeviceClock): Countdown {
    val remaining = timeline.remainingMs(clock.elapsedMs(), clock.wallMs(), clock.bootId())
    return Countdown(remaining, remaining.toFloat() / timeline.ttlMs, phaseFor(remaining))
}

/** Emits a fresh countdown ~4×/s until expiry (then once more and stops). */
fun TemporaryTrip.countdownFlow(clock: DeviceClock, periodMs: Long = 250): Flow<Countdown> = flow {
    while (true) {
        val c = countdown(clock)
        emit(c)
        if (c.phase == ExpiryPhase.Expired) break
        delay(periodMs)
    }
}
