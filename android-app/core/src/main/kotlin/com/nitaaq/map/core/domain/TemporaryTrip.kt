package com.nitaaq.map.core.domain

/**
 * A temporary, server-signed trip link returned by `POST /api/trips`.
 *
 * @property id the signed token (`<base64url payload>.<HMAC signature>`), opaque to the app.
 * @property tripUrl the `/trip/<id>` link that goes into the QR. Never the raw Google Maps link.
 * @property sourceUrl the Google Maps link the user entered (kept so the trip can be renewed).
 * @property expiresAtMs server expiry instant (epoch millis), parsed from `expiresAt`.
 * @property timeline anchors used to count down without trusting the device wall clock.
 */
data class TemporaryTrip(
    val id: String,
    val tripUrl: String,
    val sourceUrl: String,
    val expiresAtMs: Long,
    val timeline: ExpiryTimeline,
)

sealed interface TripError {
    /** Nothing was entered. */
    data object EmptyLink : TripError

    /** Rejected locally or by the server (HTTP 400/422). [serverMessage] is the server's Arabic text. */
    data class InvalidLink(val serverMessage: String? = null) : TripError

    /** No route to the server (offline, DNS, TLS, refused). */
    data object NoConnection : TripError

    /** The server did not answer in time. */
    data object Timeout : TripError

    /** Server answered with an unexpected HTTP status (5xx, 404 after a backend move, ...). */
    data class Server(val httpCode: Int, val serverMessage: String? = null) : TripError

    /** The response was malformed, or the QR link points to a host the app does not trust. */
    data class UntrustedResponse(val reason: String) : TripError
}

sealed interface TripResult {
    data class Success(val trip: TemporaryTrip) : TripResult
    data class Failure(val error: TripError) : TripResult
}
