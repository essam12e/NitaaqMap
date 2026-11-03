package com.nitaaq.map.core.domain

import kotlin.math.max
import kotlin.math.min

/**
 * Counts down a trip's validity **without trusting the device wall clock**.
 *
 * At the moment the server response arrives we record:
 *  - [remainingAtAnchorMs]: `expiresAt - serverDate` (both from the server), clamped to the TTL;
 *  - [anchorElapsedMs]: the monotonic clock (`SystemClock.elapsedRealtime()`), which the user
 *    cannot change from Settings;
 *  - [anchorWallMs] + [serverOffsetMs]: a fallback for after a reboot, when the monotonic clock
 *    restarts.
 *
 * This only drives the UI. The real enforcement is server-side: `/trip/<id>` re-checks the
 * signed `exp` on every scan, so a screenshot of an expired QR never opens the trip.
 */
data class ExpiryTimeline(
    val expiresAtMs: Long,
    val remainingAtAnchorMs: Long,
    val anchorElapsedMs: Long,
    val anchorWallMs: Long,
    val serverOffsetMs: Long,
    val bootId: Int,
    val ttlMs: Long = DEFAULT_TTL_MS,
) {
    fun remainingMs(nowElapsedMs: Long, nowWallMs: Long, currentBootId: Int): Long {
        val sameBoot = currentBootId == bootId && nowElapsedMs >= anchorElapsedMs
        val raw = if (sameBoot) {
            remainingAtAnchorMs - (nowElapsedMs - anchorElapsedMs)
        } else {
            // After a reboot only the wall clock is left; correct it with the measured server offset.
            expiresAtMs - (nowWallMs + serverOffsetMs)
        }
        return min(max(raw, 0L), ttlMs)
    }

    companion object {
        const val DEFAULT_TTL_MS: Long = 5 * 60 * 1000L

        /**
         * @param serverDateMs value of the HTTP `Date` header (1-second precision), or null.
         * @param deviceWallMs device wall clock when the response arrived.
         */
        fun fromServer(
            expiresAtMs: Long,
            serverDateMs: Long?,
            deviceWallMs: Long,
            elapsedMs: Long,
            bootId: Int,
            ttlMs: Long = DEFAULT_TTL_MS,
        ): ExpiryTimeline {
            // The Date header is truncated to the second, so the true server time may be up to
            // 999 ms later. Subtract that so the app never shows *more* time than the server allows.
            val serverNow = serverDateMs?.plus(DATE_HEADER_PRECISION_MS) ?: deviceWallMs
            val remaining = min(max(expiresAtMs - serverNow, 0L), ttlMs)
            return ExpiryTimeline(
                expiresAtMs = expiresAtMs,
                remainingAtAnchorMs = remaining,
                anchorElapsedMs = elapsedMs,
                anchorWallMs = deviceWallMs,
                serverOffsetMs = serverNow - deviceWallMs,
                bootId = bootId,
                ttlMs = ttlMs,
            )
        }

        private const val DATE_HEADER_PRECISION_MS = 999L
    }
}

enum class ExpiryPhase { Active, Expiring, Expired }

fun phaseFor(remainingMs: Long, warnBelowMs: Long = 60_000L): ExpiryPhase = when {
    remainingMs <= 0L -> ExpiryPhase.Expired
    remainingMs <= warnBelowMs -> ExpiryPhase.Expiring
    else -> ExpiryPhase.Active
}

/** "4:05" style, rounding *up* so 0:00 is only shown once the link is really expired. */
fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999) / 1000
    // Locale.ROOT keeps Latin digits stable regardless of the device locale.
    return String.format(java.util.Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
