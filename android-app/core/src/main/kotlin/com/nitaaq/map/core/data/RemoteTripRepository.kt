package com.nitaaq.map.core.data

import com.nitaaq.map.core.config.BackendConfig
import com.nitaaq.map.core.domain.ExpiryTimeline
import com.nitaaq.map.core.domain.TemporaryTrip
import com.nitaaq.map.core.domain.TripError
import com.nitaaq.map.core.domain.TripRepository
import com.nitaaq.map.core.domain.TripResult
import com.nitaaq.map.core.network.ApiErrorBody
import com.nitaaq.map.core.network.CreateTripRequest
import com.nitaaq.map.core.network.NetworkFactory
import com.nitaaq.map.core.network.TripsApi
import com.nitaaq.map.core.utils.DeviceClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.format.DateTimeParseException

class RemoteTripRepository(
    private val api: TripsApi,
    private val config: BackendConfig,
    private val clock: DeviceClock,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : TripRepository {

    override suspend fun createTrip(normalizedMapsUrl: String): TripResult = withContext(io) {
        val response = try {
            api.createTrip(CreateTripRequest(url = normalizedMapsUrl, siteOrigin = config.qrPublicOrigin))
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            return@withContext TripResult.Failure(TripError.Timeout)
        } catch (e: InterruptedIOException) {
            return@withContext TripResult.Failure(TripError.Timeout)
        } catch (e: IOException) {
            return@withContext TripResult.Failure(TripError.NoConnection)
        } catch (e: Exception) {
            // Malformed JSON on a 200 surfaces here as a serialization exception.
            return@withContext TripResult.Failure(TripError.UntrustedResponse("bad-body"))
        }

        val receivedWall = clock.wallMs()
        val receivedElapsed = clock.elapsedMs()

        if (!response.isSuccessful) {
            val message = response.errorBody()?.use { body ->
                runCatching { NetworkFactory.json.decodeFromString(ApiErrorBody.serializer(), body.string()).error }.getOrNull()
            }
            val error = when (response.code()) {
                400, 422 -> TripError.InvalidLink(message)
                else -> TripError.Server(response.code(), message)
            }
            return@withContext TripResult.Failure(error)
        }

        val body = response.body()
        val id = body?.id
        val tripUrl = body?.tripUrl
        val expiresAt = body?.expiresAt?.let(::parseInstant)
        if (id.isNullOrBlank() || tripUrl.isNullOrBlank() || expiresAt == null) {
            return@withContext TripResult.Failure(TripError.UntrustedResponse("missing-fields"))
        }
        if (!isTrustedTripUrl(tripUrl, id)) {
            return@withContext TripResult.Failure(TripError.UntrustedResponse("untrusted-host"))
        }

        val serverDate = response.headers().getDate("Date")?.time
        val timeline = ExpiryTimeline.fromServer(
            expiresAtMs = expiresAt,
            serverDateMs = serverDate,
            deviceWallMs = receivedWall,
            elapsedMs = receivedElapsed,
            bootId = clock.bootId(),
        )
        TripResult.Success(
            TemporaryTrip(
                id = id,
                tripUrl = tripUrl,
                sourceUrl = normalizedMapsUrl,
                expiresAtMs = expiresAt,
                timeline = timeline,
            ),
        )
    }

    /** The QR must contain `<trusted host>/trip/<id>` and nothing else. */
    internal fun isTrustedTripUrl(tripUrl: String, id: String): Boolean {
        val url = tripUrl.toHttpUrlOrNull() ?: return false
        if (!url.isHttps && !config.allowCleartext) return false
        if (url.host.lowercase() !in config.trustedTripHosts) return false
        val segments = url.pathSegments
        return segments.size == 2 && segments[0] == "trip" && segments[1] == id && url.query == null
    }

    private fun parseInstant(value: String): Long? = try {
        Instant.parse(value).toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
