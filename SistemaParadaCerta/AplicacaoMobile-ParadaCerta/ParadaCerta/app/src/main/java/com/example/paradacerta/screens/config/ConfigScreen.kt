package com.example.paradacerta.screens.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.paradacerta.models.Cliente
import com.example.paradacerta.models.Endereco
import com.example.paradacerta.validation.UserFieldValidator
import com.example.paradacerta.viewmodel.AddressStateSave
import com.example.paradacerta.viewmodel.DeleteState
import com.example.paradacerta.viewmodel.SaveState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    cliente: Cliente,
    endereco: Endereco?,
    onSaveChanges: (
        nome: String,
        email: String,
        senha: String,
        dataNascimento: String,
        numeroCelular: String,
        cep: String,
        logradouro: String,
        numero: String,
        complemento: String,
        bairro: String,
        cidade: String,
        estado: String
    ) -> Unit,
    onDeleteAccount: () -> Unit,
    saveState: SaveState = SaveState(),
    deleteState: DeleteState = DeleteState(),
    addressState: AddressStateSave = AddressStateSave(),
    onFetchCep: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var nome by remember { mutableStateOf(cliente.nome) }
    var email by remember { mutableStateOf(cliente.email) }
    var senha by remember { mutableStateOf("") }
    var numeroCelular by remember { mutableStateOf(TextFieldValue(formatTelefone(cliente.numeroCelular))) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var dataNascimento by remember {
        mutableStateOf(TextFieldValue(cliente.dataNascimento?.let { dateFormat.format(it) } ?: ""))
    }

    var cep by remember { mutableStateOf(UserFieldValidator.somenteDigitos(endereco?.cep ?: "")) }
    var logradouro by remember { mutableStateOf(endereco?.logradouro ?: "") }
    var numero by remember { mutableStateOf(endereco?.numero ?: "") }
    var complemento by remember { mutableStateOf(endereco?.complemento ?: "") }
    var bairro by remember { mutableStateOf(endereco?.bairro ?: "") }
    var cidade by remember { mutableStateOf(endereco?.cidade ?: "") }
    var estado by remember { mutableStateOf(endereco?.estado ?: "") }

    var nomeError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var senhaError by remember { mutableStateOf<String?>(null) }
    var dataError by remember { mutableStateOf<String?>(null) }
    var telefoneError by remember { mutableStateOf<String?>(null) }
    var cepError by remember { mutableStateOf<String?>(null) }
    var numeroError by remember { mutableStateOf<String?>(null) }
    var enderecoError by remember { mutableStateOf<String?>(null) }
    var isLoadingCep by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val nextField = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val doneField = KeyboardActions(onDone = { focusManager.clearFocus() })

    LaunchedEffect(addressState) {
        val recebeuEndereco = addressState.logradouro.isNotBlank() ||
            addressState.bairro.isNotBlank() ||
            addressState.cidade.isNotBlank() ||
            addressState.estado.isNotBlank()
        if (addressState.error == null && !recebeuEndereco) return@LaunchedEffect

        isLoadingCep = false
        cepError = addressState.error
        if (addressState.error == null) {
            logradouro = addressState.logradouro
            bairro = addressState.bairro
            cidade = addressState.cidade
            estado = addressState.estado
            enderecoError = null
        } else {
            logradouro = ""
            bairro = ""
            cidade = ""
            estado = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                title = {
                    Text("Configurações", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Dados Pessoais", style = MaterialTheme.typography.titleMedium) }

            item {
                OutlinedTextField(
                    value = nome,
                    onValueChange = {
                        nome = it
                        nomeError = UserFieldValidator.validarNome(it)
                    },
                    label = { Text("Nome") },
                    isError = nomeError != null,
                    supportingText = { nomeError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = nextField,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it.trim()
                        emailError = UserFieldValidator.validarEmail(email)
                    },
                    label = { Text("E-mail") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = nextField,
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = senha,
                    onValueChange = {
                        senha = it
                        senhaError = UserFieldValidator.validarSenha(it, obrigatoria = false)
                    },
                    label = { Text("Nova Senha (deixe vazio para não alterar)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = nextField,
                    isError = senhaError != null,
                    supportingText = { senhaError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = dataNascimento,
                    onValueChange = { newValue ->
                        dataNascimento = formatMaskedTextFieldValue(newValue, 8, ::formatData)
                        dataError = UserFieldValidator.validarDataNascimento(dataNascimento.text)
                    },
                    label = { Text("Data de Nascimento") },
                    placeholder = { Text("DD/MM/AAAA") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = nextField,
                    isError = dataError != null,
                    supportingText = { dataError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = numeroCelular,
                    onValueChange = { newValue ->
                        numeroCelular = formatMaskedTextFieldValue(newValue, 11, ::formatTelefone)
                        telefoneError = UserFieldValidator.validarTelefone(numeroCelular.text)
                    },
                    label = { Text("Celular") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = nextField,
                    isError = telefoneError != null,
                    supportingText = { telefoneError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Endereço", style = MaterialTheme.typography.titleMedium)
            }

            item {
                OutlinedTextField(
                    value = cep,
                    onValueChange = {
                        val novoCep = UserFieldValidator.somenteDigitos(it).take(8)
                        if (novoCep != cep) {
                            cep = novoCep
                            cepError = UserFieldValidator.validarCep(novoCep)
                            logradouro = ""
                            bairro = ""
                            cidade = ""
                            estado = ""
                            enderecoError = null
                            if (novoCep.length == 8) {
                                isLoadingCep = true
                                cepError = null
                                onFetchCep(novoCep)
                            } else {
                                isLoadingCep = false
                            }
                        }
                    },
                    label = { Text("CEP") },
                    placeholder = { Text("00000000") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = nextField,
                    isError = cepError != null,
                    supportingText = { cepError?.let { Text(it) } },
                    trailingIcon = {
                        if (isLoadingCep) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = logradouro,
                    onValueChange = {},
                    label = { Text("Logradouro") },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = numero,
                    onValueChange = {
                        numero = it.trim().take(10)
                        numeroError = UserFieldValidator.validarNumeroEndereco(numero)
                    },
                    label = { Text("Número") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = nextField,
                    isError = numeroError != null,
                    supportingText = { numeroError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = complemento,
                    onValueChange = { complemento = it.take(60) },
                    label = { Text("Complemento") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = doneField,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = bairro,
                    onValueChange = {},
                    label = { Text("Bairro") },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = cidade,
                    onValueChange = {},
                    label = { Text("Cidade") },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = estado,
                    onValueChange = {},
                    label = { Text("Estado (UF)") },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            enderecoError?.let { erro ->
                item {
                    Text(
                        text = erro,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Button(
                    onClick = {
                        nomeError = UserFieldValidator.validarNome(nome)
                        emailError = UserFieldValidator.validarEmail(email)
                        senhaError = UserFieldValidator.validarSenha(senha, obrigatoria = false)
                        dataError = UserFieldValidator.validarDataNascimento(dataNascimento.text)
                        telefoneError = UserFieldValidator.validarTelefone(numeroCelular.text)
                        cepError = UserFieldValidator.validarCep(cep)
                        numeroError = UserFieldValidator.validarNumeroEndereco(numero)
                        enderecoError = if (
                            logradouro.isBlank() || bairro.isBlank() ||
                            cidade.isBlank() || estado.length != 2
                        ) {
                            "Informe um CEP válido para preencher o endereço."
                        } else null

                        if (listOf(
                                nomeError,
                                emailError,
                                senhaError,
                                dataError,
                                telefoneError,
                                cepError,
                                numeroError,
                                enderecoError
                            ).all { it == null }
                        ) {
                            onSaveChanges(
                                nome.trim(),
                                email.trim(),
                                senha,
                                dataNascimento.text,
                                numeroCelular.text,
                                cep,
                                logradouro,
                                numero,
                                complemento.trim(),
                                bairro,
                                cidade,
                                estado
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saveState.isLoading && !deleteState.isLoading && !isLoadingCep
                ) {
                    if (saveState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Salvar Alterações")
                }
            }

            if (saveState.errorMessage != null) {
                item { ErrorCard(saveState.errorMessage) }
            }

            item {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !saveState.isLoading && !deleteState.isLoading
                ) {
                    if (deleteState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Apagar Conta")
                }
            }

            if (deleteState.errorMessage != null) {
                item { ErrorCard(deleteState.errorMessage) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAccount()
                }) {
                    Text("Confirmar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Excluir Conta") },
            text = { Text("Tem certeza que deseja apagar sua conta? Essa ação não pode ser desfeita.") }
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatData(valor: String): String {
    val digits = UserFieldValidator.somenteDigitos(valor).take(8)
    return when {
        digits.length <= 2 -> digits
        digits.length <= 4 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
        else -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
    }
}

private fun formatTelefone(valor: String): String {
    val digits = UserFieldValidator.somenteDigitos(valor).take(11)
    return when {
        digits.length <= 2 -> digits
        digits.length <= 7 -> "(${digits.substring(0, 2)}) ${digits.substring(2)}"
        else -> "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7)}"
    }
}

private fun formatMaskedTextFieldValue(
    newValue: TextFieldValue,
    maxDigits: Int,
    formatter: (String) -> String
): TextFieldValue {
    val digits = UserFieldValidator.somenteDigitos(newValue.text).take(maxDigits)
    val formatted = formatter(digits)
    val digitsBeforeCursor = newValue.text
        .take(newValue.selection.start)
        .count { it.isDigit() }
        .coerceAtMost(digits.length)

    val cursor = findCursorAfterDigits(formatted, digitsBeforeCursor)
    return TextFieldValue(text = formatted, selection = TextRange(cursor))
}

private fun findCursorAfterDigits(formatted: String, digitsBeforeCursor: Int): Int {
    if (digitsBeforeCursor <= 0) return 0

    var digitsCount = 0
    for (index in formatted.indices) {
        if (formatted[index].isDigit()) digitsCount++
        if (digitsCount >= digitsBeforeCursor) return index + 1
    }
    return formatted.length
}
