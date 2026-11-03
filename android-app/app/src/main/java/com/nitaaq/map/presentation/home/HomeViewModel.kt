package com.nitaaq.map.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nitaaq.map.core.domain.CreateTripUseCase
import com.nitaaq.map.core.domain.ExpiryPhase
import com.nitaaq.map.core.domain.LinkCheck
import com.nitaaq.map.core.domain.MapsLinkValidator
import com.nitaaq.map.core.domain.TripResult
import com.nitaaq.map.core.utils.DeviceClock
import com.nitaaq.map.data.ActiveTripStore
import com.nitaaq.map.presentation.Countdown
import com.nitaaq.map.presentation.UiError
import com.nitaaq.map.presentation.countdownFlow
import com.nitaaq.map.presentation.toUiError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LinkFieldState { Idle, Valid, Invalid }

data class HomeUiState(
    val input: String = "",
    val fieldState: LinkFieldState = LinkFieldState.Idle,
    val isLoading: Boolean = false,
    val error: UiError? = null,
)

sealed interface HomeEvent {
    data object OpenResult : HomeEvent
    data object Failed : HomeEvent
}

class HomeViewModel(
    private val savedState: SavedStateHandle,
    private val createTrip: CreateTripUseCase,
    private val store: ActiveTripStore,
    private val clock: DeviceClock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        savedState.get<String>(KEY_INPUT).orEmpty().let { HomeUiState(input = it, fieldState = fieldStateFor(it)) },
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    /** A still-valid QR from earlier (e.g. the app was closed and reopened). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeTrip: StateFlow<Countdown?> = store.activeTrip
        .flatMapLatest { trip -> trip?.countdownFlow(clock, periodMs = 1_000)?.map { it.takeIf { c -> c.phase != ExpiryPhase.Expired } } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onInputChange(value: String) {
        savedState[KEY_INPUT] = value
        _state.update { it.copy(input = value, fieldState = fieldStateFor(value), error = null) }
    }

    fun clearInput() = onInputChange("")

    /** Runs the request once; repeated taps while loading are ignored. */
    fun createQr() {
        val current = _state.value
        if (current.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = createTrip(current.input)) {
                is TripResult.Success -> {
                    store.save(result.trip)
                    _state.update { it.copy(isLoading = false) }
                    _events.send(HomeEvent.OpenResult)
                }
                is TripResult.Failure -> {
                    _state.update { it.copy(isLoading = false, error = result.error.toUiError()) }
                    _events.send(HomeEvent.Failed)
                }
            }
        }
    }

    private fun fieldStateFor(value: String): LinkFieldState = when (MapsLinkValidator.check(value)) {
        LinkCheck.Empty -> LinkFieldState.Idle
        LinkCheck.Invalid -> LinkFieldState.Invalid
        is LinkCheck.Valid -> LinkFieldState.Valid
    }

    private companion object {
        const val KEY_INPUT = "input"
    }
}
