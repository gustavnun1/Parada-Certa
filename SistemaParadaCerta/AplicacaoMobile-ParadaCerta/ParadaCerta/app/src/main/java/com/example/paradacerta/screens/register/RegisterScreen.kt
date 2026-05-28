package com.example.paradacerta.screens.register

import com.example.paradacerta.legal.LegalDocuments
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.screens.common.VEHICLE_BRANDS
import com.example.paradacerta.validation.PlacaValidator
import com.example.paradacerta.validation.UserFieldValidator
import com.example.paradacerta.viewmodel.RegisterViewModel
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

// ---------- Força da senha ----------
private data class PasswordStrength(val label: String, val color: Color, val progress: Float)

private fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength("", Color.Transparent, 0f)

    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when (score) {
        1 -> PasswordStrength("Muito fraca", Color(0xFFD32F2F), 0.2f)
        2 -> PasswordStrength("Fraca", Color(0xFFFF5722), 0.4f)
        3 -> PasswordStrength("Média", Color(0xFFFFA000), 0.6f)
        4 -> PasswordStrength("Forte", Color(0xFF388E3C), 0.8f)
        else -> PasswordStrength("Muito forte", Color(0xFF1B5E20), 1f)
    }
}

private fun isPasswordStrong(password: String): Boolean {
    return password.length >= 8 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isDigit() } &&
            password.any { !it.isLetterOrDigit() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onFinishRegister: () -> Unit = {},
    onBackToLogin: () -> Unit = {},
    viewModel: RegisterViewModel = viewModel()
) {
    var step by remember { mutableStateOf(1) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val nextField = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val doneField = KeyboardActions(onDone = { focusManager.clearFocus() })

    // Dados Pessoais
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var cpf by remember { mutableStateOf(TextFieldValue("")) }
    var dtnascimento by remember { mutableStateOf(TextFieldValue("")) }
    var numCelular by remember { mutableStateOf(TextFieldValue("")) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var cpfError by remember { mutableStateOf<String?>(null) }
    var birthDateError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // Dados do Veículo
    var plate by remember { mutableStateOf("") }
    var selectedVehicleBrand by remember { mutableStateOf("") }
    var expandedVehicleBrand by remember { mutableStateOf(false) }
    val plateIsValid = PlacaValidator.isValida(plate)

    val vehicleTypes = listOf("Carro", "Moto", "Caminhão", "Van")
    val colors = listOf(
        "Branco", "Preto", "Prata", "Cinza", "Vermelho",
        "Azul", "Verde", "Amarelo", "Marrom", "Bege"
    )

    var expandedVehicleType by remember { mutableStateOf(false) }
    var expandedVehicleColor by remember { mutableStateOf(false) }
    var selectedVehicleType by remember { mutableStateOf(vehicleTypes.first()) }
    var selectedVehicleColor by remember { mutableStateOf(colors.first()) }

    // Endereço
    val addressState by viewModel.addressState.collectAsState()
    val registerState by viewModel.registerState.collectAsState()

    var cep by remember { mutableStateOf("") }
    var logradouro by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }

    var isLoadingCep by remember { mutableStateOf(false) }
    var cepError by remember { mutableStateOf<String?>(null) }
    var cepLengthError by remember { mutableStateOf(false) }
    var numeroError by remember { mutableStateOf<String?>(null) }
    var enderecoError by remember { mutableStateOf<String?>(null) }
    var showCidadeDialog by remember { mutableStateOf(false) }
    var cidadeForaCobertura by remember { mutableStateOf("") }

    // Termos de Uso e Política de Privacidade
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Força da senha
    val passwordStrength = evaluatePasswordStrength(password)

    LaunchedEffect(addressState) {
        isLoadingCep = false
        cepError = addressState.error
        if (addressState.error == null) {
            logradouro = addressState.logradouro
            bairro = addressState.bairro
            cidade = addressState.cidade
            estado = addressState.estado
            enderecoError = null
            // Bloqueia cidades fora da cobertura
            if (addressState.cidade.isNotBlank() &&
                !addressState.cidade.trim().equals("São Paulo", ignoreCase = true)
            ) {
                cidadeForaCobertura = addressState.cidade
                showCidadeDialog = true
            }
        } else {
            logradouro = ""
            bairro = ""
            cidade = ""
            estado = ""
        }
    }

    LaunchedEffect(registerState) {
        if (registerState.isSuccess) {
            Toast.makeText(context, "Cadastro realizado com sucesso!", Toast.LENGTH_LONG).show()
            onFinishRegister()
            viewModel.resetRegisterState()
        }
    }

    if (registerState.isLoading) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Cadastrando...") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
            },
            confirmButton = { }
        )
    }

    registerState.errorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.resetRegisterState() },
            title = { Text("Erro no Cadastro") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetRegisterState() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastro - Etapa $step de 4") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step > 1) {
                            step--
                            showError = false
                        } else {
                            onBackToLogin()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── STEP 1 – Dados Pessoais ───────────────────────────────
                if (step == 1) {
                    Text("Dados Pessoais", style = MaterialTheme.typography.headlineSmall)

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = UserFieldValidator.validarNome(it)
                        },
                        label = { Text("Nome completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = nameError != null,
                        supportingText = { nameError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = nextField,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

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

                    // Senha com indicador de força
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = UserFieldValidator.validarSenha(it)
                            confirmPasswordError = if (confirmPassword.isNotEmpty() && it != confirmPassword) {
                                "As senhas não coincidem."
                            } else null
                        },
                        label = { Text("Senha") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = { passwordError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = nextField,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (password.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = { passwordStrength.progress },
                            color = passwordStrength.color,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Força da senha: ${passwordStrength.label}",
                            color = passwordStrength.color,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!isPasswordStrong(password)) {
                            Text(
                                text = "Use ao menos 8 caracteres, maiúscula, minúscula, número e símbolo",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = if (password != it) "As senhas não coincidem." else null
                        },
                        label = { Text("Repetir Senha") },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = confirmPasswordError != null,
                        supportingText = { confirmPasswordError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = nextField,
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar senha" else "Mostrar senha"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text(
                            "As senhas não coincidem",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

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
                            cpfError = UserFieldValidator.validarCpf(formatted)
                        },
                        label = { Text("CPF") },
                        placeholder = { Text("000.000.000-00") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = nextField,
                        isError = cpfError != null,
                        supportingText = { cpfError?.let { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dtnascimento,
                        onValueChange = { newValue ->
                            val digits = newValue.text.filter { it.isDigit() }.take(8)
                            val formatted = when {
                                digits.length <= 2 -> digits
                                digits.length <= 4 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
                                else -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
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
                            dtnascimento = TextFieldValue(text = formatted, selection = TextRange(newCursorPosition))
                            birthDateError = UserFieldValidator.validarDataNascimento(formatted)
                        },
                        label = { Text("Data de Nascimento") },
                        placeholder = { Text("DD/MM/AAAA") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = nextField,
                        isError = birthDateError != null,
                        supportingText = { birthDateError?.let { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = numCelular,
                        onValueChange = { newValue ->
                            val digits = newValue.text.filter { it.isDigit() }.take(11)
                            val formatted = when {
                                digits.length <= 2 -> digits
                                digits.length <= 7 -> "(${digits.substring(0, 2)}) ${digits.substring(2)}"
                                else -> "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7)}"
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
                            numCelular = TextFieldValue(text = formatted, selection = TextRange(newCursorPosition))
                            phoneError = UserFieldValidator.validarTelefone(formatted)
                        },
                        label = { Text("Número do Telefone") },
                        placeholder = { Text("(00) 00000-0000") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = doneField,
                        isError = phoneError != null,
                        supportingText = { phoneError?.let { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── STEP 2 – Dados do Veículo ─────────────────────────────
                if (step == 2) {
                    Text("Dados do Veículo", style = MaterialTheme.typography.headlineSmall)

                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = PlacaValidator.normalizar(it) },
                        label = { Text("Placa") },
                        placeholder = { Text("ABC1234 ou ABC1D23") },
                        isError = plate.isNotBlank() && !plateIsValid,
                        supportingText = if (plate.isNotBlank() && !plateIsValid) {
                            { Text(PlacaValidator.MENSAGEM_FORMATO_INVALIDO) }
                        } else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = doneField,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tipo de veículo
                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleType,
                        onExpandedChange = { expandedVehicleType = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            value = selectedVehicleType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Veículo") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleType)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehicleType,
                            onDismissRequest = { expandedVehicleType = false }
                        ) {
                            vehicleTypes.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedVehicleType = option
                                        expandedVehicleType = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    // Marca do veículo — dropdown (somente seleção)
                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleBrand,
                        onExpandedChange = { expandedVehicleBrand = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            value = selectedVehicleBrand,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Marca do Veículo") },
                            placeholder = { Text("Selecione a marca") },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleBrand)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehicleBrand,
                            onDismissRequest = { expandedVehicleBrand = false },
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            VEHICLE_BRANDS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedVehicleBrand = option
                                        expandedVehicleBrand = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    // Cor do veículo
                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleColor,
                        onExpandedChange = { expandedVehicleColor = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            value = selectedVehicleColor,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cor do Veículo") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleColor)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehicleColor,
                            onDismissRequest = { expandedVehicleColor = false }
                        ) {
                            colors.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedVehicleColor = option
                                        expandedVehicleColor = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                // ── STEP 3 – Endereço ─────────────────────────────────────
                if (step == 3) {
                    Text("Endereço", style = MaterialTheme.typography.headlineSmall)

                    OutlinedTextField(
                        value = cep,
                        onValueChange = {
                            val novoCep = it.filter { char -> char.isDigit() }.take(8)
                            cep = novoCep
                            cepLengthError = false
                            cepError = UserFieldValidator.validarCep(novoCep)
                            logradouro = ""
                            bairro = ""
                            cidade = ""
                            estado = ""
                            enderecoError = null
                            if (novoCep.length == 8) {
                                isLoadingCep = true
                                cepError = null
                                viewModel.fetchCep(novoCep)
                            } else {
                                isLoadingCep = false
                            }
                        },
                        label = { Text("CEP") },
                        placeholder = { Text("00000000") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = nextField,
                        modifier = Modifier.fillMaxWidth(),
                        isError = cepError != null || cepLengthError,
                        trailingIcon = {
                            if (isLoadingCep) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        singleLine = true
                    )

                    when {
                        cepLengthError -> Text(
                            "CEP incompleto — digite os 8 dígitos",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        cepError != null -> Text(
                            cepError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = logradouro,
                        onValueChange = {},
                        label = { Text("Logradouro") },
                        enabled = false,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = numero,
                        onValueChange = {
                            numero = it.trim().take(10)
                            numeroError = UserFieldValidator.validarNumeroEndereco(numero)
                        },
                        label = { Text("Número") },
                        isError = numeroError != null,
                        supportingText = { numeroError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = nextField,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = complemento,
                        onValueChange = { complemento = it },
                        label = { Text("Complemento (opcional)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = doneField,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bairro,
                        onValueChange = {},
                        label = { Text("Bairro") },
                        enabled = false,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cidade,
                        onValueChange = {},
                        label = { Text("Cidade") },
                        enabled = false,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = estado,
                        onValueChange = {},
                        label = { Text("Estado (UF)") },
                        placeholder = { Text("SP") },
                        enabled = false,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Aviso visível quando cidade não é São Paulo
                    enderecoError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (cidade.isNotBlank() &&
                        !cidade.trim().equals("São Paulo", ignoreCase = true)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "O Parada Certa ainda não está disponível em $cidade. " +
                                       "Em breve chegaremos à sua cidade!",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // ── STEP 4 – Termos e Política ────────────────────────────
                if (step == 4) {
                    Text("Termos e Política", style = MaterialTheme.typography.headlineSmall)

                    Text(
                        text = "Para finalizar seu cadastro, é necessário aceitar nossos Termos de Uso e Política de Privacidade.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = {
                                acceptedTerms = it
                                if (showError) showError = false
                            }
                        )
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = "Li e aceito os ")
                            TextButton(
                                onClick = { showTermsDialog = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.offset(y = (-8).dp)
                            ) {
                                Text("Termos de Uso", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedPrivacy,
                            onCheckedChange = {
                                acceptedPrivacy = it
                                if (showError) showError = false
                            }
                        )
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text(text = "Li e aceito a ")
                            TextButton(
                                onClick = { showPrivacyDialog = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.offset(y = (-8).dp)
                            ) {
                                Text("Política de Privacidade", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (showError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Você precisa aceitar os Termos de Uso e a Política de Privacidade para continuar",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when (step) {
                        1 -> {
                            nameError = UserFieldValidator.validarNome(name)
                            emailError = UserFieldValidator.validarEmail(email)
                            passwordError = UserFieldValidator.validarSenha(password)
                            confirmPasswordError = if (password != confirmPassword) "As senhas não coincidem." else null
                            cpfError = UserFieldValidator.validarCpf(cpf.text)
                            birthDateError = UserFieldValidator.validarDataNascimento(dtnascimento.text)
                            phoneError = UserFieldValidator.validarTelefone(numCelular.text)

                            showError = listOf(
                                nameError,
                                emailError,
                                passwordError,
                                confirmPasswordError,
                                cpfError,
                                birthDateError,
                                phoneError
                            ).any { it != null }

                            if (!showError) {
                                showError = false
                                step++
                            }
                        }
                        2 -> {
                            if (!plateIsValid || selectedVehicleBrand.isBlank()) {
                                showError = true
                            } else {
                                showError = false
                                step++
                            }
                        }
                        3 -> {
                            val cidadeValida = cidade.trim().equals("São Paulo", ignoreCase = true)
                            cepError = UserFieldValidator.validarCep(cep)
                            numeroError = UserFieldValidator.validarNumeroEndereco(numero)
                            enderecoError = if (logradouro.isBlank() || bairro.isBlank() ||
                                cidade.isBlank() || estado.length != 2
                            ) {
                                "Informe um CEP válido para preencher o endereço."
                            } else null
                            if (cep.length != 8) {
                                cepLengthError = true
                            } else if (numeroError != null || enderecoError != null) {
                                showError = true
                            } else if (!cidadeValida) {
                                cidadeForaCobertura = cidade
                                showCidadeDialog = true
                            } else {
                                showError = false
                                step++
                            }
                        }
                        4 -> {
                            if (acceptedTerms && acceptedPrivacy) {
                                viewModel.registerUser(
                                    nome = name,
                                    email = email,
                                    senha = password,
                                    cpf = cpf.text.filter { it.isDigit() },
                                    dataNascimento = dtnascimento.text,
                                    placa = plate,
                                    modeloVeiculo = selectedVehicleBrand,
                                    corVeiculo = selectedVehicleColor,
                                    cep = cep,
                                    logradouro = logradouro,
                                    numero = numero,
                                    complemento = complemento,
                                    bairro = bairro,
                                    cidade = cidade,
                                    estado = estado,
                                    numeroCelular = numCelular.text
                                )
                            } else {
                                showError = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !registerState.isLoading
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (step < 4) "Próximo" else "Finalizar Cadastro")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }

    // ── Dialog: cidade fora de cobertura ─────────────────────────────────
    if (showCidadeDialog) {
        AlertDialog(
            onDismissRequest = { showCidadeDialog = false },
            title = { Text("Cidade não disponível") },
            text = {
                Text(
                    "O Parada Certa ainda não está disponível em $cidadeForaCobertura.\n\n" +
                    "Em breve chegaremos à sua cidade! Por enquanto, o aplicativo " +
                    "atende apenas a cidade de São Paulo."
                )
            },
            confirmButton = {
                TextButton(onClick = { showCidadeDialog = false }) {
                    Text("Entendi")
                }
            }
        )
    }

    // ── Dialogs de Termos e Política ──────────────────────────────────────
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Termos de Uso", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = LegalDocuments.TERMOS_DE_USO,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Fechar") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Política de Privacidade", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = LegalDocuments.POLITICA_PRIVACIDADE,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Fechar") }
            }
        )
    }
}
