package com.nitaaq.map.core

import com.nitaaq.map.core.config.BackendConfig
import com.nitaaq.map.core.data.RemoteTripRepository
import com.nitaaq.map.core.domain.CreateTripUseCase
import com.nitaaq.map.core.domain.TripError
import com.nitaaq.map.core.domain.TripResult
import com.nitaaq.map.core.network.NetworkFactory
import com.nitaaq.map.core.utils.JvmClock
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Runs the *same* network stack the app uses against a real Nitaaq server (local `next start`
 * or the Vercel deployment). Skipped unless NITAAQ_E2E_BASE_URL is set.
 */
class LiveServerE2ETest {
    private val baseUrl = System.getenv("NITAAQ_E2E_BASE_URL")

    @Test
    fun createsSignedLinkThatRedirectsToGoogleMaps() = runBlocking {
        assumeTrue("NITAAQ_E2E_BASE_URL not set", !baseUrl.isNullOrBlank())
        val host = baseUrl!!.toHttpUrlHost()
        val config = BackendConfig.fromProperties(baseUrl!!, host, null, allowCleartext = baseUrl.startsWith("http://"))
        val client = NetworkFactory.okHttpClient(config, "NitaaqMapE2E")
        val useCase = CreateTripUseCase(RemoteTripRepository(NetworkFactory.tripsApi(config, client), config, JvmClock))

        val result = useCase("https://www.google.com/maps/search/?api=1&query=24.7136,46.6753")
        assertTrue(result.toString(), result is TripResult.Success)
        val trip = (result as TripResult.Success).trip
        val remaining = trip.timeline.remainingMs(JvmClock.elapsedMs(), JvmClock.wallMs(), 0)
        assertTrue("remaining=$remaining", remaining in 290_000L..300_000L)

        // Scanning the QR: the server verifies the signature and redirects to Google Maps.
        val noRedirects = OkHttpClient.Builder().followRedirects(false).build()
        noRedirects.newCall(Request.Builder().url(trip.tripUrl).build()).execute().use { response ->
            assertTrue("status=${response.code}", response.code in 300..399)
            assertEquals("https://www.google.com/maps/search/?api=1&query=24.7136,46.6753", response.header("Location"))
        }

        // A tampered token is refused (no redirect, expiry page instead).
        val tampered = trip.tripUrl.dropLast(2) + "xx"
        noRedirects.newCall(Request.Builder().url(tampered).build()).execute().use { response ->
            assertEquals(200, response.code)
            assertTrue(response.body.string().contains("انتهت صلاحية رابط الرحلة"))
        }

        // Server-side rejection of a non-Google link.
        val rejected = RemoteTripRepository(NetworkFactory.tripsApi(config, client), config, JvmClock).createTrip("https://example.com/")
        assertTrue(rejected.toString(), (rejected as TripResult.Failure).error is TripError.InvalidLink)
    }

    private fun String.toHttpUrlHost(): String = java.net.URI(this).host
}
