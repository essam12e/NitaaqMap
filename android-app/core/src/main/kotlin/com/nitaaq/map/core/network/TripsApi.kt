package com.nitaaq.map.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Mirrors `app/api/trips/route.ts` exactly:
 *  - request  `{ "url": string, "siteOrigin"?: string }`
 *  - 200      `{ "id": string, "tripUrl": string, "expiresAt": ISO-8601 string }`
 *  - 400/422  `{ "error": string }` (Arabic message)
 */
interface TripsApi {
    @POST("api/trips")
    suspend fun createTrip(@Body body: CreateTripRequest): Response<CreateTripResponse>
}

@Serializable
data class CreateTripRequest(
    @SerialName("url") val url: String,
    @SerialName("siteOrigin") val siteOrigin: String? = null,
)

@Serializable
data class CreateTripResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("tripUrl") val tripUrl: String? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
)

@Serializable
data class ApiErrorBody(@SerialName("error") val error: String? = null)
