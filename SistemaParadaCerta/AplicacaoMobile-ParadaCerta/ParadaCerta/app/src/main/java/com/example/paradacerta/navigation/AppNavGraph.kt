package com.example.paradacerta.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.models.DevidaReservaExtra
import java.text.NumberFormat
import java.util.Locale
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
import com.example.paradacerta.screens.profile.PaymentMethodsScreen
import com.example.paradacerta.screens.profile.VeiculoManagementScreen
import com.example.paradacerta.viewmodel.AvaliacaoViewModel
import com.example.paradacerta.screens.profile.ProfileScreen
import com.example.paradacerta.screens.register.RegisterScreen
import com.example.paradacerta.screens.login.ForgotPasswordScreen
import com.example.paradacerta.screens.login.LoginScreen
import com.example.paradacerta.notifications.NotificationPreferencesManager
import com.example.paradacerta.notifications.ReservationNotificationScheduler
import com.example.paradacerta.viewmodel.UserViewModel
import com.example.paradacerta.viewmodel.SaveViewModel
import com.example.paradacerta.viewmodel.DeleteViewModel
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.Endereco
import com.example.paradacerta.models.SessaoAtiva
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
    object PaymentMethods : Screen(route = "payment_methods")
    object VeiculoManagement : Screen(route = "veiculo_management")
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
    val context = LocalContext.current
    val userData by userViewModel.userData.collectAsState()
    val veiculosData by userViewModel.veiculosData.collectAsState()
    val enderecoData by userViewModel.enderecoData.collectAsState()

    var showAlarmPermissionDialog by remember { mutableStateOf(false) }

    if (showAlarmPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmPermissionDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Permissão de alarme necessária") },
            text = {
                Text(
                    "Para receber notificações no horário exato da sua reserva, " +
                    "habilite a permissão de alarmes e lembretes nas configurações do app."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showAlarmPermissionDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                }) { Text("Abrir configurações") }
            },
            dismissButton = {
                TextButton(onClick = { showAlarmPermissionDialog = false }) { Text("Agora não") }
            }
        )
    }

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
                },
                onBackToLogin = {
                    navController.popBackStack()
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
                onScannerClick = {
                    navController.navigate(Screen.Scanner.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onMapClick = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
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
            val devidaReservaExtra by userViewModel.devidaReservaExtra.collectAsState()
            val divida = devidaReservaExtra
            if (divida != null) {
                AcessoBloqueadoScreen(
                    titulo = "Mapa bloqueado",
                    divida = divida,
                    onPagar = {
                        navController.navigate(
                            Screen.Payment.createRoute(
                                divida.sessaoId, divida.valor,
                                divida.nomeEstacionamento, divida.pixKey
                            )
                        )
                    },
                    onVoltar = { navController.popBackStack() }
                )
            } else {
                MapScreen(
                    onParkingClick = { parkingId ->
                        navController.navigate(Screen.ParkingDetails.createRoute(parkingId))
                    }
                )
            }
        }

        // Aba de scanner (substituiu Favoritos)
        composable(Screen.Scanner.route) {
            val devidaReservaExtra by userViewModel.devidaReservaExtra.collectAsState()
            val divida = devidaReservaExtra
            if (divida != null) {
                AcessoBloqueadoScreen(
                    titulo = "Scanner bloqueado",
                    divida = divida,
                    onPagar = {
                        navController.navigate(
                            Screen.Payment.createRoute(
                                divida.sessaoId, divida.valor,
                                divida.nomeEstacionamento, divida.pixKey
                            )
                        )
                    },
                    onVoltar = { navController.popBackStack() }
                )
            } else {
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
        }

        composable(
            route = Screen.Payment.route,
            arguments = listOf(
                navArgument("sessaoId") { type = NavType.StringType },
                navArgument("valor")    { type = NavType.StringType },
                navArgument("nome")     { type = NavType.StringType },
                navArgument("pixKey")   { type = NavType.StringType }
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
                sessaoId      = sessaoId,
                valor         = valor,
                nome          = nome,
                pixKey        = pixKey,
                userViewModel = userViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = { estId ->
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
            val addressStateSave by saveViewModel.addressStateSave.collectAsState()
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
                endereco = enderecoData ?: Endereco(
                    cep = "", logradouro = "", numero = "", complemento = "",
                    bairro = "", cidade = "", estado = ""
                ),
                onSaveChanges = { nome, email, senha, dataNascimento, numeroCelular,
                                  cep, logradouro, numero, complemento, bairro, cidade, estado ->
                    val cpf = userData?.cpf ?: ""
                    if (cpf.isNotEmpty()) {
                        saveViewModel.saveUser(
                            nome = nome, email = email,
                            senha = senha,
                            cpf = cpf,
                            dataNascimento = dataNascimento, numeroCelular = numeroCelular,
                            cep = cep, logradouro = logradouro, numero = numero,
                            complemento = complemento, bairro = bairro, cidade = cidade, estado = estado
                        )
                    }
                },
                onDeleteAccount = {
                    userData?.cpf?.let { cpf -> deleteViewModel.deleteAccount(cpf) }
                },
                saveState = saveState,
                deleteState = deleteState,
                addressState = addressStateSave,
                onFetchCep = saveViewModel::fetchCep
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                cliente = userData,
                veiculos = veiculosData,
                endereco = enderecoData,
                onConfigClick = { navController.navigate(Screen.Config.route) },
                onPaymentMethodsClick = { navController.navigate(Screen.PaymentMethods.route) },
                onVeiculosClick = { navController.navigate(Screen.VeiculoManagement.route) },
                onExitClick = {
                    userViewModel.clearUser()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
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
                cpf = userData?.cpf ?: "",
                veiculos = veiculosData,
                onReservaFeita = { sessao ->
                    if (NotificationPreferencesManager.isReservasEnabled(context)) {
                        val agendado = ReservationNotificationScheduler.schedule(context, sessao)
                        if (!agendado && sessao.horarioReserva != null) {
                            showAlarmPermissionDialog = true
                        }
                    }
                    userViewModel.iniciarSessao(sessao)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Gerenciamento de veículos ─────────────────────────────────────────
        composable(Screen.VeiculoManagement.route) {
            VeiculoManagementScreen(
                cpf = userData?.cpf ?: "",
                onBackClick = { navController.popBackStack() },
                onVeiculosAlterados = {
                    userData?.cpf?.let { cpf -> userViewModel.atualizarDados(cpf) }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcessoBloqueadoScreen(
    titulo: String,
    divida: DevidaReservaExtra,
    onPagar: () -> Unit,
    onVoltar: () -> Unit
) {
    val moeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    titleContentColor = MaterialTheme.colorScheme.onError,
                    navigationIconContentColor = MaterialTheme.colorScheme.onError
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Pagamento pendente",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Você possui uma cobrança de ${moeda.format(divida.valor)} pelo tempo excedente na reserva de ${divida.nomeEstacionamento}.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Regularize o pagamento para usar esta função.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onPagar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Pagar ${moeda.format(divida.valor)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onVoltar, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar")
            }
        }
    }
}
