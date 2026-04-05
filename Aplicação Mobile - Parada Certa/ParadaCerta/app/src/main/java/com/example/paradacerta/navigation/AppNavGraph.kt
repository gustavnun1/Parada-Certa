package com.example.paradacerta.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.paradacerta.screens.favorites.FavoritesScreen
import com.example.paradacerta.screens.home.HomeScreen
import com.example.paradacerta.screens.config.ConfigScreen
import com.example.paradacerta.screens.map.MapScreen
import com.example.paradacerta.screens.parking.ParkingDetailsScreen
import com.example.paradacerta.screens.profile.ProfileScreen
import com.example.paradacerta.screens.register.RegisterScreen
import com.example.paradacerta.screens.login.LoginScreen
import com.example.paradacerta.viewmodel.UserViewModel
import com.example.paradacerta.viewmodel.SaveViewModel
import com.example.paradacerta.viewmodel.DeleteViewModel
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.Endereco
import com.example.paradacerta.models.Veiculo
import java.util.Date

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Map : Screen("map")
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
    object Register : Screen(route = "register")
    object Login : Screen(route = "login")
    object Config : Screen(route = "config")
        object ParkingDetails : Screen("parking_details/{parkingId}") {
        fun createRoute(parkingId: Int) = "parking_details/$parkingId"
    }

}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Login.route,
    userViewModel: UserViewModel = viewModel()

) {
    val userData by userViewModel.userData.collectAsState()
    val veiculoData by userViewModel.veiculoData.collectAsState()
    val enderecoData by userViewModel.enderecoData.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Register.route) {
            RegisterScreen(
                onFinishRegister = {
                    navController.navigate(Screen.Login.route) {
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
                },
                userViewModel = userViewModel
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onParkingClick = { parkingId ->
                    navController.navigate(Screen.ParkingDetails.createRoute(parkingId))
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                onParkingClick = { parkingId ->
                    navController.navigate(Screen.ParkingDetails.createRoute(parkingId))
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onParkingClick = { parkingId ->
                    navController.navigate(Screen.ParkingDetails.createRoute(parkingId))
                }
            )
        }

        // Tela de Configuração com Save e Delete
        composable(Screen.Config.route) {
            // ViewModels para Save e Delete
            val saveViewModel: SaveViewModel = viewModel()
            val deleteViewModel: DeleteViewModel = viewModel()

            val saveState by saveViewModel.saveState.collectAsState()
            val deleteState by deleteViewModel.deleteState.collectAsState()

            // Observa quando o save for bem-sucedido
            LaunchedEffect(saveState.isSuccess) {
                if (saveState.isSuccess) {
                    // Atualiza os dados no UserViewModel se necessário
                    // ou simplesmente navega de volta
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Config.route) { inclusive = true }
                    }
                    saveViewModel.resetSaveState()
                }
            }

            // Observa quando a exclusão for bem-sucedida
            LaunchedEffect(deleteState.isSuccess) {
                if (deleteState.isSuccess) {
                    userViewModel.clearUser()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    deleteViewModel.resetDeleteState()
                }
            }

            ConfigScreen(
                cliente = userData ?: Cliente(
                    nome = "",
                    cpf = "",
                    email = "",
                    senha = "",
                    dataNascimento = Date(),
                    numeroCelular = "",
                    placa = ""
                ),
                veiculo = veiculoData ?: Veiculo(
                    nome = "",
                    placa = "",
                    cor = "",
                    responsavel = ""
                ),
                endereco = enderecoData ?: Endereco(
                    cep = "",
                    logradouro = "",
                    numero = "",
                    complemento = "",
                    bairro = "",
                    cidade = "",
                    estado = "",
                    cpfCliente = ""
                ),
                onSaveChanges = { nome, email, senha, dataNascimento, numeroCelular,
                                  modeloVeiculo, corVeiculo, placa,
                                  cep, logradouro, numero, complemento, bairro, cidade, estado ->

                    // Pega o CPF do usuário logado
                    val cpf = userData?.cpf ?: ""

                    if (cpf.isNotEmpty()) {
                        saveViewModel.saveUser(
                            nome = nome,
                            email = email,
                            senha = senha,
                            cpf = cpf,
                            dataNascimento = dataNascimento,
                            numeroCelular = numeroCelular,
                            placa = placa,
                            modeloVeiculo = modeloVeiculo,
                            corVeiculo = corVeiculo,
                            cep = cep,
                            logradouro = logradouro,
                            numero = numero,
                            complemento = complemento,
                            bairro = bairro,
                            cidade = cidade,
                            estado = estado
                        )
                    }
                },
                onDeleteAccount = {
                    // Pega o CPF do usuário logado e deleta a conta
                    userData?.cpf?.let { cpf ->
                        deleteViewModel.deleteAccount(cpf)
                    }
                },
                saveState = saveState,      // ← Passa o estado para mostrar loading/erro
                deleteState = deleteState   // ← Passa o estado para mostrar loading/erro
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                cliente = userData,
                veiculo = veiculoData,
                endereco = enderecoData,
                onConfigClick = {
                    navController.navigate(Screen.Config.route)
                },
                onExitClick = {
                    userViewModel.clearUser()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

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