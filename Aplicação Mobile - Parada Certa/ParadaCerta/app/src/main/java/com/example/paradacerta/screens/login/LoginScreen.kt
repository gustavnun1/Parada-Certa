package com.example.paradacerta.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.viewmodel.LoginViewModel
import com.example.paradacerta.ui.theme.CinzaMedio
import com.example.paradacerta.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: () -> Unit = {},
    onRegister: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(),
    userViewModel: UserViewModel  // ← Recebe do NavGraph
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by viewModel.loginState.collectAsState()

    // Observa quando o login for bem-sucedido
    LaunchedEffect(loginState.isSuccess) {
        if (loginState.isSuccess && loginState.userData != null) {
            // Salva os dados do usuário no UserViewModel (global)
            loginState.userData?.let { userData ->
                userViewModel.setUser(
                    cliente = userData.cliente,
                    veiculo = userData.veiculo,
                    endereco = userData.endereco
                )
            }
            onLogin()
            viewModel.resetLoginState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Bem-vindo",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Faça login para continuar",
                color = CinzaMedio
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Esqueci minha senha")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.loginUser(email = email, senha = password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loginState.isLoading
            ) {
                if (loginState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Entrar")
                }
            }

            loginState.errorMessage?.let { erro ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Não tem conta?",
                fontSize = 13.sp,
                color = CinzaMedio
            )

            TextButton(onClick = onRegister) {
                Text("Criar conta")
            }
        }
    }
}