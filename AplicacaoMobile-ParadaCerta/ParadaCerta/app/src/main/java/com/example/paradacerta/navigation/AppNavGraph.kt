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
import com.example.paradacerta.screens.home.HomeScreen
import com.example.paradacerta.screens.rating.RatingScreen
import com.example.paradacerta.screens.scanner.QrScannerScreen
import com.example.paradacerta.screens.payment.PaymentScreen
import com.example.paradacerta.screens.config.ConfigScreen
import com.example.paradacerta.screens.map.MapScreen
import com.example.paradacerta.screens.parking.ParkingDetailsScreen
import com.example.paradacerta.screens.premium.PremiumPaymentScreen
import com.example.paradacerta.screens.premium.PremiumScreen
import com.example.paradacerta.screens.profile.PaymentMethodsScreen
import com.example.paradacerta.viewmodel.AvaliacaoViewModel
import com.example.paradacerta.viewmodel.PremiumPaymentViewModel
import com.example.paradacerta.viewmodel.CancelPremiumViewModel
import com.example.paradacerta.screens.profile.ProfileScreen
import com.example.paradacerta.screens.register.RegisterScreen
import com.example.paradacerta.screens.login.ForgotPasswordScreen
import com.example.paradacerta.screens.login.LoginScreen
import com.example.paradacerta.viewmodel.UserViewModel
import com.example.paradacerta.viewmodel.SaveViewModel
import com.example.paradacerta.viewmodel.DeleteViewModel
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.Endereco
import com.example.paradacerta.models.SessaoAtiva
import com.example.paradacerta.models.Veiculo
import java.util.Date

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Map : Screen("map")
    object Scanner : Screen("scanner")
    object Profile : Screen("profile")
    object Register : Screen(route = "register")
    object Login : Screen(route = "login")
    object ForgotPassword : Screen(route = "forgot_password")
    object Config : Screen(route = "config")
    object Premium : Screen(route = "premium")
    object PremiumPayment : Screen("premium_payment/{planType}/{planPrice}") {
        fun createRoute(planType: String, planPrice: Double) =
            "premium_payment/$planType/$planPrice"
    }
    object PaymentMethods : Screen(route = "payment_methods")
    object Payment : Screen("payment/{sessaoId}/{valor}/{nome}/{pixKey}") {
        fun createRoute(sessaoId: String, valor: Double, nome: String, pixKey: String): String {
            val safeId = sessaoId.ifBlank { "-" }
            val safeNome = java.net.URLEncoder.encode(nome, "UTF-8")
            val safePixKey = java.net.URLEncoder.encode(pixKey.ifBlank { "-" }, "UTF-8")
            return "payment/$safeId/$valor/$safeNome/$safePixKey"
        }
    }
    object ParkingDetails : Screen("parking_details/{parkingId}") {
        fun createRoute(parkingId: Int) = "parking_details/$parkingId"
    }
    object Rating : Screen("rating/{estacionamentoId}/{nomeEstacionamento}/{cpf}") {
        fun createRoute(estacionamentoId: Int, nomeEstacionamento: String, cpf: String): String {
            val safeNome = java.net.URLEncoder.encode(nomeEstacionamento, "UTF-8")
            val safeCpf  = java.net.URLEncoder.encode(cpf, "UTF-8")
            return "rating/$estacionamentoId/$safeNome/$safeCpf"
        }
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
                onForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                userViewModel = userViewModel
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                userViewModel = userViewModel,
                isPremium = userData?.premium ?: false,
                onScannerClick = {
                    navController.navigate(Screen.Scanner.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onPagarEstacionamento = { sessaoId, valor, nome, pixKey ->
                    navController.navigate(Screen.Payment.createRoute(sessaoId, valor, nome, pixKey))
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

        // Aba de scanner (substituiu Favoritos)
        composable(Screen.Scanner.route) {
            QrScannerScreen(
                userViewModel = userViewModel,
                onEntrada = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onPagamento = { sessaoId, valor, nome, pixKey ->
                    navController.navigate(
                        Screen.Payment.createRoute(sessaoId, valor, nome, pixKey)
                    )
                }
            )
        }

        // Tela de pagamento
        composable(
            route = Screen.Payment.route,
            arguments = listOf(
                navArgument("sessaoId") { type = NavType.StringType },
                navArgument("valor") { type = NavType.StringType },
                navArgument("nome") { type = NavType.StringType },
                navArgument("pixKey") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessaoId = (backStackEntry.arguments?.getString("sessaoId") ?: "").let { if (it == "-") "" else it }
            val valor = backStackEntry.arguments?.getString("valor")?.toDoubleOrNull() ?: 0.0
            val nome = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("nome") ?: "", "UTF-8"
            )
            val pixKey = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("pixKey") ?: "-", "UTF-8"
            ).let { if (it == "-") "" else it }

            PaymentScreen(
                sessaoId = sessaoId,
                valor = valor,
                nomeEstacionamento = nome,
                pixKey = pixKey,
                userViewModel = userViewModel,
                onBack = { navController.popBackStack() },
                onPagamentoConfirmado = { estId ->
                    val cpf = userData?.cpf ?: ""
                    navController.navigate(Screen.Rating.createRoute(estId, nome, cpf)) {
                        popUpTo(Screen.Payment.route) { inclusive = true }
                    }
                }
            )
        }

        // Tela de Configuração com Save e Delete
        composable(Screen.Config.route) {
            val saveViewModel: SaveViewModel = viewModel()
            val deleteViewModel: DeleteViewModel = viewModel()

            val saveState by saveViewModel.saveState.collectAsState()
            val deleteState by deleteViewModel.deleteState.collectAsState()

            LaunchedEffect(saveState.isSuccess) {
                if (saveState.isSuccess) {
                    // Recarrega os dados atualizados do backend para que o Perfil mostre as informações novas
                    userData?.cpf?.let { cpf -> userViewModel.atualizarDados(cpf) }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                    saveViewModel.resetSaveState()
                }
            }

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
                    nome = "", cpf = "", email = "", senha = "",
                    dataNascimento = Date(), numeroCelular = "", placa = ""
                ),
                veiculo = veiculoData ?: Veiculo(nome = "", placa = "", cor = ""),
                endereco = enderecoData ?: Endereco(
                    cep = "", logradouro = "", numero = "", complemento = "",
                    bairro = "", cidade = "", estado = ""
                ),
                onSaveChanges = { nome, email, senha, dataNascimento, numeroCelular,
                                  modeloVeiculo, corVeiculo, placa,
                                  cep, logradouro, numero, complemento, bairro, cidade, estado ->
                    val cpf = userData?.cpf ?: ""
                    if (cpf.isNotEmpty()) {
                        saveViewModel.saveUser(
                            nome = nome, email = email,
                            senha = senha.ifEmpty { userData?.senha ?: "" },
                            cpf = cpf,
                            dataNascimento = dataNascimento, numeroCelular = numeroCelular,
                            placa = placa, modeloVeiculo = modeloVeiculo, corVeiculo = corVeiculo,
                            cep = cep, logradouro = logradouro, numero = numero,
                            complemento = complemento, bairro = bairro, cidade = cidade, estado = estado
                        )
                    }
                },
                onDeleteAccount = {
                    userData?.cpf?.let { cpf -> deleteViewModel.deleteAccount(cpf) }
                },
                saveState = saveState,
                deleteState = deleteState
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                cliente = userData,
                veiculo = veiculoData,
                endereco = enderecoData,
                onConfigClick = { navController.navigate(Screen.Config.route) },
                onPremiumClick = { navController.navigate(Screen.Premium.route) },
                onPaymentMethodsClick = { navController.navigate(Screen.PaymentMethods.route) },
                onExitClick = {
                    userViewModel.clearUser()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Premium.route) {
            val cancelPremiumViewModel: CancelPremiumViewModel = viewModel()
            val cancelState by cancelPremiumViewModel.state.collectAsState()

            LaunchedEffect(cancelState.isSuccess) {
                if (cancelState.isSuccess) {
                    userViewModel.setPremium(false)
                    cancelPremiumViewModel.resetState()
                    navController.popBackStack()
                }
            }

            PremiumScreen(
                onBackClick = { navController.popBackStack() },
                isPremium = userData?.premium ?: false,
                onSubscribeMonthly = {
                    navController.navigate(Screen.PremiumPayment.createRoute("MENSAL", 14.90))
                },
                onSubscribeAnnual = {
                    navController.navigate(Screen.PremiumPayment.createRoute("ANUAL", 149.90))
                },
                onCancelPremium = {
                    userData?.cpf?.let { cancelPremiumViewModel.cancelar(it) }
                }
            )
        }

        composable(
            route = Screen.PremiumPayment.route,
            arguments = listOf(
                navArgument("planType") { type = NavType.StringType },
                navArgument("planPrice") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val planType = backStackEntry.arguments?.getString("planType") ?: "MENSAL"
            val planPrice = backStackEntry.arguments?.getString("planPrice")?.toDoubleOrNull() ?: 14.90
            val premiumPaymentViewModel: PremiumPaymentViewModel = viewModel()
            PremiumPaymentScreen(
                planType = planType,
                planPrice = planPrice,
                userViewModel = userViewModel,
                viewModel = premiumPaymentViewModel,
                onBackClick = { navController.popBackStack() },
                onPaymentSuccess = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Premium.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PaymentMethods.route) {
            PaymentMethodsScreen(
                cpf = userData?.cpf ?: "",
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ParkingDetails.route,
            arguments = listOf(navArgument("parkingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getInt("parkingId") ?: 0
            ParkingDetailsScreen(
                parkingId = parkingId,
                isPremium = userData?.premium ?: false,
                cpf = userData?.cpf ?: "",
                modeloVeiculo = veiculoData?.nome ?: "",
                placa = veiculoData?.placa ?: userData?.placa ?: "",
                onReservaFeita = { sessao ->
                    userViewModel.iniciarSessao(sessao)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Tela de avaliação do estacionamento ──────────────────────────────
        composable(
            route = Screen.Rating.route,
            arguments = listOf(
                navArgument("estacionamentoId")  { type = NavType.IntType },
                navArgument("nomeEstacionamento") { type = NavType.StringType },
                navArgument("cpf")               { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val estId = backStackEntry.arguments?.getInt("estacionamentoId") ?: 0
            val nome  = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("nomeEstacionamento") ?: "", "UTF-8"
            )
            val cpf = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("cpf") ?: "", "UTF-8"
            )
            val avaliacaoViewModel: AvaliacaoViewModel = viewModel()
            RatingScreen(
                estacionamentoId = estId,
                nomeEstacionamento = nome,
                cpf = cpf,
                onConcluido = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                viewModel = avaliacaoViewModel
            )
        }
    }
}
