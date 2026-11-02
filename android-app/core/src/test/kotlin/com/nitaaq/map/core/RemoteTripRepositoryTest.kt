package com.nitaaq.map.core

import com.nitaaq.map.core.config.BackendConfig
import com.nitaaq.map.core.data.RemoteTripRepository
import com.nitaaq.map.core.domain.CreateTripUseCase
import com.nitaaq.map.core.domain.TripError
import com.nitaaq.map.core.domain.TripResult
import com.nitaaq.map.core.network.NetworkFactory
import com.nitaaq.map.core.utils.DeviceClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class RemoteTripRepositoryTest {

    private lateinit var server: MockWebServer
    private val clock = object : DeviceClock {
        var wall = 0L
        var elapsed = 1_000L
        override fun wallMs() = wall
        override fun elapsedMs() = elapsed
        override fun bootId() = 1
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun config(qrOrigin: String? = null, readTimeoutMs: Long = 5_000) = BackendConfig(
        apiBaseUrl = server.url("/").toString(),
        trustedTripHosts = setOf(server.hostName.lowercase()),
        qrPublicOrigin = qrOrigin,
        allowCleartext = true,
        connectTimeoutMs = 2_000,
        readTimeoutMs = readTimeoutMs,
    )

    private fun repo(config: BackendConfig = config()): RemoteTripRepository {
        val client = NetworkFactory.okHttpClient(config, "NitaaqMapTest")
        return RemoteTripRepository(NetworkFactory.tripsApi(config, client), config, clock, Dispatchers.Unconfined)
    }

    private fun httpDate(ms: Long) = DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(ms).atOffset(ZoneOffset.UTC))

    private fun okBody(id: String, expiresAt: Long, host: String = "${server.hostName}:${server.port}") =
        """{"id":"$id","tripUrl":"http://$host/trip/$id","expiresAt":"${Instant.ofEpochMilli(expiresAt)}"}"""

    @Test
    fun successParsesContractAndUsesServerTimeNotDeviceTime() = runTest {
        val serverNow = 1_800_000_000_000L
        clock.wall = serverNow + 3_600_000L // device clock is one hour ahead
        server.enqueue(
            MockResponse().setBody(okBody("payload.sig", serverNow + 300_000L)).setHeader("Date", httpDate(serverNow))
                .setHeader("Content-Type", "application/json"),
        )

        val result = repo().createTrip("https://maps.app.goo.gl/AbC")

        assertTrue(result.toString(), result is TripResult.Success)
        val trip = (result as TripResult.Success).trip
        assertEquals("payload.sig", trip.id)
        assertTrue(trip.tripUrl.endsWith("/trip/payload.sig"))
        val remaining = trip.timeline.remainingMs(clock.elapsed, clock.wall, 1)
        assertTrue("remaining=$remaining", remaining in 299_000L..300_000L)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/trips", request.path)
        val sent = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("https://maps.app.goo.gl/AbC", sent["url"]!!.jsonPrimitive.content)
        assertFalse("siteOrigin must be omitted by default", sent.containsKey("siteOrigin"))
    }

    @Test
    fun sendsStableQrOriginWhenConfigured() = runTest {
        server.enqueue(MockResponse().setBody(okBody("a.b", System.currentTimeMillis() + 300_000L)))
        repo(config(qrOrigin = "https://go.nitaaq.example")).createTrip("https://maps.app.goo.gl/x")
        val sent = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("https://go.nitaaq.example", sent["siteOrigin"]!!.jsonPrimitive.content)
    }

    @Test
    fun invalidLinkReturnsServerArabicMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":"الرابط يجب أن يكون رابط خرائط Google صالح."}"""))
        val result = repo().createTrip("https://example.com")
        assertEquals(TripResult.Failure(TripError.InvalidLink("الرابط يجب أن يكون رابط خرائط Google صالح.")), result)
    }

    @Test
    fun emptyLink400IsInvalidLink() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"فضلا الصق رابط الرحلة أولا."}"""))
        val result = repo().createTrip("")
        assertEquals(TripResult.Failure(TripError.InvalidLink("فضلا الصق رابط الرحلة أولا.")), result)
    }

    @Test
    fun serverErrorIsReported() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        assertEquals(TripResult.Failure(TripError.Server(500, null)), repo().createTrip("https://maps.app.goo.gl/x"))
    }

    @Test
    fun malformedBodyIsRejected() = runTest {
        server.enqueue(MockResponse().setBody("<html>not json</html>"))
        val result = repo().createTrip("https://maps.app.goo.gl/x")
        assertTrue(result.toString(), (result as TripResult.Failure).error is TripError.UntrustedResponse)
    }

    @Test
    fun missingFieldsAreRejected() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":"a.b"}"""))
        val result = repo().createTrip("https://maps.app.goo.gl/x")
        assertEquals(TripResult.Failure(TripError.UntrustedResponse("missing-fields")), result)
    }

    @Test
    fun qrLinkToUntrustedHostIsNeverAccepted() = runTest {
        server.enqueue(MockResponse().setBody(okBody("a.b", System.currentTimeMillis() + 300_000L, host = "evil.example")))
        val result = repo().createTrip("https://maps.app.goo.gl/x")
        assertEquals(TripResult.Failure(TripError.UntrustedResponse("untrusted-host")), result)
    }

    @Test
    fun qrLinkMustBeTheSignedTripPath() = runTest {
        // A response that tries to put the raw Google Maps link (or anything else) in the QR.
        server.enqueue(
            MockResponse().setBody(
                """{"id":"a.b","tripUrl":"http://${server.hostName}:${server.port}/redirect?to=maps","expiresAt":"${Instant.now().plusSeconds(300)}"}""",
            ),
        )
        val result = repo().createTrip("https://maps.app.goo.gl/x")
        assertEquals(TripResult.Failure(TripError.UntrustedResponse("untrusted-host")), result)
    }

    @Test
    fun cleartextLinksRejectedInProductionConfig() {
        val prod = BackendConfig("https://nitaaq-map.vercel.app/", setOf("nitaaq-map.vercel.app"))
        val r = RemoteTripRepository(NetworkFactory.tripsApi(prod, NetworkFactory.okHttpClient(prod, "t")), prod, clock)
        assertTrue(r.isTrustedTripUrl("https://nitaaq-map.vercel.app/trip/a.b", "a.b"))
        assertFalse(r.isTrustedTripUrl("http://nitaaq-map.vercel.app/trip/a.b", "a.b"))
        assertFalse(r.isTrustedTripUrl("https://nitaaq-map.vercel.app.evil.com/trip/a.b", "a.b"))
        assertFalse(r.isTrustedTripUrl("https://nitaaq-map.vercel.app/trip/other.id", "a.b"))
        assertFalse(r.isTrustedTripUrl("https://maps.app.goo.gl/x", "a.b"))
    }

    @Test
    fun noConnection() = runTest {
        val cfg = config()
        server.shutdown()
        val result = repo(cfg).createTrip("https://maps.app.goo.gl/x")
        assertEquals(TripResult.Failure(TripError.NoConnection), result)
    }

    @Test
    fun timeout() = runTest {
        server.enqueue(MockResponse().setBody(okBody("a.b", 0)).setHeadersDelay(3, TimeUnit.SECONDS))
        val result = repo(config(readTimeoutMs = 500)).createTrip("https://maps.app.goo.gl/x")
        assertEquals(TripResult.Failure(TripError.Timeout), result)
    }

    @Test
    fun useCaseValidatesLocallyWithoutNetwork() = runTest {
        val useCase = CreateTripUseCase(repo())
        assertEquals(TripResult.Failure(TripError.EmptyLink), useCase("   "))
        assertEquals(TripResult.Failure(TripError.InvalidLink()), useCase("https://example.com/x"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun useCaseSendsNormalizedLink() = runTest {
        server.enqueue(MockResponse().setBody(okBody("a.b", System.currentTimeMillis() + 300_000L)))
        val result = CreateTripUseCase(repo())("وصلني هنا https://maps.app.goo.gl/AbC).")
        assertTrue(result is TripResult.Success)
        val sent = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("https://maps.app.goo.gl/AbC", sent["url"]!!.jsonPrimitive.content)
    }
}
