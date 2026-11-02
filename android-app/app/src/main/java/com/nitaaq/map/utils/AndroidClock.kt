package com.nitaaq.map.utils

import android.content.ContentResolver
import android.os.SystemClock
import android.provider.Settings
import com.nitaaq.map.core.utils.DeviceClock

class AndroidClock(private val resolver: ContentResolver) : DeviceClock {
    override fun wallMs(): Long = System.currentTimeMillis()

    /** Monotonic, includes deep sleep, and cannot be changed from the Date & time settings. */
    override fun elapsedMs(): Long = SystemClock.elapsedRealtime()

    override fun bootId(): Int = Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, 0)
}
