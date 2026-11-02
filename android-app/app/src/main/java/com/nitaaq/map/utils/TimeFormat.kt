package com.nitaaq.map.utils

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * "10:42 م" for an absolute server instant, in the device's time zone.
 * The instant itself comes from the server, so a wrong device clock does not change it.
 */
fun formatClockTime(epochMs: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
    val h24 = cal.get(Calendar.HOUR_OF_DAY)
    val h12 = (h24 % 12).let { if (it == 0) 12 else it }
    val minute = cal.get(Calendar.MINUTE)
    val suffix = if (h24 < 12) "ص" else "م"
    return String.format(Locale.ROOT, "%d:%02d %s", h12, minute, suffix)
}
