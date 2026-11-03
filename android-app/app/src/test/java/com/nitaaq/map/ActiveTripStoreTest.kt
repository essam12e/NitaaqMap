package com.nitaaq.map

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nitaaq.map.core.domain.ExpiryTimeline
import com.nitaaq.map.core.domain.TemporaryTrip
import com.nitaaq.map.data.ActiveTripStore
import com.nitaaq.map.presentation.countdown
import com.nitaaq.map.core.domain.ExpiryPhase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** "Reopen the app": the active code and its countdown survive a restart, and the phone clock can't extend it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ActiveTripStoreTest {

    @Test
    fun countdownResumesAfterReopenAndIgnoresPhoneClockChanges() = runBlocking {
        val clock = FakeClock()
        val store = ActiveTripStore(ApplicationProvider.getApplicationContext())
        val expiresAt = clock.wall + 300_000
        val trip = TemporaryTrip(
            "a.b", "https://nitaaq-map.vercel.app/trip/a.b", "https://maps.app.goo.gl/x", expiresAt,
            ExpiryTimeline.fromServer(expiresAt, clock.wall, clock.wall, clock.elapsed, clock.boot),
        )
        store.save(trip)

        // App closed; 2 minutes pass; user sets the phone clock back by a day; app reopened (same boot).
        clock.elapsed += 120_000
        clock.wall -= 86_400_000
        val reopened = ActiveTripStore(ApplicationProvider.getApplicationContext()).activeTrip.first()!!
        val c = reopened.countdown(clock)
        assert(c.remainingMs in 179_000L..180_000L) { "remaining=${c.remainingMs}" }
        assertEquals(ExpiryPhase.Active, c.phase)

        // 3 more minutes: expired, regardless of the wall clock.
        clock.elapsed += 180_000
        assertEquals(ExpiryPhase.Expired, reopened.countdown(clock).phase)

        store.clear()
        assertNull(store.activeTrip.first())
    }
}
