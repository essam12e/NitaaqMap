package com.nitaaq.map

import com.nitaaq.map.core.utils.DeviceClock

class FakeClock(var wall: Long = 1_800_000_000_000L, var elapsed: Long = 100_000L, var boot: Int = 1) : DeviceClock {
    override fun wallMs() = wall
    override fun elapsedMs() = elapsed
    override fun bootId() = boot
}
