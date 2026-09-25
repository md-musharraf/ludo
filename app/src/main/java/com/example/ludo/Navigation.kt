package com.example.ludo

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ludo.model.AiDifficulty
import com.example.ludo.ui.screens.GameScreen
import com.example.ludo.ui.screens.HomeScreen
import com.example.ludo.ui.screens.SplashScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Splash,
        enterTransition = { fadeIn(animationSpec = tween(350)) },
        exitTransition = { fadeOut(animationSpec = tween(350)) }
    ) {
        composable<Splash> {
            SplashScreen(onSplashFinished = {
                navController.navigate(Home) {
                    popUpTo(Splash) { inclusive = true }
                }
            })
        }
        composable<Home> {
            HomeScreen(onStartGame = { playerCount, isVsAI, aiDifficulty ->
                // Single-top guards against a double tap stacking two matches.
                navController.navigate(Game(playerCount, isVsAI, aiDifficulty.name)) {
                    launchSingleTop = true
                }
            })
        }
        composable<Game> { backStackEntry ->
            // Route arguments are untrusted input (deep links, restored state): sanitize them.
            val params = backStackEntry.toRoute<Game>()
            GameScreen(
                playerCount = params.playerCount.coerceIn(2, 4),
                isVsAI = params.isVsAI,
                aiDifficulty = AiDifficulty.fromName(params.aiDifficulty),
                onNavigateHome = {
                    if (!navController.popBackStack(Home, inclusive = false)) {
                        navController.navigate(Home) { popUpTo(0) }
                    }
                }
            )
        }
    }
}
