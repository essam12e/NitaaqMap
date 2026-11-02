package com.nitaaq.map.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nitaaq.map.AppContainer
import com.nitaaq.map.presentation.home.HomeViewModel
import com.nitaaq.map.presentation.result.ResultViewModel
import com.nitaaq.map.presentation.settings.SettingsViewModel

object ViewModelFactories {
    fun home(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { HomeViewModel(createSavedStateHandle(), c.createTrip, c.activeTripStore, c.clock) }
    }

    fun result(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { ResultViewModel(c.activeTripStore, c.clock, c.createTrip, c.qrExporter) }
    }

    fun settings(c: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer { SettingsViewModel(c.settings) }
    }
}
