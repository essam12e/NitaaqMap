package com.nitaaq.map

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nitaaq.map.navigation.NitaaqNavHost
import com.nitaaq.map.presentation.ViewModelFactories
import com.nitaaq.map.presentation.home.HomeViewModel
import com.nitaaq.map.ui.theme.NitaaqTheme
import com.nitaaq.map.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private var themeReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the splash only until the saved theme is known (avoids a light→dark flash). Usually < 50 ms.
        splash.setKeepOnScreenCondition { !themeReady }
        enableEdgeToEdge()

        val container = (application as NitaaqApplication).container
        if (savedInstanceState == null) handleShareIntent(intent)

        setContent {
            var initialMode by remember { mutableStateOf<ThemeMode?>(null) }
            LaunchedEffect(Unit) {
                initialMode = container.settings.themeMode.first()
                themeReady = true
            }
            val mode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = initialMode ?: ThemeMode.System)
            val reducedMotion = remember { !ValueAnimator.areAnimatorsEnabled() }

            NitaaqTheme(themeMode = mode, reducedMotion = reducedMotion) {
                // Activity-scoped so Home and Result share it (e.g. "new code" clears the input).
                val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactories.home(container))
                LaunchedEffect(homeViewModel) {
                    container.incomingText.collect { text ->
                        homeViewModel.onInputChange(text)
                        container.consumeIncomingText()
                    }
                }
                NitaaqNavHost(
                    container = container,
                    reducedMotion = reducedMotion,
                    homeViewModelProvider = { homeViewModel },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /** Text shared from Google Maps ("Share" → Nitaaq Map) pre-fills the link field. */
    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return
        (application as NitaaqApplication).container.onTextShared(text.take(2_000))
    }
}
