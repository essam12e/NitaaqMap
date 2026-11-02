package com.nitaaq.map.presentation.result

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nitaaq.map.core.domain.CreateTripUseCase
import com.nitaaq.map.core.domain.ExpiryPhase
import com.nitaaq.map.core.domain.TemporaryTrip
import com.nitaaq.map.core.domain.TripResult
import com.nitaaq.map.core.qr.QrMatrix
import com.nitaaq.map.core.utils.DeviceClock
import com.nitaaq.map.data.ActiveTripStore
import com.nitaaq.map.presentation.Countdown
import com.nitaaq.map.presentation.UiError
import com.nitaaq.map.presentation.countdownFlow
import com.nitaaq.map.presentation.toUiError
import com.nitaaq.map.utils.QrExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val loaded: Boolean = false,
    val trip: TemporaryTrip? = null,
    val qr: QrMatrix? = null,
    val countdown: Countdown? = null,
    val renewing: Boolean = false,
    val renewError: UiError? = null,
    val busyExporting: Boolean = false,
)

sealed interface ResultEvent {
    data class LaunchShare(val intent: Intent) : ResultEvent
    data object Saved : ResultEvent
    data object SaveFailed : ResultEvent
    data object ShareFailed : ResultEvent
    data object Expired : ResultEvent
    data object Renewed : ResultEvent
    data object RenewFailed : ResultEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModel(
    private val store: ActiveTripStore,
    private val clock: DeviceClock,
    private val createTrip: CreateTripUseCase,
    private val exporter: QrExporter,
) : ViewModel() {

    /** The last trip seen, kept in memory after it expires (the store is cleared at expiry). */
    private val shownTrip = MutableStateFlow<TemporaryTrip?>(null)
    private val loaded = MutableStateFlow(false)
    private val ui = MutableStateFlow(ResultUiState())

    private val _events = Channel<ResultEvent>(Channel.BUFFERED)
    val events: Flow<ResultEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            store.activeTrip.collect { stored ->
                if (stored != null) shownTrip.value = stored
                loaded.value = true
            }
        }
        // When the countdown hits zero: notify once and wipe the stored link.
        viewModelScope.launch {
            shownTrip.filterNotNull()
                .flatMapLatest { trip -> trip.countdownFlow(clock).map { trip.id to it.phase } }
                .distinctUntilChanged()
                .collect { (_, phase) ->
                    if (phase == ExpiryPhase.Expired) {
                        store.clear()
                        _events.send(ResultEvent.Expired)
                    }
                }
        }
    }

    private val qrFlow = shownTrip.map { it?.let { trip -> QrMatrix.encode(trip.tripUrl) } }

    val state: StateFlow<ResultUiState> = combine(
        shownTrip,
        qrFlow,
        shownTrip.flatMapLatest { it?.countdownFlow(clock) ?: flowOf(null) },
        loaded,
        ui,
    ) { trip, qr, countdown, isLoaded, extra ->
        extra.copy(loaded = isLoaded, trip = trip, qr = qr, countdown = countdown)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState())

    /** New signed link for the same Google Maps destination. */
    fun renew() {
        val source = shownTrip.value?.sourceUrl ?: return
        if (ui.value.renewing || source.isBlank()) return
        ui.update { it.copy(renewing = true, renewError = null) }
        viewModelScope.launch {
            when (val result = createTrip(source)) {
                is TripResult.Success -> {
                    store.save(result.trip)
                    shownTrip.value = result.trip
                    ui.update { it.copy(renewing = false) }
                    _events.send(ResultEvent.Renewed)
                }
                is TripResult.Failure -> {
                    ui.update { it.copy(renewing = false, renewError = result.error.toUiError()) }
                    _events.send(ResultEvent.RenewFailed)
                }
            }
        }
    }

    fun share() = export { trip ->
        exporter.shareIntent(trip).fold(
            onSuccess = { ResultEvent.LaunchShare(it) },
            onFailure = { ResultEvent.ShareFailed },
        )
    }

    fun save() = export { trip ->
        exporter.saveToGallery(trip).fold(onSuccess = { ResultEvent.Saved }, onFailure = { ResultEvent.SaveFailed })
    }

    /** Leaving for a different link: forget the current one. */
    fun discard() {
        viewModelScope.launch { store.clear() }
    }

    private fun export(block: suspend (TemporaryTrip) -> ResultEvent) {
        val trip = shownTrip.value ?: return
        val countdown = state.value.countdown
        // Never hand out an expired code.
        if (countdown == null || countdown.phase == ExpiryPhase.Expired || ui.value.busyExporting) return
        ui.update { it.copy(busyExporting = true) }
        viewModelScope.launch {
            val event = block(trip)
            ui.update { it.copy(busyExporting = false) }
            _events.send(event)
        }
    }
}
