package com.example.paradacerta.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
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
    userViewModel: UserViewModel
) {
    // Modo de login: false = e-mail, true = CPF
    var isCpfMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()

    // Reseta os campos ao trocar de modo
    LaunchedEffect(isCpfMode) {
        email = ""
        cpf = TextFieldValue("")
    }

    LaunchedEffect(loginState.isSuccess) {
        if (loginState.isSuccess && loginState.userData != null) {
            loginState.userData?.let { userData ->
                userViewModel.setUser(
                    cliente = userData.cliente,
                    veiculo = userData.veiculo,
                    endereco = userData.endereco
                )
                userViewModel.restaurarSessaoAtiva(userData.cliente.cpf)
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

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle E-mail / CPF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = !isCpfMode,
                    onClick = { isCpfMode = false },
                    label = { Text("E-mail") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = isCpfMode,
                    onClick = { isCpfMode = true },
                    label = { Text("CPF") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isCpfMode) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = cpf,
                    onValueChange = { newValue ->
                        val digits = newValue.text.filter { it.isDigit() }.take(11)
                        val formatted = when {
                            digits.length <= 3 -> digits
                            digits.length <= 6 -> "${digits.substring(0, 3)}.${digits.substring(3)}"
                            digits.length <= 9 -> "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6)}"
                            else -> "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}"
                        }
                        val oldDigitsBeforeCursor = newValue.text.take(newValue.selection.start).count { it.isDigit() }
                        var newCursorPosition = 0
                        var digitsCount = 0
                        for (i in formatted.indices) {
                            if (formatted[i].isDigit()) digitsCount++
                            if (digitsCount >= oldDigitsBeforeCursor) {
                                newCursorPosition = i + 1
                                break
                            }
                        }
                        if (newCursorPosition > formatted.length) newCursorPosition = formatted.length
                        cpf = TextFieldValue(text = formatted, selection = TextRange(newCursorPosition))
                    },
                    label = { Text("CPF") },
                    placeholder = { Text("000.000.000-00") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val loginValue = if (isCpfMode) cpf.text.filter { it.isDigit() } else email
                    viewModel.loginUser(login = loginValue, senha = password, isCpf = isCpfMode)
                },
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
