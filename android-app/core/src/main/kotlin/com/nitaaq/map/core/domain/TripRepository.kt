package com.nitaaq.map.core.domain

/**
 * Backend-agnostic contract. The Vercel implementation lives in `data/RemoteTripRepository`;
 * a Cloudflare Worker exposing the same `POST /api/trips` contract needs no new implementation,
 * only a different [com.nitaaq.map.core.config.BackendConfig].
 */
interface TripRepository {
    suspend fun createTrip(normalizedMapsUrl: String): TripResult
}
