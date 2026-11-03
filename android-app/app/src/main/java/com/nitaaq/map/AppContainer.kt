package com.nitaaq.map

import android.content.Context
import com.nitaaq.map.core.config.BackendConfig
import com.nitaaq.map.core.data.RemoteTripRepository
import com.nitaaq.map.core.domain.CreateTripUseCase
import com.nitaaq.map.core.domain.TripRepository
import com.nitaaq.map.core.network.NetworkFactory
import com.nitaaq.map.core.utils.DeviceClock
import com.nitaaq.map.data.ActiveTripStore
import com.nitaaq.map.data.SettingsRepository
import com.nitaaq.map.utils.AndroidClock
import com.nitaaq.map.utils.QrExporter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Manual dependency container: the app is small, so a DI framework would add more than it saves.
 * Swapping the backend = a different [BackendConfig] (from Gradle properties) or another
 * [TripRepository] implementation; no screen or ViewModel changes.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val backendConfig: BackendConfig = BackendConfig.fromProperties(
        apiBaseUrl = BuildConfig.API_BASE_URL,
        trustedTripHostsCsv = BuildConfig.TRUSTED_TRIP_HOSTS,
        qrPublicOrigin = BuildConfig.QR_PUBLIC_ORIGIN,
        allowCleartext = BuildConfig.ALLOW_CLEARTEXT,
    )

    val clock: DeviceClock = AndroidClock(appContext.contentResolver)

    private val httpClient = NetworkFactory.okHttpClient(
        backendConfig,
        userAgent = "NitaaqMap-Android/${BuildConfig.VERSION_NAME}",
    )

    val tripRepository: TripRepository =
        RemoteTripRepository(NetworkFactory.tripsApi(backendConfig, httpClient), backendConfig, clock)

    val createTrip = CreateTripUseCase(tripRepository)
    val activeTripStore = ActiveTripStore(appContext)
    val settings = SettingsRepository(appContext)
    val qrExporter = QrExporter(appContext)

    /** Text shared into the app from Google Maps ("Share" → Nitaaq Map). */
    private val _incomingText = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val incomingText: SharedFlow<String> = _incomingText
    fun onTextShared(text: String) {
        _incomingText.tryEmit(text)
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun consumeIncomingText() = _incomingText.resetReplayCache()
}
