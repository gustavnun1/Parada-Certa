package com.example.paradacerta.screens.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.ui.theme.CinzaMedio
import com.example.paradacerta.viewmodel.ForgotPasswordViewModel
import com.example.paradacerta.viewmodel.RecuperacaoEtapa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.etapa) {
        if (state.etapa == RecuperacaoEtapa.SUCESSO) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar senha") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.etapa == RecuperacaoEtapa.AGUARDANDO_CODIGO) {
                            viewModel.voltarParaSolicitar()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = state.etapa,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            modifier = Modifier.padding(padding),
            label = "recuperacao_etapa"
        ) { etapa ->
            when (etapa) {
                RecuperacaoEtapa.SOLICITAR, RecuperacaoEtapa.SUCESSO ->
                    Passo1Solicitar(
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onSolicitar = { login, isCpf -> viewModel.solicitarCodigo(login, isCpf) }
                    )
                RecuperacaoEtapa.AGUARDANDO_CODIGO ->
                    Passo2Confirmar(
                        mensagemEnvio = state.mensagemSucesso,
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onConfirmar = { codigo, novaSenha -> viewModel.confirmarCodigo(codigo, novaSenha) },
                        onReenviar = { viewModel.voltarParaSolicitar() }
                    )
            }
        }
    }
}

// ── Passo 1: informe e-mail ou CPF ───────────────────────────────────────────

@Composable
private fun Passo1Solicitar(
    isLoading: Boolean,
    errorMessage: String?,
    onSolicitar: (login: String, isCpf: Boolean) -> Unit
) {
    var isCpfMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(isCpfMode) {
        email = ""
        cpf = TextFieldValue("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LockReset,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Recuperar senha", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Informe seu e-mail ou CPF cadastrado.\nVamos enviar um código de verificação.",
            color = CinzaMedio,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilterChip(
                selected = !isCpfMode,
                onClick = { isCpfMode = false },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            FilterChip(
                selected = isCpfMode,
                onClick = { isCpfMode = true },
                label = { Text("CPF") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isCpfMode) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                    var newCursor = 0; var digCount = 0
                    for (i in formatted.indices) {
                        if (formatted[i].isDigit()) digCount++
                        if (digCount >= oldDigitsBeforeCursor) { newCursor = i + 1; break }
                    }
                    cpf = TextFieldValue(formatted, TextRange(newCursor.coerceAtMost(formatted.length)))
                },
                label = { Text("CPF") },
                placeholder = { Text("000.000.000-00") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val loginValue = if (isCpfMode) cpf.text.filter { it.isDigit() } else email
                onSolicitar(loginValue, isCpfMode)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviando...")
            } else {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar código")
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Passo 2: insira código + nova senha ──────────────────────────────────────

@Composable
private fun Passo2Confirmar(
    mensagemEnvio: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onConfirmar: (codigo: String, novaSenha: String) -> Unit,
    onReenviar: () -> Unit
) {
    var codigo by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var senhaVisible by remember { mutableStateOf(false) }
    var confirmarVisible by remember { mutableStateOf(false) }
    var senhaError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Código enviado!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        if (mensagemEnvio != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    Text(mensagemEnvio, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Campo do código — 6 dígitos, teclado numérico
        OutlinedTextField(
            value = codigo,
            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) codigo = it },
            label = { Text("Código de 6 dígitos") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = novaSenha,
            onValueChange = { novaSenha = it; senhaError = null },
            label = { Text("Nova senha") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { senhaVisible = !senhaVisible }) {
                    Icon(if (senhaVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            },
            visualTransformation = if (senhaVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = senhaError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmarSenha,
            onValueChange = { confirmarSenha = it; senhaError = null },
            label = { Text("Confirmar nova senha") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { confirmarVisible = !confirmarVisible }) {
                    Icon(if (confirmarVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            },
            visualTransformation = if (confirmarVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = senhaError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        senhaError?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (codigo.length != 6) { senhaError = "O código deve ter 6 dígitos"; return@Button }
                if (novaSenha.length < 6) { senhaError = "Senha deve ter pelo menos 6 caracteres"; return@Button }
                if (novaSenha != confirmarSenha) { senhaError = "As senhas não coincidem"; return@Button }
                onConfirmar(codigo, novaSenha)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verificando...")
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Redefinir senha")
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onReenviar) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Não recebi o código — reenviar")
        }
    }
}
