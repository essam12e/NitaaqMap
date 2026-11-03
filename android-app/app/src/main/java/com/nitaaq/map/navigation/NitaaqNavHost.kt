package com.nitaaq.map.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nitaaq.map.AppContainer
import com.nitaaq.map.presentation.ViewModelFactories
import com.nitaaq.map.presentation.home.HomeScreen
import com.nitaaq.map.presentation.home.HomeViewModel
import com.nitaaq.map.presentation.result.ResultScreen
import com.nitaaq.map.presentation.result.ResultViewModel
import com.nitaaq.map.presentation.settings.SettingsScreen
import com.nitaaq.map.presentation.settings.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val RESULT = "result"
    const val SETTINGS = "settings"
}

@Composable
fun NitaaqNavHost(
    container: AppContainer,
    reducedMotion: Boolean,
    homeViewModelProvider: @Composable () -> HomeViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val duration = if (reducedMotion) 0 else 320
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        // Slides follow reading direction (Start/End are RTL-aware).
        enterTransition = {
            if (reducedMotion) EnterTransition.None
            else slideIntoContainer(SlideDirection.Start, tween(duration)) { it / 5 } + fadeIn(tween(duration))
        },
        exitTransition = { if (reducedMotion) ExitTransition.None else fadeOut(tween(duration / 2)) },
        popEnterTransition = { if (reducedMotion) EnterTransition.None else fadeIn(tween(duration)) },
        popExitTransition = {
            if (reducedMotion) ExitTransition.None
            else slideOutOfContainer(SlideDirection.End, tween(duration)) { it / 5 } + fadeOut(tween(duration))
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModelProvider(),
                onOpenResult = { navController.navigate(Routes.RESULT) { launchSingleTop = true } },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable(Routes.RESULT) {
            val vm: ResultViewModel = viewModel(factory = ViewModelFactories.result(container))
            val homeVm = homeViewModelProvider()
            ResultScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onNewLink = {
                    homeVm.clearInput()
                    if (!navController.popBackStack(Routes.HOME, inclusive = false)) {
                        navController.navigate(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = ViewModelFactories.settings(container))
            SettingsScreen(
                viewModel = vm,
                apiHost = container.backendConfig.apiBaseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
