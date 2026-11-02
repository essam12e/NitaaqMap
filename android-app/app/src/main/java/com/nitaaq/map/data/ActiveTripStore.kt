package com.nitaaq.map.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nitaaq.map.core.domain.ExpiryTimeline
import com.nitaaq.map.core.domain.TemporaryTrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.activeTripStore by preferencesDataStore(name = "active_trip")

/**
 * Keeps the current QR across process death and app restarts, so the countdown resumes correctly.
 *
 * Why not Android Keystore? The stored link is not a secret credential: it is the same value the
 * QR shows, it is useless after 5 minutes (the server enforces `exp`), and it is cleared as soon
 * as it expires. It lives in app-private storage, excluded from backups and device transfer
 * (see `data_extraction_rules.xml`). No server secret is ever on the device.
 */
class ActiveTripStore(private val context: Context) {
    private object Keys {
        val id = stringPreferencesKey("id")
        val tripUrl = stringPreferencesKey("trip_url")
        val sourceUrl = stringPreferencesKey("source_url")
        val expiresAt = longPreferencesKey("expires_at")
        val remainingAtAnchor = longPreferencesKey("remaining_at_anchor")
        val anchorElapsed = longPreferencesKey("anchor_elapsed")
        val anchorWall = longPreferencesKey("anchor_wall")
        val serverOffset = longPreferencesKey("server_offset")
        val bootId = intPreferencesKey("boot_id")
        val ttl = longPreferencesKey("ttl")
    }

    val activeTrip: Flow<TemporaryTrip?> = context.activeTripStore.data.map(::decode)

    suspend fun save(trip: TemporaryTrip) {
        context.activeTripStore.edit { p ->
            p.clear()
            p[Keys.id] = trip.id
            p[Keys.tripUrl] = trip.tripUrl
            p[Keys.sourceUrl] = trip.sourceUrl
            p[Keys.expiresAt] = trip.expiresAtMs
            with(trip.timeline) {
                p[Keys.remainingAtAnchor] = remainingAtAnchorMs
                p[Keys.anchorElapsed] = anchorElapsedMs
                p[Keys.anchorWall] = anchorWallMs
                p[Keys.serverOffset] = serverOffsetMs
                p[Keys.bootId] = bootId
                p[Keys.ttl] = ttlMs
            }
        }
    }

    suspend fun clear() {
        context.activeTripStore.edit { it.clear() }
    }

    private fun decode(p: Preferences): TemporaryTrip? {
        val id = p[Keys.id] ?: return null
        val tripUrl = p[Keys.tripUrl] ?: return null
        val expiresAt = p[Keys.expiresAt] ?: return null
        return TemporaryTrip(
            id = id,
            tripUrl = tripUrl,
            sourceUrl = p[Keys.sourceUrl].orEmpty(),
            expiresAtMs = expiresAt,
            timeline = ExpiryTimeline(
                expiresAtMs = expiresAt,
                remainingAtAnchorMs = p[Keys.remainingAtAnchor] ?: 0L,
                anchorElapsedMs = p[Keys.anchorElapsed] ?: 0L,
                anchorWallMs = p[Keys.anchorWall] ?: 0L,
                serverOffsetMs = p[Keys.serverOffset] ?: 0L,
                bootId = p[Keys.bootId] ?: -1,
                ttlMs = p[Keys.ttl] ?: ExpiryTimeline.DEFAULT_TTL_MS,
            ),
        )
    }
}
