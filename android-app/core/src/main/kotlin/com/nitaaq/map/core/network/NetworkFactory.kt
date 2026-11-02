package com.nitaaq.map.core.network

import com.nitaaq.map.core.config.BackendConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkFactory {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    fun okHttpClient(config: BackendConfig, userAgent: String): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(config.connectTimeoutMs + config.readTimeoutMs, TimeUnit.MILLISECONDS)
            // POST must never be silently replayed: each call mints a new signed link.
            .retryOnConnectionFailure(false)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", userAgent)
                        .header("Accept", "application/json")
                        .build(),
                )
            }
            .build()

    fun tripsApi(config: BackendConfig, client: OkHttpClient): TripsApi =
        Retrofit.Builder()
            .baseUrl(config.apiBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()
            .create(TripsApi::class.java)
}
