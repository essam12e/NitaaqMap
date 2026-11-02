package com.nitaaq.map.core.config

/**
 * Single source of truth for where the app talks to. Nothing else in the code base contains a
 * host name. The Android app builds this from Gradle properties (see `android-app/gradle.properties`),
 * so moving from Vercel to Cloudflare is a build-config change, not a code change.
 *
 * @property apiBaseUrl base URL that hosts `POST api/trips` (must end with `/`).
 * @property trustedTripHosts hosts allowed inside the QR. A response pointing anywhere else is
 *   rejected, so a misconfigured or compromised backend cannot make the app print a QR to an
 *   arbitrary site.
 * @property qrPublicOrigin optional stable origin sent as `siteOrigin`; the server then builds
 *   `tripUrl` on that origin (e.g. a permanent `https://go.example.com`). Null = server's own origin.
 * @property allowCleartext only for local development against `http://10.0.2.2:3000/`.
 */
data class BackendConfig(
    val apiBaseUrl: String,
    val trustedTripHosts: Set<String>,
    val qrPublicOrigin: String? = null,
    val allowCleartext: Boolean = false,
    val connectTimeoutMs: Long = 10_000,
    val readTimeoutMs: Long = 15_000,
) {
    init {
        require(apiBaseUrl.endsWith("/")) { "apiBaseUrl must end with '/'" }
        require(trustedTripHosts.isNotEmpty()) { "trustedTripHosts must not be empty" }
        require(allowCleartext || apiBaseUrl.startsWith("https://")) { "apiBaseUrl must use https" }
    }

    companion object {
        fun fromProperties(
            apiBaseUrl: String,
            trustedTripHostsCsv: String,
            qrPublicOrigin: String?,
            allowCleartext: Boolean,
        ) = BackendConfig(
            apiBaseUrl = apiBaseUrl.trim().let { if (it.endsWith("/")) it else "$it/" },
            trustedTripHosts = trustedTripHostsCsv.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
            qrPublicOrigin = qrPublicOrigin?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() },
            allowCleartext = allowCleartext,
        )
    }
}
