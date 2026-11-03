package com.nitaaq.map

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nitaaq.map.core.domain.CreateTripUseCase
import com.nitaaq.map.core.domain.ExpiryTimeline
import com.nitaaq.map.core.domain.TemporaryTrip
import com.nitaaq.map.core.domain.TripError
import com.nitaaq.map.core.domain.TripRepository
import com.nitaaq.map.core.domain.TripResult
import com.nitaaq.map.data.ActiveTripStore
import com.nitaaq.map.presentation.home.HomeViewModel
import com.nitaaq.map.presentation.home.LinkFieldState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HomeViewModelTest {

    private class GatedRepository : TripRepository {
        var calls = 0
        val gate = CompletableDeferred<TripResult>()
        override suspend fun createTrip(normalizedMapsUrl: String): TripResult {
            calls++
            return gate.await()
        }
    }

    private val clock = FakeClock()
    private lateinit var store: ActiveTripStore

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store = ActiveTripStore(ApplicationProvider.getApplicationContext())
        runBlocking { store.clear() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun trip() = TemporaryTrip(
        id = "a.b",
        tripUrl = "https://nitaaq-map.vercel.app/trip/a.b",
        sourceUrl = "https://maps.app.goo.gl/x",
        expiresAtMs = clock.wall + 300_000,
        timeline = ExpiryTimeline.fromServer(clock.wall + 300_000, clock.wall, clock.wall, clock.elapsed, clock.boot),
    )

    @Test
    fun liveValidationOfTheInput() {
        val vm = HomeViewModel(SavedStateHandle(), CreateTripUseCase(GatedRepository()), store, clock)
        assertEquals(LinkFieldState.Idle, vm.state.value.fieldState)
        vm.onInputChange("https://example.com")
        assertEquals(LinkFieldState.Invalid, vm.state.value.fieldState)
        vm.onInputChange("خذني هنا https://maps.app.goo.gl/AbC")
        assertEquals(LinkFieldState.Valid, vm.state.value.fieldState)
    }

    @Test
    fun repeatedTapsWhileLoadingSendOneRequest() = runBlocking {
        val repo = GatedRepository()
        val vm = HomeViewModel(SavedStateHandle(), CreateTripUseCase(repo), store, clock)
        vm.onInputChange("https://maps.app.goo.gl/AbC")
        repeat(5) { vm.createQr() }
        assertTrue(vm.state.value.isLoading)
        assertEquals(1, repo.calls)
        repo.gate.complete(TripResult.Success(trip()))
        // The DataStore write resumes on its own IO thread, so wait for the
        // view model to finish instead of asserting on the very next line.
        withTimeout(10_000) { vm.state.first { !it.isLoading } }
        assertFalse(vm.state.value.isLoading)
        assertEquals(1, repo.calls)
        // Saved for the result screen / next app launch.
        assertEquals("a.b", store.activeTrip.first()?.id)
    }

    @Test
    fun offlineErrorIsRetryable() = runBlocking {
        val repo = GatedRepository()
        val vm = HomeViewModel(SavedStateHandle(), CreateTripUseCase(repo), store, clock)
        vm.onInputChange("https://maps.app.goo.gl/AbC")
        vm.createQr()
        repo.gate.complete(TripResult.Failure(TripError.NoConnection))
        val error = vm.state.value.error
        assertNotNull(error)
        assertTrue(error!!.retryable)
        assertTrue(error.isConnectivity)
        assertNull(store.activeTrip.first())
    }

    @Test
    fun inputSurvivesProcessDeathViaSavedState() {
        val handle = SavedStateHandle()
        HomeViewModel(handle, CreateTripUseCase(GatedRepository()), store, clock).onInputChange("https://maps.app.goo.gl/AbC")
        val restored = HomeViewModel(SavedStateHandle(mapOf("input" to handle.get<String>("input"))), CreateTripUseCase(GatedRepository()), store, clock)
        assertEquals("https://maps.app.goo.gl/AbC", restored.state.value.input)
        assertEquals(LinkFieldState.Valid, restored.state.value.fieldState)
    }
}
