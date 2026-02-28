package com.example.paradacerta.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.paradacerta.screens.favorites.FavoritesScreen
import com.example.paradacerta.screens.home.HomeScreen
import com.example.paradacerta.screens.map.MapScreen
import com.example.paradacerta.screens.parking.ParkingDetailsScreen
import com.example.paradacerta.screens.profile.ProfileScreen
import com.example.paradacerta.screens.register.RegisterScreen
import com.example.paradacerta.screens.login.LoginScreen
/**
 * Rotas de navegação do aplicativo
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Map : Screen("map")
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
    object Register : Screen(route = "register")
    object Login : Screen(route = "login")
    object ParkingDetails : Screen("parking_details/{parkingId}") {
        fun createRoute(parkingId: Int) = "parking_details/$parkingId"
    }
}

/**
 * Grafo de navegação principal do aplicativo
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Register.route) {
            RegisterScreen(
                onFinishRegister = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLogin = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        // Tela Home
        composable(Screen.Home.route) {
            HomeScreen(
                onParkingClick = { parkingId ->
                    navController.navigate(Screen.ParkingDetails.createRoute(parkingId))
                }
            )
        }

        // Tela Mapa
        composable(Screen.Map.route) {
            MapScreen(
                onParkingClick = { parkingId ->
                    navController.navigate(Screen.ParkingDetails.createRoute(parkingId))
                }
            )
        }

        // Tela Favoritos
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onParkingClick = { parkingId ->
                    navController.navigate(Screen.ParkingDetails.createRoute(parkingId))
                }
            )
        }

        // Tela Perfil
        composable(Screen.Profile.route) {
            ProfileScreen()
        }

        // Tela Detalhes do Estacionamento
        composable(
            route = Screen.ParkingDetails.route,
            arguments = listOf(
                navArgument("parkingId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getInt("parkingId") ?: 0
            ParkingDetailsScreen(
                parkingId = parkingId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}