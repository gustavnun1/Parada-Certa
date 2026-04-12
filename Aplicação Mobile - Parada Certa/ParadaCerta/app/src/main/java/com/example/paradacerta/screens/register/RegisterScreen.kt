package com.example.paradacerta.screens.register

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paradacerta.viewmodel.RegisterViewModel
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

// ---------- Marcas de veículos ----------
private val VEHICLE_BRANDS = listOf(
    "Audi", "BYD", "BMW", "Caoa Chery", "Chevrolet", "Citroën",
    "Dodge", "Ducati", "Fiat", "Ford", "GWM", "Harley-Davidson",
    "Honda", "Hyundai", "Jeep", "Kawasaki", "Kia", "Land Rover",
    "Mercedes-Benz", "Mitsubishi", "Nissan", "Peugeot", "RAM",
    "Renault", "Subaru", "Suzuki", "Toyota", "Triumph",
    "Volkswagen", "Volvo", "Yamaha"
).sorted()

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
    viewModel: RegisterViewModel = viewModel()
) {
    var step by remember { mutableStateOf(1) }

    // Dados Pessoais
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf(TextFieldValue("")) }
    var dtnascimento by remember { mutableStateOf(TextFieldValue("")) }
    var numCelular by remember { mutableStateOf(TextFieldValue("")) }

    // Dados do Veículo
    var plate by remember { mutableStateOf("") }
    var selectedVehicleBrand by remember { mutableStateOf("") }
    var expandedVehicleBrand by remember { mutableStateOf(false) }

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
    var showCidadeDialog by remember { mutableStateOf(false) }
    var cidadeForaCobertura by remember { mutableStateOf("") }

    // Termos de Uso e Política de Privacidade
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Filtragem de marcas de veículo
    val filteredBrands = remember(selectedVehicleBrand) {
        if (selectedVehicleBrand.isBlank()) VEHICLE_BRANDS
        else VEHICLE_BRANDS.filter { it.contains(selectedVehicleBrand, ignoreCase = true) }
    }

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
            // Bloqueia cidades fora da cobertura
            if (addressState.cidade.isNotBlank() &&
                !addressState.cidade.trim().equals("São Paulo", ignoreCase = true)
            ) {
                cidadeForaCobertura = addressState.cidade
                showCidadeDialog = true
            }
        }
    }

    LaunchedEffect(registerState) {
        if (registerState.isSuccess) {
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
                    if (step > 1) {
                        IconButton(onClick = {
                            step--
                            showError = false
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
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
                        onValueChange = { name = it },
                        label = { Text("Nome completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Senha com indicador de força
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = password.isNotEmpty() && !isPasswordStrong(password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (password.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = passwordStrength.progress,
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
                        onValueChange = { confirmPassword = it },
                        label = { Text("Repetir Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
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
                        },
                        label = { Text("CPF") },
                        placeholder = { Text("000.000.000-00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        },
                        label = { Text("Data de Nascimento") },
                        placeholder = { Text("DD/MM/AAAA") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        },
                        label = { Text("Número do Telefone") },
                        placeholder = { Text("(00) 00000-0000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── STEP 2 – Dados do Veículo ─────────────────────────────
                if (step == 2) {
                    Text("Dados do Veículo", style = MaterialTheme.typography.headlineSmall)

                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it.uppercase().take(7) },
                        label = { Text("Placa") },
                        placeholder = { Text("ABC1234") },
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

                    // Marca do veículo — combobox pesquisável
                    ExposedDropdownMenuBox(
                        expanded = expandedVehicleBrand && filteredBrands.isNotEmpty(),
                        onExpandedChange = { expandedVehicleBrand = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            value = selectedVehicleBrand,
                            onValueChange = {
                                selectedVehicleBrand = it
                                expandedVehicleBrand = true
                            },
                            label = { Text("Marca do Veículo") },
                            placeholder = { Text("Ex: Volkswagen, Fiat, BYD...") },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expandedVehicleBrand && filteredBrands.isNotEmpty()
                                )
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        if (filteredBrands.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = expandedVehicleBrand,
                                onDismissRequest = { expandedVehicleBrand = false },
                                modifier = Modifier.heightIn(max = 240.dp)
                            ) {
                                filteredBrands.forEach { option ->
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
                            cep = it.filter { char -> char.isDigit() }.take(8)
                            if (cep.length == 8) {
                                isLoadingCep = true
                                cepError = null
                                viewModel.fetchCep(cep)
                            }
                        },
                        label = { Text("CEP") },
                        placeholder = { Text("00000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = cepError != null,
                        trailingIcon = {
                            if (isLoadingCep) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )

                    cepError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    OutlinedTextField(
                        value = logradouro,
                        onValueChange = { logradouro = it },
                        label = { Text("Logradouro") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = numero,
                        onValueChange = { numero = it },
                        label = { Text("Número") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = complemento,
                        onValueChange = { complemento = it },
                        label = { Text("Complemento (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bairro,
                        onValueChange = { bairro = it },
                        label = { Text("Bairro") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cidade,
                        onValueChange = { cidade = it },
                        label = { Text("Cidade") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = estado,
                        onValueChange = { estado = it.uppercase().take(2) },
                        label = { Text("Estado (UF)") },
                        placeholder = { Text("SP") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Aviso visível quando cidade não é São Paulo
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
                            if (name.isBlank() || email.isBlank() ||
                                !isPasswordStrong(password) ||
                                password != confirmPassword ||
                                cpf.text.replace("[^0-9]".toRegex(), "").length != 11 ||
                                !dtnascimento.text.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
                                showError = true
                            } else {
                                showError = false
                                step++
                            }
                        }
                        2 -> {
                            if (plate.isBlank() || selectedVehicleBrand.isBlank()) {
                                showError = true
                            } else {
                                showError = false
                                step++
                            }
                        }
                        3 -> {
                            val cidadeValida = cidade.trim().equals("São Paulo", ignoreCase = true)
                            if (cep.length != 8 || logradouro.isBlank() ||
                                numero.isBlank() || bairro.isBlank() ||
                                cidade.isBlank() || estado.length != 2) {
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
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
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
                        text = """
1. APRESENTAÇÃO

O presente documento tem por finalidade estabelecer os Termos de Uso do aplicativo Parada Certa, solução tecnológica voltada à conexão entre motoristas e estacionamentos privados.

Ao utilizar o Parada Certa, o usuário declara estar ciente, compreender e concordar integralmente com as disposições aqui previstas, em conformidade com a LGPD.

2. ACEITE DOS TERMOS

O aceite deste Termo ocorre quando o usuário:
• Realiza o cadastro no aplicativo
• Marca "Li e concordo com os Termos de Uso"
• Continua utilizando o sistema

3. CADASTRO E ELEGIBILIDADE

Para utilizar o aplicativo como motorista, é necessário:
• Ser maior de 18 anos
• Fornecer dados verdadeiros: nome, e-mail, CPF, placa do veículo

O usuário é responsável por manter a confidencialidade de sua senha.

4. DESCRIÇÃO DOS SERVIÇOS

O Parada Certa oferece:
• Visualização de estacionamentos próximos
• Consulta de vagas disponíveis em tempo real
• Visualização de valores, horários e avaliações
• Reserva antecipada de vagas
• Planos gratuitos e pagos com funcionalidades adicionais

5. RESPONSABILIDADES DO MOTORISTA

O motorista compromete-se a:
• Utilizar o aplicativo de forma lícita
• Fornecer apenas avaliações verdadeiras
• Respeitar as regras de cada estacionamento

6. LIMITAÇÕES DE RESPONSABILIDADE

O Parada Certa atua como intermediador, não sendo responsável por:
• Danos, furtos ou roubos em estacionamentos
• Divergências entre informações cadastradas
• Interrupções temporárias de acesso

7. ALTERAÇÕES

Este Termo pode ser alterado a qualquer tempo. O uso continuado após atualização será interpretado como aceite.

Para dúvidas: atendimento@paradacerta.com
                        """.trimIndent(),
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
                        text = """
POLÍTICA DE PRIVACIDADE - PARADA CERTA

1. CONTROLADORA DE DADOS

O Parada Certa atua como Controladora dos dados coletados, em conformidade com a LGPD (Lei 13.709/2018).

2. DADOS COLETADOS DE MOTORISTAS

Poderão ser coletados:
• Dados cadastrais: nome, e-mail, CPF, telefone, data de nascimento
• Dados de veículo: placa, modelo, cor
• Dados de uso: histórico de estacionamentos, reservas, avaliações
• Dados de localização (quando autorizado)
• Dados de pagamento (quando aplicável)

3. FINALIDADES DO TRATAMENTO

Os dados serão usados para:
• Cadastro e autenticação de usuários
• Funcionamento das funcionalidades (mapa, vagas, reservas)
• Oferecer informações sobre vagas e preços
• Melhorar a experiência de uso
• Cumprir obrigações legais
• Exibir anúncios personalizados (quando aplicável)

4. COMPARTILHAMENTO DE DADOS

Os dados de motoristas poderão ser:
• Compartilhados com estacionamentos para execução de serviços
• Usados de forma anonimizada para estatísticas
• Compartilhados com intermediadores de pagamento
• Compartilhados com autoridades mediante ordem judicial

5. BASE LEGAL

O tratamento é realizado com base em:
• Consentimento do titular
• Cumprimento de obrigação legal
• Execução de contrato
• Legítimo interesse do controlador

6. ARMAZENAMENTO E SEGURANÇA

Os dados são armazenados em ambiente seguro com:
• Servidores em nuvem
• Criptografia
• Controle de acesso

7. DIREITOS DOS TITULARES (LGPD)

Você pode:
• Confirmar existência de tratamento
• Acessar seus dados pessoais
• Solicitar correção de dados
• Solicitar eliminação de dados
• Solicitar portabilidade
• Revogar consentimento

Para exercer seus direitos: atendimento@paradacerta.com

8. RETENÇÃO DOS DADOS

Os dados serão mantidos:
• Pelo período necessário ao uso da plataforma
• Pelo prazo exigido por lei
• Ou até pedido de exclusão

9. ALTERAÇÕES

Esta Política pode ser alterada a qualquer tempo. Você será notificado por e-mail.

Contato: atendimento@paradacerta.com
                        """.trimIndent(),
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
