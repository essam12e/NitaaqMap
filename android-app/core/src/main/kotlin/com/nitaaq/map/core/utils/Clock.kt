package com.nitaaq.map.core.utils

/** Abstracts time so expiry logic is testable (and so the device wall clock is never the authority). */
interface DeviceClock {
    /** Wall clock (user can change it). */
    fun wallMs(): Long

    /** Monotonic clock since boot (`SystemClock.elapsedRealtime()` on Android). */
    fun elapsedMs(): Long

    /** Changes on every reboot (`Settings.Global.BOOT_COUNT` on Android). */
    fun bootId(): Int
}

object JvmClock : DeviceClock {
    override fun wallMs() = System.currentTimeMillis()
    override fun elapsedMs() = System.nanoTime() / 1_000_000
    override fun bootId() = 0
}
