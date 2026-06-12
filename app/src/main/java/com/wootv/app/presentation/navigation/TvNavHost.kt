package com.wootv.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wootv.app.presentation.screens.epg.EpgScreen
import com.wootv.app.presentation.screens.home.HomeScreen
import com.wootv.app.presentation.screens.player.PlayerScreen
import com.wootv.app.presentation.screens.search.SearchScreen
import com.wootv.app.presentation.screens.splash.SplashScreen

@Composable
fun TvNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onNavigate = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId))
                },
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("channelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: return@composable
            PlayerScreen(
                channelId = channelId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Epg.route,
            arguments = listOf(navArgument("channelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getLong("channelId") ?: return@composable
            EpgScreen(
                channelId = channelId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onChannelClick = { channelId ->
                    navController.navigate(Screen.Player.createRoute(channelId)) {
                        popUpTo(Screen.Search.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
