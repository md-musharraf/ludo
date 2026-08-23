package com.example.ludo

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ludo.ui.screens.GameScreen
import com.example.ludo.ui.screens.HomeScreen
import com.example.ludo.ui.screens.SplashScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Splash,
        enterTransition = { fadeIn(animationSpec = tween(500)) },
        exitTransition = { fadeOut(animationSpec = tween(500)) }
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
                navController.navigate(Game(playerCount, isVsAI, aiDifficulty))
            })
        }
        composable<Game> { backStackEntry ->
            val gameParams = backStackEntry.toRoute<Game>()
            GameScreen(
                playerCount = gameParams.playerCount,
                isVsAI = gameParams.isVsAI,
                aiDifficulty = gameParams.aiDifficulty,
                onNavigateHome = {
                    navController.navigate(Home) {
                        popUpTo(Home) { inclusive = true }
                    }
                }
            )
        }
    }
}
